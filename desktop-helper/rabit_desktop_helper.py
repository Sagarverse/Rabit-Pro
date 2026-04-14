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
import re
import shlex
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
        artwork_b64 = _music_artwork_base64()
        if title:
            return {
                "title": title,
                "artist": artist or "Unknown artist",
                "album": album or "",
                "artworkBase64": artwork_b64,
                "source": "music"
            }

    # Then Spotify
    spotify_state = _run_osascript('tell application "Spotify" to if it is running then player state as string')
    if spotify_state == "playing":
        title = _run_osascript('tell application "Spotify" to name of current track')
        artist = _run_osascript('tell application "Spotify" to artist of current track')
        album = _run_osascript('tell application "Spotify" to album of current track')
        artwork_url = _run_osascript('tell application "Spotify" to artwork url of current track')
        artwork_b64 = _fetch_image_base64_from_url(artwork_url)
        if title:
            return {
                "title": title,
                "artist": artist or "Unknown artist",
                "album": album or "",
                "artworkBase64": artwork_b64,
                "source": "spotify"
            }

    # Browser fallbacks (web players): YouTube Music / Spotify Web / Apple Music Web
    browser_title = _read_browser_now_playing_title()
    if browser_title:
        parsed = _parse_browser_now_playing(browser_title)
        if parsed:
            return parsed

    return None


def _read_browser_now_playing_title() -> str:
    # Prefer Chromium-family first, then Safari.
    scripts = [
        'tell application "Google Chrome" to if it is running then return title of active tab of front window',
        'tell application "Brave Browser" to if it is running then return title of active tab of front window',
        'tell application "Microsoft Edge" to if it is running then return title of active tab of front window',
        'tell application "Safari" to if it is running then return name of front document',
    ]
    for script in scripts:
        title = _run_osascript(script).strip()
        if title:
            return title
    return ""


def _parse_browser_now_playing(title: str) -> dict | None:
    t = title.strip()
    if not t:
        return None

    # YouTube Music pattern: "Song - Artist - YouTube Music"
    if t.endswith(" - YouTube Music"):
        core = t[: -len(" - YouTube Music")].strip()
        parts = [p.strip() for p in core.split(" - ") if p.strip()]
        if len(parts) >= 2:
            return {
                "title": parts[0],
                "artist": parts[1],
                "album": "",
                "artworkBase64": None,
                "source": "youtube-music-web",
            }

    # Spotify web pattern examples:
    # "Song Name - song by Artist | Spotify"
    # "Artist - Song | Spotify" (fallback)
    if t.endswith(" | Spotify"):
        core = t[: -len(" | Spotify")].strip()
        if " - song by " in core:
            song, artist = core.split(" - song by ", 1)
            if song.strip():
                return {
                    "title": song.strip(),
                    "artist": artist.strip() or "Unknown artist",
                    "album": "",
                    "artworkBase64": None,
                    "source": "spotify-web",
                }
        parts = [p.strip() for p in core.split(" - ") if p.strip()]
        if len(parts) >= 2:
            return {
                "title": parts[-1],
                "artist": parts[0],
                "album": "",
                "artworkBase64": None,
                "source": "spotify-web",
            }

    # Apple Music web often has site suffix and track in tab title.
    if "Apple Music" in t and " - " in t:
        parts = [p.strip() for p in t.split(" - ") if p.strip()]
        if len(parts) >= 2:
            return {
                "title": parts[0],
                "artist": parts[1],
                "album": "",
                "artworkBase64": None,
                "source": "apple-music-web",
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


def _request_json_with_retry(
    url: str,
    method: str,
    body: dict,
    headers: dict,
    max_retries: int,
    base_backoff_seconds: float,
):
    attempt = 0
    while True:
        try:
            return _request_json(url, method=method, body=body, headers=headers)
        except urllib.error.HTTPError as e:
            # Retry server-side failures, but do not retry auth/client errors.
            if e.code < 500 or attempt >= max_retries:
                raise
        except urllib.error.URLError:
            if attempt >= max_retries:
                raise

        attempt += 1
        time.sleep(base_backoff_seconds * (2 ** (attempt - 1)))


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


def _resolve_pin(cli_pin: str) -> str:
    if cli_pin:
        return cli_pin.strip()

    env_pin = os.environ.get("RABIT_PIN", "").strip()
    if env_pin:
        return env_pin

    if sys.stdin.isatty():
        try:
            typed = input("Enter Rabit bridge PIN: ").strip()
            return typed
        except EOFError:
            return ""

    return ""


def _is_valid_pin(pin: str) -> bool:
    return bool(re.fullmatch(r"\d{4}", pin or ""))


def _format_auth_error(err: Exception, host: str, port: int) -> str:
    if isinstance(err, urllib.error.HTTPError):
        if err.code in (401, 403):
            return (
                f"Bridge rejected credentials at http://{host}:{port} (HTTP {err.code}).\n"
                "Most likely causes:\n"
                "1) PIN is stale/wrong (generate a fresh PIN in Rabit -> Web Bridge)\n"
                "2) Web Bridge was restarted after helper launch\n"
                "3) Another phone/instance is running on that host:port"
            )
        return f"HTTP error from http://{host}:{port}: {err.code} {err.reason}"

    if isinstance(err, urllib.error.URLError):
        reason = err.reason
        reason_text = str(reason)
        if "Connection refused" in reason_text or "Errno 61" in reason_text:
            return (
                f"Could not reach Rabit bridge at http://{host}:{port} (connection refused).\n"
                "Checks:\n"
                "1) Open Rabit app on phone and start Web Bridge/desktop helper mode\n"
                "2) Ensure phone and Mac are on the same Wi-Fi network\n"
                "3) Verify phone IP and bridge port (default 8080)\n"
                "4) Re-try with --host <phone_ip> --port <bridge_port>"
            )
        if "timed out" in reason_text.lower():
            return (
                f"Timed out connecting to http://{host}:{port}.\n"
                "Phone may be on a different network/subnet, asleep, or firewall-filtered."
            )
        return f"Network error connecting to http://{host}:{port}: {reason_text}"
    return str(err)


def _music_artwork_base64() -> str | None:
    script = r'''
    tell application "Music"
        if it is not running then return ""
        if player state is not playing then return ""
        try
            if (count of artworks of current track) is 0 then return ""
            set artData to raw data of artwork 1 of current track
            set tmpPath to POSIX path of ((path to temporary items folder as text) & "rabit_music_artwork.bin")
            set outFile to open for access (POSIX file tmpPath) with write permission
            set eof outFile to 0
            write artData to outFile
            close access outFile
            return tmpPath
        on error
            try
                close access (POSIX file tmpPath)
            end try
            return ""
        end try
    end tell
    '''
    tmp_path = _run_osascript(script).strip()
    if not tmp_path:
        return None
    try:
        with open(tmp_path, "rb") as f:
            data = f.read()
        return _encode_image_base64(data)
    except Exception:
        return None
    finally:
        try:
            os.remove(tmp_path)
        except Exception:
            pass


def _fetch_image_base64_from_url(url: str) -> str | None:
    if not url:
        return None
    try:
        with urllib.request.urlopen(url, timeout=4) as resp:
            data = resp.read()
        return _encode_image_base64(data)
    except Exception:
        return None


def _encode_image_base64(data: bytes, max_size_bytes: int = 750_000) -> str | None:
    if not data or len(data) > max_size_bytes:
        return None
    return base64.b64encode(data).decode("ascii")


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


def _push_pcm_stream(
    base_url: str,
    token: str,
    chunk_iter,
    sample_rate: int,
    channels: int,
    source: str,
    chunk_post_retries: int,
    chunk_retry_backoff_seconds: float,
) -> None:
    _request_json_with_retry(
        url=f"{base_url}/audio/start",
        method="POST",
        body={"sampleRate": sample_rate, "channels": channels, "source": source},
        headers={"X-Session-Token": token},
        max_retries=chunk_post_retries,
        base_backoff_seconds=chunk_retry_backoff_seconds,
    )

    chunk_count = 0
    try:
        for chunk in chunk_iter:
            if not chunk:
                continue
            _request_json_with_retry(
                url=f"{base_url}/audio/chunk",
                method="POST",
                body={"pcm16leBase64": base64.b64encode(chunk).decode("ascii")},
                headers={"X-Session-Token": token},
                max_retries=chunk_post_retries,
                base_backoff_seconds=chunk_retry_backoff_seconds,
            )
            chunk_count += 1
            if chunk_count % 50 == 0:
                print(f"[audio] streamed {chunk_count} chunks")
    finally:
        _request_json_with_retry(
            url=f"{base_url}/audio/stop",
            method="POST",
            body={"reason": "eof"},
            headers={"X-Session-Token": token},
            max_retries=chunk_post_retries,
            base_backoff_seconds=chunk_retry_backoff_seconds,
        )


def stream_file_to_phone(
    base_url: str,
    token: str,
    file_path: str,
    sample_rate: int,
    channels: int,
    chunk_bytes: int,
    chunk_post_retries: int,
    chunk_retry_backoff_seconds: float,
) -> None:
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

    proc = subprocess.Popen(ffmpeg_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        def iterator():
            while True:
                chunk = proc.stdout.read(chunk_bytes) if proc.stdout else b""
                if not chunk:
                    break
                yield chunk

        _push_pcm_stream(
            base_url=base_url,
            token=token,
            chunk_iter=iterator(),
            sample_rate=sample_rate,
            channels=channels,
            source="desktop-helper-file",
            chunk_post_retries=chunk_post_retries,
            chunk_retry_backoff_seconds=chunk_retry_backoff_seconds,
        )
        proc.wait(timeout=10)
        if proc.returncode not in (0, None):
            stderr = (proc.stderr.read().decode("utf-8", errors="ignore") if proc.stderr else "").strip()
            msg = stderr.splitlines()[-1] if stderr else f"ffmpeg exited with code {proc.returncode}"
            raise RuntimeError(f"File stream encoder failed: {msg}")
    finally:
        if proc.poll() is None:
            proc.terminate()


def stream_command_to_phone(
    base_url: str,
    token: str,
    command: str,
    sample_rate: int,
    channels: int,
    chunk_bytes: int,
    chunk_post_retries: int,
    chunk_retry_backoff_seconds: float,
) -> None:
    if not command.strip():
        raise ValueError("Missing --stream-command value")

    # Command must output raw PCM16LE bytes to stdout.
    cmd = shlex.split(command)
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        def iterator():
            while True:
                chunk = proc.stdout.read(chunk_bytes) if proc.stdout else b""
                if not chunk:
                    break
                yield chunk

        _push_pcm_stream(
            base_url=base_url,
            token=token,
            chunk_iter=iterator(),
            sample_rate=sample_rate,
            channels=channels,
            source="desktop-helper-command",
            chunk_post_retries=chunk_post_retries,
            chunk_retry_backoff_seconds=chunk_retry_backoff_seconds,
        )
        proc.wait(timeout=10)
        if proc.returncode not in (0, None):
            stderr = (proc.stderr.read().decode("utf-8", errors="ignore") if proc.stderr else "").strip()
            msg = stderr.splitlines()[-1] if stderr else f"command exited with code {proc.returncode}"
            raise RuntimeError(f"Command stream encoder failed: {msg}")
    finally:
        if proc.poll() is None:
            proc.terminate()


def main() -> int:
    parser = argparse.ArgumentParser(description="Rabit desktop helper")
    parser.add_argument("--host", default="127.0.0.1", help="Phone IP/hostname")
    parser.add_argument("--port", type=int, default=8080, help="Bridge port")
    parser.add_argument("--pin", default="", help="4-digit bridge PIN (or set RABIT_PIN env)")
    parser.add_argument("--device-id", default="rabit-desktop-helper", help="Device identifier")
    parser.add_argument("--poll", type=float, default=2.5, help="Polling interval seconds")
    parser.add_argument("--stream-file", default="", help="Optional audio/video file to stream to phone via Wi-Fi PCM")
    parser.add_argument(
        "--stream-command",
        default="",
        help="Optional command that outputs PCM16LE to stdout and streams it to phone",
    )
    parser.add_argument("--stream-rate", type=int, default=44100, help="PCM sample rate for --stream-file")
    parser.add_argument("--stream-channels", type=int, default=2, help="PCM channels for stream mode (1 or 2)")
    parser.add_argument("--stream-chunk-bytes", type=int, default=4096, help="PCM bytes sent per /audio/chunk request")
    parser.add_argument("--stream-retries", type=int, default=3, help="Retry count for transient /audio chunk network failures")
    parser.add_argument(
        "--stream-retry-backoff",
        type=float,
        default=0.15,
        help="Initial retry backoff seconds for /audio chunk POST failures",
    )
    parser.add_argument("--stream-only", action="store_true", help="Only run streaming mode and exit")
    args = parser.parse_args()

    pin = _resolve_pin(args.pin)
    if not _is_valid_pin(pin):
        print("Missing or invalid PIN. Provide --pin 1234 or export RABIT_PIN=1234")
        return 2

    base_url = f"http://{args.host}:{args.port}"
    try:
        token = authenticate(base_url, pin, args.device_id)
    except Exception as e:
        print("Authentication failed:")
        print(_format_auth_error(e, args.host, args.port))
        return 1

    print("Authenticated. Starting companion loop...")
    chunk_bytes = max(512, args.stream_chunk_bytes)
    chunk_retries = max(0, args.stream_retries)
    chunk_retry_backoff = max(0.05, args.stream_retry_backoff)

    if args.stream_file:
        print(f"Starting Wi-Fi PCM stream from file: {args.stream_file}")
        stream_file_to_phone(
            base_url=base_url,
            token=token,
            file_path=args.stream_file,
            sample_rate=args.stream_rate,
            channels=max(1, min(2, args.stream_channels)),
            chunk_bytes=chunk_bytes,
            chunk_post_retries=chunk_retries,
            chunk_retry_backoff_seconds=chunk_retry_backoff,
        )
        print("File stream completed.")
        if args.stream_only:
            return 0

    if args.stream_command:
        print(f"Starting Wi-Fi PCM stream from command: {args.stream_command}")
        stream_command_to_phone(
            base_url,
            token,
            args.stream_command,
            args.stream_rate,
            max(1, min(2, args.stream_channels)),
            chunk_bytes,
            chunk_retries,
            chunk_retry_backoff,
        )
        print("Command stream completed.")
        if args.stream_only:
            return 0

    run_loop(base_url, token, args.poll)
    return 0


if __name__ == "__main__":
    sys.exit(main())
