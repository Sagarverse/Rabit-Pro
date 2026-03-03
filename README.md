# Rabit Pro: AI-Augmented Bluetooth HID Suite

**Rabit Pro** is a high-performance Android utility that transforms your smartphone into a sophisticated Bluetooth HID (Human Interface Device) controller. By emulating hardware-level keyboard and mouse inputs, Rabit Pro bridges the gap between mobile and desktop productivity, supercharged with integrated AI automation.

---

## 🚀 Key Features

### ⌨️ Bluetooth HID Emulation
- **Virtual Keyboard:** Full keyboard emulation with modifier support (Shift, Ctrl, Alt, GUI).
- **Precision Mouse:** High-accuracy mouse tracking with support for buttons and scroll wheel.
- **Consumer Keys:** Integrated media controls, volume, and system shortcuts.
- **"One-Tap" Unlock:** Securely store and transmit credentials to unlock your Mac with a single tap.

### 🤖 AI Assistant & Automation
- **Gemini AI Integration:** Cloud-powered AI assistant for complex text generation and task management.
- **System Instructions:** Define persistent behavioral commands (e.g., "Reply in two sentences") to customize AI personality.
- **Local LLM Support:** Capability for offline AI interactions using `.gguf` models.
- **Auto-Push:** Automatically type AI-generated responses directly to your connected desktop.

### 🔐 Security & Screen Handoff
- **End-to-End Encryption (E2EE):** AES-GCM 256-bit pairing with QR-based key exchange ensures secure communication between Android and Mac.
- **Screen Handoff:** Instantly send URLs and clipboard content from your phone to your computer.
- **Shake-to-Control:** Use device sensors to answer or reject calls on your phone using HID commands.

---

## 🛠️ Technical Stack

- **UI Framework:** Jetpack Compose (Material 3) with Premium Glassmorphism UI.
- **Language:** 100% Kotlin.
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles and Repository pattern.
- **Networking:** Ktor (Local File Server), Retrofit, OkHttp.
- **Security:** Android Jetpack Security-Crypto (AES-GCM, Tink).
- **Bluetooth:** Native Android Bluetooth Stack (HID over GATT profile).

---

## 🏗️ Project Architecture

The app is built on a modular, scalable architecture:
- **`data/bluetooth`**: Core HID and GATT management logic.
- **`data/gemini`**: Integration with Google Gemini API and LLM handling.
- **`data/secure`**: Cryptographic implementation for E2EE and credential management.
- **`domain/repository`**: Interface abstractions for clean dependency injection.
- **`ui/`**: Modern, reactive UI components built with Compose and Coroutines.

---

## 🚦 Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/[your-username]/rabit-pro.git
   ```
2. **Setup API Key:**
   Obtain a Gemini API key from Google AI Studio and enter it in the app's settings.
3. **Build:**
   Open the project in Android Studio (Iguana or newer) and build the `:app` module.
4. **Permissions:**
   The app requires Bluetooth, Location (for scanning), and Foreground Service permissions to function as an HID peripheral.

---

## 👨‍💻 Developer Notes

This project was developed with a focus on **Hardware-Software interaction** and **AI Productivity**. It demonstrates advanced usage of Android's Bluetooth stack, secure data storage, and modern reactive UI patterns.

*Designed and Developed by [Your Name/GitHub Name]*
