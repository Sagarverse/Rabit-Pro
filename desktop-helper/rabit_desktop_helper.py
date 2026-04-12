#!/usr/bin/env python3
"""
Rabit Companion Desktop Helper (macOS-first)

Capabilities:
- Authenticates with the Rabit phone bridge using a PIN
- Pushes Mac clipboard -> phone (/clipboard)
- Pulls phone clipboard -> Mac (/clipboard)
- Prints transfer queue snapshots (/transfers)

No third-party Python deps required.
"""

from __future__ import annotations

import argparse
import json
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

        except urllib.error.HTTPError as e:
            print(f"[http] {e.code}")
        except Exception as e:
            print(f"[err] {e}")

        time.sleep(poll_seconds)


def main() -> int:
    parser = argparse.ArgumentParser(description="Rabit desktop helper")
    parser.add_argument("--host", default="127.0.0.1", help="Phone IP/hostname")
    parser.add_argument("--port", type=int, default=8765, help="Bridge port")
    parser.add_argument("--pin", required=True, help="4-digit bridge PIN")
    parser.add_argument("--device-id", default="rabit-desktop-helper", help="Device identifier")
    parser.add_argument("--poll", type=float, default=2.5, help="Polling interval seconds")
    args = parser.parse_args()

    base_url = f"http://{args.host}:{args.port}"
    try:
        token = authenticate(base_url, args.pin, args.device_id)
    except Exception as e:
        print(f"Authentication failed: {e}")
        return 1

    print("Authenticated. Starting companion loop...")
    run_loop(base_url, token, args.poll)
    return 0


if __name__ == "__main__":
    sys.exit(main())
