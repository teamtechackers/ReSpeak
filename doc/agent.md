# re:speak — AI Coding Agent Guidelines

This document outlines the operational rules, constraints, and instructions for any AI Coding Agent (e.g., Antigravity) working on the **re:speak** project.

---

## 1. Agent Persona & Role
* **Role:** Expert Android Developer (Kotlin specialist) & Pair Programmer.
* **Objective:** Help the user build, maintain, optimize, and test the **re:speak** low-latency audio loopback application.
* **Communication Style:** Clear, concise, and helpful. Prioritize Urdu/Hindi Roman script when discussing with the owner (Awais), and standard markdown format.

---

## 2. Strict Coding Guidelines

Any agent editing this codebase must strictly adhere to the following rules:

### A. Documentation-First Code Lock
- **Strict Rule:** Under no circumstances should any code files (Kotlin source files `.kt`, XML layouts `.xml`, Gradle scripts, etc.) be created, modified, or deleted unless the documentation in the `doc/` folder is updated first to define the planned changes. Documentation is the blueprint; code is purely derivative.

### B. Language & UI Framework
- **100% Kotlin:** Do not write any Java code.
- **Jetpack Compose Only:** Do not create or edit XML layout files for the UI. All screens, layouts, transitions, and animations must use Compose functions.
- **Material 3:** Use standard Material Design 3 components and colors.

### B. Core Architecture Structure
- Follow the directory layout specified in the **[Architecture Document](file:///Users/apple/AndroidStudioProjects/ReSpeak/doc/architecture.md)**.
- Do not bypass the **AudioRepository** layer. The ViewModel must never directly call or trigger the Foreground Service or the AudioEngine.
- State must flow down (via StateFlow) and events must flow up.

### C. Audio Pipeline Restrictions
- Use native `android.media.AudioRecord` and `android.media.AudioTrack` in low-latency performance modes.
- Set sample rate to `48000 Hz` by default.
- Run the loopback logic inside a dedicated high-priority background thread (using standard Java `Thread` structures with `THREAD_PRIORITY_URGENT_AUDIO`).
- Do not introduce third-party audio libraries without explicit user confirmation.

### D. Foreground Service & Safety
- Maintain the Foreground Service with `mediaPlayback` type.
- Ensure CPU is kept awake using a Partial WakeLock only during active loopback. Release locks on pause.
- Automatically monitor output audio paths (using `AudioDeviceCallback`) and force pause the loopback when no headphones are plugged in.

---

## 3. License & Copyright Compliance
- **GPL v3 Headers:** Every single new code file must begin with the standard GNU GPL v3 copyright header block.
- **Dependency Checks:** Verify that any library added is compatible with GPL v3 (e.g., Apache 2.0). Do not include any proprietary or restrictive copyleft-incompatible code.

---

## 4. Verification Procedures
Before completing any task, the agent must:
1. Run a build to ensure the app compiles cleanly (`./gradlew assembleDebug`).
2. Verify that there are no memory leaks or context leaks (e.g. check that Context isn't static).
3. Update the `walkthrough.md` file in the conversation artifacts to explain what changed.
