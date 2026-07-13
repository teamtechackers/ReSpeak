# re:speak — Product Brief

## Project Overview
* **App Name:** re:speak
* **Platform:** Android (Kotlin)
* **License:** GNU GPL v3
* **Repository:** Public on GitHub with complete build instructions

## Purpose & Goal
re:speak is a **real-time audio loopback** application. It is designed to capture microphone input and immediately play it back through the user's earphones/headphones with the absolute lowest latency possible.

### Why this app?
By listening to their own voice in real-time, users can:
1. Become aware of their speech patterns, speed, and pronunciation.
2. Self-correct vocal quirks or speech issues (therapy/training support).
3. Practice public speaking or presentation skills.

---

## Core UX Flow
The user journey is designed to be extremely direct and distraction-free:
1. **Launch App:** The user opens a single-screen interface.
2. **Safety Check:** The app checks for connected earphones. If none are plugged in, a prominent warning is shown to discourage feedback.
3. **Start Session:** User connects earphones and taps the large central **Play** button.
4. **Active Session:** The user speaks, hearing their voice dynamically loop back into their ears. A subtle pulsing waveform displays on screen.
5. **End Session:** User taps **Pause**; the loopback stops, and the session ends cleanly.

---

## Critical Technical Constraints
1. **Background Playback (Screen Off / Phone Locked):** The audio loopback must continue running when the user turns off their screen or locks their phone to save battery during usage. This requires implementing an Android **Foreground Service** using the `mediaPlayback` type.
2. **Audio Feedback Prevention:** Playback must be blocked or heavily discouraged when headphones/earphones are not connected to avoid immediate loud acoustic feedback loop (mic picking up speaker output).
