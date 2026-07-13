# re:speak — Functional Flow & How It Works

This document explains the internal mechanisms, audio routing, and system logic that drive the **re:speak** application's functionality.

---

## 1. Core Functionality: Real-time Audio Loopback

The main function is to capture sound from the microphone and immediately play it back through the earphones with negligible delay (latency).

```
[ User Speaks ] 
       │
       ▼
 🎤 Microphone (Audio Source)
       │ (Captured as raw PCM bytes)
       ▼
 🧵 High-Priority Audio Thread (Buffer: read -> write)
       │ (Direct memory copy, no digital processing delay)
       ▼
 🎧 Earphones (Audio Output)
```

### The Code Loop:
1. When the user taps **Play**, a dedicated thread starts running at maximum system priority.
2. An `AudioRecord` object opens the microphone stream and captures raw audio in small chunks of memory (buffers).
3. An `AudioTrack` object immediately accepts these memory chunks and streams them to the earphones.
4. This read-and-write process repeats infinitely (every few milliseconds) inside a loop until paused.

---

## 2. Background Persistence (Why it works when locked)

Normally, Android kills background tasks or puts them to sleep to save battery. To prevent this, the app uses a **Foreground Service**:
1. When loopback starts, the app launches `AudioLoopbackService`.
2. This service displays a **persistent, non-dismissible notification** on the lock screen. This signals to Android that the app is performing an active task (like a music player).
3. The service acquires a **Partial WakeLock**, which keeps the phone's CPU running even if the screen turns completely black and the phone goes to sleep.
4. Tapping "Pause" inside the app or directly on the notification sends an exit command to the service, releasing the CPU lock and shutting down the service cleanly.

---

## 3. Safety Check: Feedback Loop Prevention

Because the microphone and the phone speaker are physically close to each other, playing microphone audio through the phone's built-in speaker will create a **feedback loop** (a loud, high-pitched screeching sound).

```
   ┌─────────────────────────────────────────┐
   ▼                                         │
🎤 Mic ───[ Captured Audio ]───► Speaker ────┘ (Loud Feedback Loop!)
```

### The Earphones Logic:
1. The app registers an `AudioDeviceCallback` with the Android OS.
2. The OS continuously reports the list of active output devices.
3. If the list contains a **headset, USB earphones, or Bluetooth earbuds**, the state is marked as **Safe**. The user is allowed to start loopback immediately.
4. If only the built-in speaker or standard HDMI is active, the state is marked as **Unsafe**. The Play button is disabled, and a warning is shown.
5. If the user unplug/disconnects their earphones during a live session, the callback detects this instantly, pauses the loopback thread, and updates the UI state to prevent feedback.

---

## 4. Audio Focus (Sharing with other apps)

Android uses "Audio Focus" to manage which app gets to use the microphone and speakers at any given time.
1. Before starting, re:speak requests **Transient Audio Focus**.
2. If another app (like YouTube or Spotify) starts playing, or if the user receives a **phone call**:
   - The OS tells re:speak that it has **lost focus**.
   - re:speak automatically pauses the loopback.
3. Once the phone call ends or the other app stops, re:speak detects that focus is returned and resumes the audio loop.
