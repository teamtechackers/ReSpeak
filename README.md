# re:speak

A real-time audio loopback app for Android. Captures microphone input and plays it back instantly through your earphones — helping you hear and improve your own voice.

---

## What it does

re:speak routes your microphone audio directly to your earphones in real time, with the lowest latency possible. By hearing your own voice as you speak, you can:

- Become aware of your speech patterns, pace, and pronunciation
- Self-correct vocal habits (useful for speech therapy or training)
- Practice public speaking or presentation delivery

---

## Features

- **Ultra-low latency loopback** — raw PCM audio piped directly from `AudioRecord` to `AudioTrack` on a high-priority thread
- **Background playback** — continues running when the screen is off or the phone is locked, via an Android Foreground Service
- **Feedback prevention** — detects connected headphones/earphones; disables the Play button if only the built-in speaker is active
- **Audio focus handling** — automatically pauses on incoming calls or when another audio app takes focus
- **Bluetooth support** — works with wired, USB, and Bluetooth audio output devices

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| Background Audio | Android Foreground Service (`mediaPlayback`) |
| Audio Engine | `AudioRecord` + `AudioTrack` (PCM loopback) |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 16 (API 36) |

---

## Architecture

The app follows a layered Clean Architecture with unidirectional data flow:

```
Jetpack Compose UI
        │  observes state
        ▼
   MainViewModel
        │  calls controls
        ▼
  AudioRepository  ◄──────────────────┐
        │  starts/stops               │
        ▼                             │
AudioLoopbackService          AudioDeviceManager
        │  controls                   (monitors headphone routing)
        ▼
    AudioEngine
 (AudioRecord → AudioTrack PCM loop)
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog or newer
- Android device or emulator running API 24+
- **Wired or Bluetooth earphones** (required to use the app safely)

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/teamtechackers/ReSpeak.git
   ```
2. Open the project in Android Studio.
3. Connect a device or start an emulator.
4. Click **Run** or press `Shift+F10`.

---

## Permissions

| Permission | Reason |
|---|---|
| `RECORD_AUDIO` | Captures microphone input |
| `MODIFY_AUDIO_SETTINGS` | Configures audio routing |
| `FOREGROUND_SERVICE` | Keeps audio running when screen is off |
| `POST_NOTIFICATIONS` | Shows the persistent loopback notification |
| `WAKE_LOCK` | Keeps CPU active during a session |
| `BLUETOOTH_CONNECT` | Detects Bluetooth audio output devices |

---

## License

This project is licensed under the **GNU General Public License v3.0**.  
See the [LICENSE](LICENSE) file for details.
