# Project-Scoped Rules for re:speak

This file defines constraints and rules for AI Coding Agents working on the **re:speak** project.

## Behavioral Constraints
- **Documentation-First Code Lock:** Under no circumstances should any code files (Kotlin `.kt`, XML layouts, Gradle scripts, etc.) be created, modified, or deleted unless the corresponding documentation files in the `doc/` directory are updated first to define the planned changes.
- **Strictly No Code Changes Without Approval:** The user wants documentation and design finalized before any Kotlin code files are written or modified. Do not write or edit `.kt`, `.xml`, or gradle files unless the user explicitly requests code implementation.
- **Language & UI Toolkit:** 
  - Written code must be 100% Kotlin. No Java.
  - UI must use Jetpack Compose with Material 3. Do not create or edit XML layout files.
- **Architecture Guidelines:**
  - Follow MVVM + Clean Architecture package structure.
  - Decouple Jetpack Compose UI from the Foreground Service using the `AudioRepository` as the single source of truth.
- **Audio Routing & Safety Rules:**
  - Native AudioRecord/AudioTrack APIs in low-latency mode only.
  - Default sample rate is 48000 Hz.
  - Always enforce headphone detection via `AudioDeviceCallback` to prevent speaker feedback loops.
- **Licensing & GPL v3 Compliance:**
  - The project is licensed under GNU GPL v3.
  - Ensure every source file has the GPL v3 copyright header.
  - Verify all third-party dependencies are GPL-compatible.
- **Communication Style:**
  - Keep explanations concise.
  - Communicate with the user in Roman Urdu / Hindi when answering queries.
