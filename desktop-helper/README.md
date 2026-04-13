# Rabit Companion Desktop Helper

This helper provides a lightweight desktop-side companion for the Rabit phone bridge.

## Features
- Syncs clipboard from desktop to phone and phone to desktop
- Reads transfer job status from the phone bridge
- Authenticates with bridge PIN and keeps a session token
- Pushes macOS now-playing metadata (Music/Spotify) to Rabit Media Deck

## Requirements
- macOS (uses `pbpaste`/`pbcopy`)
- Python 3.9+

## Run
```bash
python3 desktop-helper/rabit_desktop_helper.py --host <PHONE_IP> --pin <PIN>
```

Example:
```bash
python3 desktop-helper/rabit_desktop_helper.py --host 192.168.1.40 --pin 1234
```

### Optional: Stream a local media file over Wi-Fi PCM fallback
Requires `ffmpeg` in PATH.

```bash
python3 desktop-helper/rabit_desktop_helper.py \
	--host 192.168.1.40 \
	--pin 1234 \
	--stream-file /path/to/song.mp3 \
	--stream-only
```

## Notes
- The phone bridge must be running in the app first.
- The helper currently uses polling for reliability and simplicity.
- You can tune `--poll` for faster or lower-power sync.
- On first run, macOS may ask for Automation permission for `python` to control Music/Spotify via AppleScript. Allow it for now-playing updates.
- Fallback PCM streaming posts to `/audio/start`, `/audio/chunk`, `/audio/stop` and plays through Rabit receiver UI status pipeline.
