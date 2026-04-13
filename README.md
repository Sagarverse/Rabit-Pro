# Rabit Pro

Rabit Pro turns an Android phone into a Bluetooth HID controller for Mac and Android targets, with AI-assisted typing and productivity modules.

## Complete Feature List

### Core Input and Control

- Bluetooth HID keyboard typing.
- Modifier key support (Cmd/Ctrl/Shift/Alt style combos).
- System shortcut and media/control key actions.
- Touchpad mode for pointer movement and clicks.
- Air-mouse/gyro control path.
- Trackpad sensitivity controls.

### AI and Text Workflows

- AI assistant chat interface.
- Prompt templates and guided response flow.
- AI text rewrite/summarize/expand use cases.
- Auto-push generated AI response to connected target.
- Copy AI output to clipboard.
- Text-to-speech playback for AI responses.
- Voice input and speech controls.

### Bridge, Handoff, and Sync

- Web Bridge local server mode.
- QR-based local Web Bridge access.
- Bridge passcode/PIN generation and rotation.
- Shared file listing and download via bridge.
- Universal clipboard sync through bridge provider.
- URL Handoff activity flow from Android share sheet.

### Automation and Remote Tools

- Automation dashboard with categorized quick macros.
- Custom user-defined macros.
- Wake-on-LAN utility screen.
- SSH terminal utility screen.
- Built-in launch/control macros (examples include Spotlight, lock, sleep, and app-launch actions).

### Security, Reliability, and Personalization

- Biometric lock option.
- Stealth history mode.
- Auto reconnect preference.
- Shake-to-disconnect gesture.
- Dynamic theme + monochrome mode.
- Haptic profile presets.
- Voice pitch and speech-rate tuning.
- Feature visibility toggles (Web Bridge, Automation, Assistant, Snippets, Shortcuts, Wake-on-LAN, SSH).

### Utility Screens and Navigation

- Onboarding and pairing flow.
- Control Hub (keyboard/pad/hub tabs).
- Snippets screen.
- Shortcuts guide screen.
- Settings, customization, and profile screens.

## App Navigation (Current Routes)

- `onboarding`
- `pairing`
- `keyboard`
- `assistant`
- `web_bridge`
- `automation`
- `wake_on_lan`
- `ssh_terminal`
- `snippets`
- `shortcuts`
- `settings`
- `customization`
- `profile`

## Step-by-Step: How To Use

### 1) Install and Launch

1. Install the latest APK from Releases.
2. Open Rabit Pro.
3. Accept required permissions when prompted (Bluetooth, notifications, and related media access where applicable).

### 2) Pair Your Device

1. Go to the Pairing screen.
2. Select your target Mac/Android device.
3. Confirm Bluetooth pairing prompts on both devices.
4. Once connected, Rabit will navigate to the Keyboard flow.

### 3) Use Keyboard and Modifier Keys

1. Open the `keyboard` screen.
2. In `INPUT` tab, type normally using on-screen keys.
3. Toggle modifiers (Cmd/Ctrl/Shift/Alt) for combinations.
4. Use batch typing for long content.

### 4) Use PAD (Touchpad/Air Mouse)

1. Switch to the `PAD` tab.
2. Move cursor, click, and scroll from your phone.
3. Disable air-mouse when not needed to reduce battery use.

### 5) AI Assistant and Auto-Push

1. Open `assistant`.
2. Ask for rewrite, summarize, brainstorm, or formatting help.
3. Use send/auto-push to type generated output onto the connected target.
4. Tune behavior via Settings for best prompt quality.

### 6) URL Handoff

1. On Android, tap Share on a web page.
2. Choose Rabit Handoff target.
3. Rabit triggers the browser-open macro flow on the connected target and types the URL.

### 7) Snippets and Shortcuts

1. Open `snippets` to save repeated text blocks.
2. Tap a snippet to inject it through HID typing.
3. Open `shortcuts` to learn and trigger fast key combos.

### 8) Automation Tools

1. Open `automation` for advanced tools.
2. Use `wake_on_lan` to wake supported devices.
3. Use `ssh_terminal` for authenticated remote command workflows.
4. Use automation features only on devices and systems you own or are authorized to manage.

### 9) Settings and Customization

1. Open `settings` for app and AI preferences.
2. Open `customization` to enable/disable visible modules.
3. Use `profile` to manage user-facing preferences.

## Build and Run

```bash
./gradlew :app:installDebug
adb shell monkey -p com.sagar.rabit -c android.intent.category.LAUNCHER 1
```

## Troubleshooting

- If pairing fails: forget existing Bluetooth pair on both devices and re-pair.
- If no typing occurs: verify target input focus and connection state in Rabit header.
- If app crashes at startup: confirm manifest component names match actual Kotlin package names.

## Tech Stack

- Kotlin + Jetpack Compose
- MVVM architecture
- Android Bluetooth HID stack
- Ktor/OkHttp networking
- Firebase integrations (project-config dependent)

## Web Docs

- Landing page: `index.html`
- Detailed usage guide: `usage-guide.html`
