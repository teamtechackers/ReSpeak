# re:speak — Dependencies & Libraries Document

This document lists all libraries, frameworks, and tools used in the **re:speak** project, along with their roles and license verification (ensuring strict **GNU GPL v3 compatibility**).

---

## 1. List of Dependencies

| Library / Dependency | Role in Project | License | GPL v3 Compatible? |
|---|---|---|---|
| **Kotlin Standard Library** | Core language constructs, collections, extensions. | Apache 2.0 | **Yes** |
| **Kotlin Coroutines & Flow** | Manages async tasks, background timers, and reactive StateFlow streams. | Apache 2.0 | **Yes** |
| **Jetpack Compose UI** | Modern declarative UI engine used to build the single-screen layouts. | Apache 2.0 | **Yes** |
| **Material 3 (Compose)** | Material Design 3 cards, warning banners, pulsing indicators, and icons. | Apache 2.0 | **Yes** |
| **Lifecycle Runtime Compose** | Binds Kotlin flows safely to Compose lifecycle states (prevents memory leaks). | Apache 2.0 | **Yes** |
| **Lifecycle ViewModel Compose** | Integrates architecture components ViewModels with the Compose UI hierarchy. | Apache 2.0 | **Yes** |
| **AndroidX Core KTX** | Core Android system utility extension functions (e.g. permission checks). | Apache 2.0 | **Yes** |
| **Audio Engine (Native API)** | Direct access to native Android audio recording and playback (`AudioRecord`, `AudioTrack`). | Android SDK | **Yes** |

---

## 2. Key Library Selections & Rationale

### A. Zero Third-Party Audio Libraries (Native SDK)
We will **not** import third-party libraries for the audio loopback. 
* *Why:* By using native `android.media.AudioRecord` and `android.media.AudioTrack` APIs:
  - We keep the binary footprint extremely small.
  - We don't have to deal with complex C++ NDK toolchains (which are required for libraries like Google Oboe).
  - The Native APIs natively support hardware Acoustic Echo Cancellation (AEC) and Noise Suppression (NS) when the audio source is set to `VOICE_COMMUNICATION`.

### B. State Observation (Lifecycle Runtime Compose)
We use `androidx.lifecycle:lifecycle-runtime-compose` specifically for `collectAsStateWithLifecycle()`:
* *Why:* In Compose, standard flow collection (`collectAsState()`) continues executing even when the app goes into the background. Using `collectAsStateWithLifecycle()` ensures that the UI stops wasting CPU cycles to update itself when the phone is locked or screen is off, while the background service handles the audio.

---

## 3. GPL v3 License Compliance Check
According to Section 2.9 of the Product Brief, all dependencies must be compatible with the **GNU GPL v3** copyleft license.
- **Apache 2.0 Compatibility:** The Free Software Foundation (FSF) explicitly confirms that the Apache 2.0 license is compatible with GNU GPL v3. Since AndroidX, Material 3, and Kotlin are Apache 2.0 licensed, we can legally use and distribute them in our GPL v3 application.
- **Proprietary SDKs:** We will strictly avoid any proprietary audio SDKs or libraries to keep the code fully open-source and distributable.
