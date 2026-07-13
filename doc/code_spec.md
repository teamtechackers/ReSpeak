# re:speak — Code & Functionality Specifications

This document defines the exact contents, responsibilities, and functionalities of each class and component we will build in the **re:speak** project.

---

## 1. UI Layer (Presentation)

### MainActivity.kt
* **Code Type:** Jetpack Compose UI Views (`@Composable` functions).
* **Role:** The entry point of the UI. It renders the screen and coordinates system permissions.
* **Core Code & Functionality:**
  - **`ReSpeakApp()`**: Core screen wrapper. Observes `uiState` from `MainViewModel` and renders the correct layout frame (Idle, Active, Warning, or Permissions).
  - **`PlayPauseButton()`**: Custom Compose Canvas drawing. It morphs shapes (Play triangle to Pause double-lines) and draws the pulsing audio-reactive ring when recording is active.
  - **`WarningCard()`**: Displays the warning banner when headphones are missing.
  - **Permission Checkers**: Registers `ActivityResultContracts.RequestPermission` to request `RECORD_AUDIO` and `POST_NOTIFICATIONS` at runtime.

### MainViewModel.kt
* **Code Type:** AndroidX Lifecycle ViewModel.
* **Role:** Manages the UI state and acts as a bridge between UI events and Business logic.
* **Core Code & Functionality:**
  - **`uiState` Flow:** Exposes a read-only StateFlow representing the screen state:
    - `isHeadsetConnected`: Boolean
    - `loopbackActive`: Boolean
    - `sessionDuration`: Formatted String (`mm:ss`)
    - `deviceName`: String
  - **`toggleLoopback()`**: Decides whether to start or stop loopback based on current state.
  - **Timer Control**: Automatically tracks the recording elapsed time when the service is active and formats it for UI consumption.

---

## 2. Business Logic Layer (Domain)

### AudioRepository.kt (Interface)
* **Code Type:** Pure Kotlin Interface.
* **Role:** Decouples the UI ViewModel from Android System Service APIs for better testability.
* **Functionality:**
  - `fun startLoopback()`
  - `fun stopLoopback()`
  - `val isServiceActive: StateFlow<Boolean>`
  - `val headsetStatus: StateFlow<Boolean>`

---

## 3. Data & System Layer (Infrastructure)

### AudioRepositoryImpl.kt
* **Code Type:** Kotlin implementation class.
* **Role:** Controls the system-level foreground service lifecycle and routes status flows.
* **Core Code & Functionality:**
  - Uses `Context.startForegroundService` to launch the service.
  - Uses `Context.stopService` to terminate it.
  - Bridges state flows from `AudioLoopbackService` and `AudioDeviceManager` to the Domain layer.

### AudioLoopbackService.kt
* **Code Type:** Android Foreground `Service` with `mediaPlayback` type.
* **Role:** Owns the low-latency background audio thread lifecycle.
* **Core Code & Functionality:**
  - **Foreground Lifecycle**: Runs `startForeground()` with a persistent lock screen notification.
  - **Notification Handler**: Builds and updates the notification showing the timer and containing a PendingIntent action for the "Pause" button.
  - **WakeLock Manager**: Requests `PARTIAL_WAKE_LOCK` via `PowerManager` to prevent CPU suspension.
  - **Audio Routing**: Sets `AudioManager.mode = MODE_IN_COMMUNICATION` to prioritize sound latency.

### AudioEngine.kt
* **Code Type:** Low-level Audio API controller.
* **Role:** Reads raw mic bytes and writes them to speaker output directly.
* **Core Code & Functionality:**
  - **`AudioRecord` Init**: Setup with `VOICE_COMMUNICATION` at 48000Hz, 16-bit PCM, Mono.
  - **`AudioTrack` Init**: Setup in `PERFORMANCE_MODE_LOW_LATENCY` streaming mode.
  - **Effect Activators**: Instantiates `AcousticEchoCanceler` and `NoiseSuppressor` matching the record session ID to isolate voice feedback.
  - **High-Priority Loop**: Launches an urgent background loop:
    ```kotlin
    while (isLooping) {
        val read = audioRecord.read(buffer, 0, buffer.size)
        if (read > 0) audioTrack.write(buffer, 0, read)
    }
    ```

### AudioDeviceManager.kt
* **Code Type:** System Hardware Callback listener.
* **Role:** Monitors headphones connectivity in real-time.
* **Core Code & Functionality:**
  - **`AudioDeviceCallback`**: Listens for connection/disconnection of wired or wireless audio equipment.
  - **Safety Validator**: Filters output devices based on safety types (allowing headphones, blocking speakers).
