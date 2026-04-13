#!/usr/bin/env python3
"""
Rabit Companion Desktop Helper (macOS-first)

Capabilities:
- Authenticates with the Rabit phone bridge using a PIN
- Pushes Mac clipboard -> phone (/clipboard)
- Pulls phone clipboard -> Mac (/clipboard)
- Prints transfer queue snapshots (/transfers)
- Pushes now-playing metadata -> phone (/now-playing)

No third-party Python deps required.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request


def _read_clipboard() -> str:
    try:
        out = subprocess.check_output(["pbpaste"], text=True)
        return out.strip()
    except Exception:
        return ""


def _write_clipboard(text: str) -> None:
    try:
        p = subprocess.Popen(["pbcopy"], stdin=subprocess.PIPE, text=True)
        p.communicate(text)
    except Exception:
        pass


def _run_osascript(script: str) -> str:
    try:
        return subprocess.check_output(["osascript", "-e", script], text=True).strip()
    except Exception:
        return ""


def _fetch_now_playing() -> dict | None:
    # Try Music.app first
    music_state = _run_osascript('tell application "Music" to if it is running then player state as string')
    if music_state == "playing":
        title = _run_osascript('tell application "Music" to name of current track')
        artist = _run_osascript('tell application "Music" to artist of current track')
        album = _run_osascript('tell application "Music" to album of current track')
        if title:
            return {
                "title": title,
                "artist": artist or "Unknown artist",
                "album": album or "",
                "source": "music"
            }

    # Then Spotify
    spotify_state = _run_osascript('tell application "Spotify" to if it is running then player state as string')
    if spotify_state == "playing":
        title = _run_osascript('tell application "Spotify" to name of current track')
        artist = _run_osascript('tell application "Spotify" to artist of current track')
        album = _run_osascript('tell application "Spotify" to album of current track')
        if title:
            return {
                "title": title,
                "artist": artist or "Unknown artist",
                "album": album or "",
                "source": "spotify"
            }

    return None


def _request_json(url: str, method: str = "GET", body: dict | None = None, headers: dict | None = None):
    payload = None
    req_headers = {"Content-Type": "application/json"}
    if headers:
        req_headers.update(headers)
    if body is not None:
        payload = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url=url, data=payload, headers=req_headers, method=method)
    with urllib.request.urlopen(req, timeout=5) as resp:
        return json.loads(resp.read().decode("utf-8"))


def authenticate(base_url: str, pin: str, device_id: str) -> str:
    data = _request_json(
        f"{base_url}/auth",
        method="POST",
        body={"pin": pin},
        headers={"X-Device-Id": device_id},
    )
    if not data.get("success"):
        raise RuntimeError("Authentication failed")
    return data["message"]


def run_loop(base_url: str, token: str, poll_seconds: float) -> None:
    last_local = _read_clipboard()
    last_remote = ""
    last_now_playing = None

    while True:
        try:
            remote = _request_json(
                f"{base_url}/clipboard",
                headers={"X-Session-Token": token},
            ).get("text", "")

            if remote and remote != last_remote and remote != last_local:
                _write_clipboard(remote)
                last_remote = remote
                last_local = remote
                print("[pull] phone -> mac clipboard updated")

            local_now = _read_clipboard()
            if local_now and local_now != last_local and local_now != last_remote:
                _request_json(
                    f"{base_url}/clipboard",
                    method="POST",
                    body={"text": local_now},
                    headers={"X-Session-Token": token},
                )
                last_local = local_now
                print("[push] mac -> phone clipboard updated")

            transfers = _request_json(
                f"{base_url}/transfers",
                headers={"X-Session-Token": token},
            )
            if isinstance(transfers, list) and transfers:
                top = transfers[0]
                print(
                    f"[transfer] {top.get('name')} {top.get('status')} {top.get('progressPercent', 0)}%"
                )

            now_playing = _fetch_now_playing()
            if now_playing and now_playing != last_now_playing:
                _request_json(
                    f"{base_url}/now-playing",
                    method="POST",
                    body=now_playing,
                    headers={"X-Session-Token": token},
                )
                last_now_playing = now_playing
                print(f"[media] {now_playing.get('title')} - {now_playing.get('artist')}")

        except urllib.error.HTTPError as e:
            print(f"[http] {e.code}")
        except Exception as e:
            print(f"[err] {e}")

        time.sleep(poll_seconds)


def stream_file_to_phone(base_url: str, token: str, file_path: str, sample_rate: int, channels: int) -> None:
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    ffmpeg_cmd = [
        "ffmpeg",
        "-hide_banner",
        "-loglevel",
        "error",
        "-i",
        file_path,
        "-f",
        "s16le",
        "-acodec",
        "pcm_s16le",
        "-ar",
        str(sample_rate),
        "-ac",
        str(channels),
        "pipe:1",
    ]

    _request_json(
        f"{base_url}/audio/start",
        method="POST",
        body={"sampleRate": sample_rate, "channels": channels, "source": "desktop-helper-file"},
        headers={"X-Session-Token": token},
    )

    proc = subprocess.Popen(ffmpeg_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        chunk_size = 4096
        while True:
            chunk = proc.stdout.read(chunk_size) if proc.stdout else b""
            if not chunk:
                break
            _request_json(
                f"{base_url}/audio/chunk",
                method="POST",
                body={"pcm16leBase64": base64.b64encode(chunk).decode("ascii")},
                headers={"X-Session-Token": token},
            )
        proc.wait(timeout=10)
    finally:
        _request_json(
            f"{base_url}/audio/stop",
            method="POST",
            body={"reason": "eof"},
            headers={"X-Session-Token": token},
        )


def main() -> int:
    parser = argparse.ArgumentParser(description="Rabit desktop helper")
    parser.add_argument("--host", default="127.0.0.1", help="Phone IP/hostname")
    parser.add_argument("--port", type=int, default=8080, help="Bridge port")
    parser.add_argument("--pin", required=True, help="4-digit bridge PIN")
    parser.add_argument("--device-id", default="rabit-desktop-helper", help="Device identifier")
    parser.add_argument("--poll", type=float, default=2.5, help="Polling interval seconds")
    parser.add_argument("--stream-file", default="", help="Optional audio/video file to stream to phone via Wi-Fi PCM")
    parser.add_argument("--stream-rate", type=int, default=44100, help="PCM sample rate for --stream-file")
    parser.add_argument("--stream-channels", type=int, default=2, help="PCM channels for --stream-file (1 or 2)")
    parser.add_argument("--stream-only", action="store_true", help="Only run file streaming and exit")
    args = parser.parse_args()

    base_url = f"http://{args.host}:{args.port}"
    try:
        token = authenticate(base_url, args.pin, args.device_id)
    except Exception as e:
        print(f"Authentication failed: {e}")
        return 1

    print("Authenticated. Starting companion loop...")
    if args.stream_file:
        print(f"Starting Wi-Fi PCM stream from file: {args.stream_file}")
        stream_file_to_phone(base_url, token, args.stream_file, args.stream_rate, max(1, min(2, args.stream_channels)))
        print("File stream completed.")
        if args.stream_only:
            return 0

    run_loop(base_url, token, args.poll)
    return 0


if __name__ == "__main__":
    sys.exit(main())
