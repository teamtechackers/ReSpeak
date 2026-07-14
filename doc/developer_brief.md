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

The loopback operates by reading PCM bytes from `AudioRecord` and writing them instantly into `AudioTrack` on a dedicated high-priority audio thread. The exact routing is determined by the **Input Source Selector**:

### Option 1: Phone Mic (High Quality Mode)
* **Audio Source:** `MediaRecorder.AudioSource.MIC`
* **AudioManager Mode:** `AudioManager.MODE_NORMAL`
* **AudioTrack Usage:** `AudioAttributes.USAGE_MEDIA`
* **Bluetooth Profile:** Standard A2DP playback (captures from phone mic, plays 48kHz stereo to headphones).

### Option 2: Headset Mic (Communication Mode)
* **Audio Source:** `MediaRecorder.AudioSource.VOICE_COMMUNICATION`
* **AudioManager Mode:** `AudioManager.MODE_IN_COMMUNICATION`
* **AudioTrack Usage:** `AudioAttributes.USAGE_VOICE_COMMUNICATION`
* **Bluetooth Profile:** Bluetooth SCO/HFP routing enabled (captures from headset mic, plays 16kHz mono to headphones).

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
* **Service Lifecycle:** Controlled by UI intents (`ACTION_START`, `ACTION_STOP`). Returns `START_STICKY` in `onStartCommand` to request system restart if killed.
* **Persistent Notification:** Shows a low-importance notification with a dynamic timer and an action button to "Pause" the loopback immediately from the lock screen.
* **Swipe-Kill Survival:** Implements `onTaskRemoved` to schedule a prompt restart via `AlarmManager` (1 second delay) if the user clears the app from Recents while active.
* **Power Management (WakeLock):** Acquire a `PowerManager.PARTIAL_WAKE_LOCK` when the loopback starts, and release it when it stops. This prevents CPU throttling by aggressive OEM battery optimization layers.

---

## 4. Headphone & Audio Device Detection

To prevent earsplitting feedback loops, the app must actively monitor output paths.

* **API Check:** Use `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)`.
* **Safe Device Types:**
  - `TYPE_WIRED_HEADPHONES`
  - `TYPE_WIRED_HEADSET`
  - `TYPE_USB_HEADSET`
  - `TYPE_BLUETOOTH_A2DP`
  - `TYPE_BLUETOOTH_SCO`
  - `TYPE_BLE_HEADSET`
  - `TYPE_BLE_BROADCAST`
  - `TYPE_HEARING_AID`
* **Real-time Monitoring:** Register an `AudioDeviceCallback` using `AudioManager.registerAudioDeviceCallback()`.
* **Behavior:**
  - Auto-pause active sessions when headphones are unplugged. Includes a **1.5-second debounce delay** to prevent false disconnect stops during background audio-routing transitions.
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

---

## 9. Splash Screen Implementation Details
* **Compose-First Approach:** Implemented inside `MainActivity.kt` as an entry state layer.
* **State Management:** A local Boolean flag `showSplash` controls the splash overlay. On launch, a `LaunchedEffect(Unit)` with `delay(2500)` triggers the transition to `showSplash = false`.
* **Asset Integration:** The logo is loaded from `R.drawable.logo` (`logo.png`).
* **Concentric Wave Animation:** Drawn inside a `Canvas` drawing concentric circles with dynamic or thin low-opacity outlines: `Color.White.copy(alpha = 0.03f)` to simulate the sound waves radiating from the logo.
* **Colors & Typography:** Matches theme color `#042C34`. Title displays "re : speak" with tagline "Listen . Reflect . Improve" using standard Roboto or Outfit font styles.
* **About Screen Notice:** Display a brief GPL notice with links to the full license text and the public GitHub repository.

---

## 10. Onboarding Screen Implementation Details
* **Theme:** Light Theme style (solid white background `#FFFFFF`).
* **State Management:** A local Boolean flag `showOnboarding` controls display. Inside `OnboardingScreen`, a local `currentSlide` integer state (0 to 2) maps the active view.
* **Transition Flow:**
  - Launch -> `SplashScreen` (2.5s delay) -> `OnboardingScreen` (Light Theme, Slide 0) -> Click Next -> Slide 1 -> Click Next -> Slide 2 -> Click Get Started -> `MainScreen` (Dark Theme).
* **Asset Integration:**
  - Slide 1: Illustration `R.drawable.onboarding_1` (`onboarding_1.png`).
  - Slide 2: Illustration `R.drawable.onboarding_2` (`onboarding_2.png`).
  - Slide 3: Illustration `R.drawable.onboarding_3` (`onboarding_3.png`).
* **Visual Components:**
  - **Slide 1:** Header: `"Hear Yourself\nin Real Time"`, Description: `"re: speak plays your voice back to you instantly so you can become more aware of your speech."`, Indicator: `● ○ ○`.
  - **Slide 2:** Header: `"Use Earphones to\nAvoid Feedback"`, Description: `"Wearing earphones prevents the mic from picking up the playback and ensures a clear, feedback - free experience."`, Indicator: `○ ● ○`.
  - **Slide 3:** Header: `"Reflect . Improve\n. Grow"`, Description: `"Listen to your patterns, build confidence , and communicate with more clarity."`, Indicator: `○ ○ ●`. Button text is `"Get Started"`.

---

## 11. Permission Screen Implementation Details
* **Theme:** Light Theme (solid white background `#FFFFFF`).
* **Visual Components:**
  - Top header: Horizontal brand logo `R.drawable.logo_horizontal` (`logo_horizontal.png`).
  - Permission required indicator pill: Row showing lock icon and `"Permission Required"` in dark teal (`#042C34`) over light teal tint (`#E8F1F2`).
  - Circular wave microphone illustration: Microphone with lock graphic `R.drawable.mic_permission_illustration` (`mic_permission_illustration.png`) centered inside concentric wave paths.
  - Header & Subtitle: `"Microphone Access Required"` (bold black, 24sp) and explanation text.
  - Privacy Card: Light blue/teal background card containing a shield/secure layout stating the voice data processed live is not recorded or uploaded.
  - Action Button: Full-width button with microphone icon on the left, dark teal `#042C34` background, white text `"Grant Permission"`. Clicking it fires the system permission request dialog.

---

## 12. Mid-Session Disconnect, Interruptions & About Screen Details
* **State 5 (Earphones Disconnected Mid-Session):**
  - Triggers if `!isHeadsetConnected` and the session was active before the disconnection (`wasActiveBeforeDisconnect == true`).
  - Pill label: Red `"Earphones disconnected"` on light red `#FFEBEB`.
  - Buttons: Stacked `"Reconnect"` (dark teal `#042C34` background) and `"Cancel Session"` (white background, red border, red text).
* **State 6 (Audio focus lost):**
  - Triggers if `loopbackState is LoopbackState.FocusLost`.
  - Pill label: Orange `"Audio Interrupted"` on light orange `#FFF5EB`.
  - Texts: Ticking paused timer, bold orange `"Paused"` status, subtitle explanation.
  - Bottom card: Rounded card details `"Audio Focus lost"` with music note icon.
* **About Screen:**
  - Full-screen view toggle with a back navigation arrow `[←]`.
  - Shows brand logo `logo_horizontal.png`, tag line, about copy, version information, license details, external links to GitHub repo, issue tracker, and privacy terms.
