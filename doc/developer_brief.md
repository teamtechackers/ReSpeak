# re:speak — Developer Brief

This document details the engineering specifications, architectural requirements, and license guidelines for the **re:speak** Android application.

---

## 1. Technical Stack
* **Language:** 100% Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Minimum SDK:** API Level 24 (Android 7.0)
* **Target SDK:** API Level 34 (Android 14) or 35 (Android 15)
* **Concurrency:** Kotlin Coroutines & Flows
* **Architecture:** MVVM (Model-View-ViewModel) paired with a persistent Foreground Service.

---

## 2. Audio Engine (Pure Kotlin Loopback)

The loopback operates by reading PCM bytes from `AudioRecord` and writing them instantly into `AudioTrack` on a dedicated high-priority audio thread.

### Input Configuration (AudioRecord)
* **Audio Source:** `MediaRecorder.AudioSource.VOICE_COMMUNICATION`
  - *Rationale:* Automatically engages the system-level acoustic echo canceler (AEC) and noise suppressor (NS) available on most Android devices.
* **Sample Rate:** `48000 Hz` (fall back to `44100 Hz` if 48kHz is unsupported).
* **Channel Configuration:** `AudioFormat.CHANNEL_IN_MONO`
* **Audio Encoding:** `AudioFormat.ENCODING_PCM_16BIT`
* **Buffer Size:** `AudioRecord.getMinBufferSize(...) * 2` (or matched to system frame size for performance).

### Output Configuration (AudioTrack)
* **Usage:** `AudioAttributes.USAGE_MEDIA`
* **Content Type:** `AudioAttributes.CONTENT_TYPE_SPEECH`
* **Sample Rate / Channel / Encoding:** Identical to the `AudioRecord` configuration.
* **Transfer Mode:** `AudioTrack.MODE_STREAM`
* **Performance Mode:** `AudioTrack.PERFORMANCE_MODE_LOW_LATENCY` (API 26+)

### Threading & Execution
- Run the reading/writing loop in a dedicated background `Thread` (not a standard coroutine dispatcher to avoid scheduling delays).
- Elevate thread priority inside the loop using:
  `Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)`

### Hardware Effects
Check availability and attach the following effects to the `AudioRecord` session ID:
- **AcousticEchoCanceler:** `AcousticEchoCanceler.create(audioSessionId).setEnabled(true)`
- **NoiseSuppressor:** `NoiseSuppressor.create(audioSessionId).setEnabled(true)`

---

## 3. Background Execution (Foreground Service)

The audio engine lives inside `AudioLoopbackService : Service()` to ensure it survives when the application is backgrounded or when the device screen turns off.

* **Foreground Service Type:** `mediaPlayback` (declared in Manifest and passed during `startForeground()`).
* **Service Lifecycle:** Controlled by UI intents (`ACTION_START`, `ACTION_STOP`). The service acts as the single source of truth for the loopback status.
* **Persistent Notification:** Shows a low-importance notification with a dynamic timer and an action button to "Pause" the loopback immediately from the lock screen.
* **Power Management (WakeLock):** Acquire a `PowerManager.PARTIAL_WAKE_LOCK` when the loopback starts, and release it when it stops. This prevents CPU throttling by aggressive OEM battery optimization layers.
* **Audio Routing Optimization:** Set `AudioManager.mode = AudioManager.MODE_IN_COMMUNICATION` while loopback is active. This tells the OS to prioritize low-latency audio capture and routing.

---

## 4. Headphone & Audio Device Detection

To prevent earsplitting feedback loops, the app must actively monitor output paths.

* **API Check:** Use `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)`.
* **Safe Device Types:**
  - `TYPE_WIRED_HEADPHONES`
  - `TYPE_WIRED_HEADSET`
  - `TYPE_USB_HEADSET`
  - `TYPE_BLUETOOTH_A2DP`
  - `TYPE_BLE_HEADSET`
  - `TYPE_BLE_BROADCAST`
* **Real-time Monitoring:** Register an `AudioDeviceCallback` using `AudioManager.registerAudioDeviceCallback()`.
* **Behavior:**
  - Auto-pause active sessions when headphones are unplugged.
  - Disable the Play button on the UI if no safe devices are detected, replacing it with a warnings banner.

---

## 5. Audio Focus & System Interruptions

* **Focus Requests:** Request transient audio focus via `AudioManager.requestAudioFocus()` using `AudioFocusRequest` (API 26+).
* **Focus Callback Reactions:**
  - `AUDIOFOCUS_LOSS`: Stop loopback service completely.
  - `AUDIOFOCUS_LOSS_TRANSIENT`: Pause audio capture, resume once focus is regained.
  - `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`: Pause loopback (ducking is not appropriate for active microphones).
* **Telephony Interruption:** Register a `TelephonyCallback` or `PhoneStateListener` to automatically pause recording during an incoming or active voice call.

---

## 6. Permissions Flow
1. Request `RECORD_AUDIO` on first launch or Play tap.
2. For devices on Android 13+ (API 33+), request `POST_NOTIFICATIONS` to permit the persistent Foreground Service notification.
3. For devices on Android 12+ displaying bluetooth device names, request `BLUETOOTH_CONNECT`.

---

## 7. Edge Cases & Verification Checklist

| # | Edge Case | Expected System Behavior |
|---|---|---|
| 1 | Play tapped without headphones | Show feedback warning dialog, allow play only after explicit user bypass. |
| 2 | Headphones disconnected during loopback | Auto-pause the service, update the notification, and return UI to warning state. |
| 3 | Incoming phone call | Pause loopback immediately. |
| 4 | Music player/YouTube started | System revokes audio focus; pause loopback. |
| 5 | Permission denied first time | Show clear explanation/rationale to the user. |
| 6 | Permission permanently denied | Display a button directing the user directly to App Settings. |
| 7 | Screen turned off | Loopback continues playing in the background. |
| 8 | Phone locked | Loopback continues playing in the background. |
| 9 | App swiped away from Recents list | Service must survive; persistent notification remains active. |
| 10| Tapped "Pause" in notification | Stop loopback service cleanly and remove the notification. |
| 11| Screen rotated | State preserved on ViewModel level; no audio glitches. |
| 12| Bluetooth routing switches (AirPods -> Speaker) | Detect speaker output and auto-pause. |
| 13| Doze / Battery saving mode active | Partial WakeLock keeps loop thread running without lag. |
| 14| Notification permission denied (Android 13+) | Block foreground service launch, prompt user to enable notifications in settings. |
| 15| Sample rate 48kHz unsupported | Fail over gracefully to 44.1kHz. |
| 16| AudioRecord fails to init | Display error UI state and provide a retry button. |

---

## 8. GNU GPL v3 Guidelines
* **License File:** The full unmodified GPL v3 license text must exist at `/LICENSE` in the root repository.
* **Copyright Headers:** Every source file (Kotlin class, build scripts) must contain the standard GPL v3 header.
* **About Screen Notice:** Display a brief GPL notice with links to the full license text and the public GitHub repository.
