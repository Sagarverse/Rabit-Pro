# Rabit Companion Desktop Helper

This helper provides a lightweight desktop-side companion for the Rabit phone bridge.

## Features
- Syncs clipboard from desktop to phone and phone to desktop
- Reads transfer job status from the phone bridge
- Authenticates with bridge PIN and keeps a session token
- Pushes macOS now-playing metadata to Rabit Media Deck:
	- Music.app
	- Spotify app
	- Browser web players (best effort): YouTube Music, Spotify Web, Apple Music Web

## Requirements
- macOS (uses `pbpaste`/`pbcopy`)
- Python 3.9+

## Run
```bash
python3 desktop-helper/rabit_desktop_helper.py --host <PHONE_IP> --pin <PIN>
```

You can also set the PIN via environment variable:

```bash
export RABIT_PIN=1234
python3 desktop-helper/rabit_desktop_helper.py --host <PHONE_IP>
```

If neither `--pin` nor `RABIT_PIN` is provided, the helper will prompt for PIN in interactive terminals.

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
	--stream-retries 4 \
	--stream-retry-backoff 0.2 \
	--stream-only
```

### Optional: Stream PCM from any command pipeline
Use this when you already have a command that outputs raw PCM16LE bytes to stdout.

```bash
python3 desktop-helper/rabit_desktop_helper.py \
	--host 192.168.1.40 \
	--pin 1234 \
	--stream-command "ffmpeg -hide_banner -loglevel error -i /path/to/song.mp3 -f s16le -acodec pcm_s16le -ar 44100 -ac 2 pipe:1" \
	--stream-rate 44100 \
	--stream-channels 2 \
	--stream-chunk-bytes 4096 \
	--stream-retries 4 \
	--stream-retry-backoff 0.2 \
	--stream-only
```

This is useful for advanced fallback setups (for example virtual audio-device pipelines) when native AirPlay codec compatibility is limited.

### Streaming reliability controls
- `--stream-chunk-bytes`: PCM bytes per `/audio/chunk` request (default `4096`)
- `--stream-retries`: retry count per failed `/audio/*` POST (default `3`)
- `--stream-retry-backoff`: initial backoff seconds (default `0.15`, exponential)

## Notes
- The phone bridge must be running in the app first.
- The helper currently uses polling for reliability and simplicity.
- You can tune `--poll` for faster or lower-power sync.
- On first run, macOS may ask for Automation permission for `python` to control Music/Spotify via AppleScript. Allow it for now-playing updates.
- Fallback PCM streaming posts to `/audio/start`, `/audio/chunk`, `/audio/stop` and plays through Rabit receiver UI status pipeline.
