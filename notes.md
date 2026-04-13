Media Deck companion command:
python3 desktop-helper/rabit_desktop_helper.py --host <PHONE_IP> --pin <PIN>

NOW_PLAYING payload consumed by app:
{
	"type": "NOW_PLAYING",
	"title": "Song Title",
	"artist": "Artist",
	"album": "Album",
	"artworkBase64": "..."
}

AirPlay receiver status:
- Service now advertises _raop._tcp and accepts RTSP requests.
- UI status text reflects runtime stages (ready, client connected, RTSP method).
- Full ALAC decode/output engine is still pending integration.

Fallback Wi-Fi PCM stream (file-based):
python3 desktop-helper/rabit_desktop_helper.py --host <PHONE_IP> --pin <PIN> --stream-file /path/to/media.mp3 --stream-only
1. Ultra-Fast Universal Drag-and-Drop (File & Photo Sharing)
What it is: Drag a file, video, or photo on your Mac over to the Rabit UI to instantly send it to your Android device, or securely browse your Android's file system from your Mac.
How to make it work without fail: Instead of relying purely on your existing WebSockets (which limit file sizes and speed), we will incorporate WebRTC DataChannels or run a lightweight local HTTP Server (nanohttpd) on the Android device via a Background Service. This allows incredibly fast, direct peer-to-peer file transfers over Wi-Fi, completely bypassing the cloud.
2. Seamless Universal Media Controls
What it is: When Spotify, Apple Music, or YouTube is playing on your Mac, a persistent, beautiful media player widget shows up on your Android phone's notification shade (and lock screen), letting you view album art, pause, play, or skip tracks remotely.
How to make it work without fail: We use the Android MediaSessionCompat API to create a media session on the phone that simply forwards transport controls (Play/Pause/Next) back over your existing WebSocket connection. On the macOS side, we use simple AppleScript commands to control the active media player.
3. Screen Handoff (Cross-Device "Continue on Mac")
What it is: Browsing a webpage, looking at a Google Maps route, or watching a YouTube video on your phone? Tap a single "Send to Mac" button, and it instantly opens that exact same page on your desktop.
How to make it work without fail: We add a custom IntentFilter on Android that catches native "Share..." actions. When you share a URL to the Rabit Android app, it instantly fires a JSON payload over the WebSocket to macOS. As soon as the macOS client receives it, it uses native commands (open <url>) to pop open the content seamlessly.
4. Smart PC-Aware Automation (Routines)
What it is: Automatically change your phone's behavior when you sit down at your Mac. When the app detects it's connected to your Mac, it automatically sets your phone to "Do Not Disturb" (so you aren't distracted by double-notifications), disables the phone's screen timeout, and routes audio to the PC.
How to make it work without fail: We use Android's BroadcastReceiver combined with your existing foreground services to register when the WebSocket successfully connects. When connected, we use NotificationManager (for DND) and AudioManager to silently modify the phone's state, returning it to normal automatically the second you walk away and disconnect.
5. Enterprise-Grade End-to-End Encryption (E2EE)
What it is: Right now, syncing highly sensitive data (Clipboards, texts, notifications) over local WebSockets can be intercepted if you are ever on a public or shared Wi-Fi network (like a cafe). This feature makes your connections completely unhackable.
How to make it work without fail: We implement AES-GCM 256-bit encryption on the payload string before sending it over the WebSocket. When you link a new Mac to your phone via a QR code or pairing string, we exchange a secure key using Elliptic-Curve Diffie-Hellman encryption, meaning data is locked down device-side only.