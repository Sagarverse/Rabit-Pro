# Rabit Companion Desktop Helper

This helper provides a lightweight desktop-side companion for the Rabit phone bridge.

## Features
- Syncs clipboard from desktop to phone and phone to desktop
- Reads transfer job status from the phone bridge
- Authenticates with bridge PIN and keeps a session token

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

## Notes
- The phone bridge must be running in the app first.
- The helper currently uses polling for reliability and simplicity.
- You can tune `--poll` for faster or lower-power sync.
