# re:speak — Designer Brief

This document serves as the visual specification for the designers of the **re:speak** application.

---

## 1. Logo & Identity
The identity of re:speak is built around simplicity and functional minimalism. 

* **Signature Concept:** The colon `:` in **re:speak** is the brand's signature element.
* **Concepts to Explore:**
  1. A colon representing a soundwave.
  2. The two dots of the colon acting as a speaker and an ear (input and output).
  3. A colon transitioning into a live audio pulse.
* **Required Logo Assets:**
  - **Adaptive Icon (Android):** Foreground layer (108x108dp) + Background layer with a 66dp safe zone.
  - **Legacy Icons:** All densities (mdpi 48px, hdpi 72px, xhdpi 96px, xxhdpi 144px, xxxhdpi 192px).
  - **Play Store Icon:** 512x512px PNG format.
  - **Themed Icon:** Monochrome variant compatible with Android 13+ themed home screens.
  - **Notification Silhouette:** A flat white icon (24x24dp) with transparent background. Android forces notification icons to be pure white silhouette on transparent background.
  - **Wordmark:** Scalable Vector Graphic (SVG) for the Splash/About screen.

---

## 2. Typography & Color Systems

### Color Palettes
The app must support both **Light Mode** and **Dark Mode** via `values-night` configurations. The colors should look premium and clean.

* **Option A: Calm Therapy (Recommended & Implemented)**
  - *Light Mode:* Soft grey background, soothing slate/blue text, and a calming teal/cyan active highlight.
  - *Dark Mode (Premium Visuals):*
    - **Background:** Vertical linear gradient starting from a deep slate-gray `Color(0xFF1E222B)` at the top, transitioning to pitch-black `Color(0xFF0B0D10)` at the bottom.
    - **Active Accent:** Vibrant neon teal `Color(0xFF00F5D4)` with soft glow shadows.
    - **Warning Accent:** Amber/Orange `Color(0xFFFF9F1C)` for alert states.
    - **Card Design:** Glassmorphic translucent panels with soft white borders (`Color.White.copy(alpha = 0.08f)`) and background blur.
* **Option B: Bold Minimalist**
  - *Light Mode:* Pure white background, bold black text, and a strong cobalt blue action color.
  - *Dark Mode:* Pitch black background, pure white text, and a neon blue action color.

### Typography
* **Font Family:** A premium modern font family like **Outfit** or **Inter**. If not imported, the system default should be styled with thin and semi-bold weights to look clean and premium.
* **Type Scale:**
  - *Display:* Large bold brand headers.
  - *Title:* Main headings and button labels.
  - *Body:* Description cards and configuration settings.
  - *Caption:* Status information, small timers, and licensing links.

---

## 3. Screen States (Single-Screen Architecture)

Since re:speak is a single-screen app, visual state clarity is extremely critical. Each state must be designed as a separate screen/frame.

### State A — Idle, Headphones Connected (Ready to Start)
* **Play Button:** Large circular button in the center (touch target min 96dp, visual size ~120dp). Contains a solid play icon.
* **Label:** "Tap to start listening" displayed below the button.
* **Status Pill:** Top-aligned chip stating the current connection (e.g., "🎧 AirPods Connected" or "🎧 Wired Headset").
* **Input Source Selector:** A glassmorphic tab toggle located in the lower-middle screen area allowing the user to switch between:
  - **Phone Mic** (uses phone's built-in microphone for high fidelity over standard A2DP audio link).
  - **Headset Mic** (uses Bluetooth headset microphone over SCO communication link).
* **Bottom Bar:** Subtle info/help icon leading to the About screen.

![State A Mockup](./images/respeak_state_a_idle.jpg)

### State B — Active (Mic Loopback Recording)
* **Pause Button:** The central button morphs smoothly from Play into a Pause icon (two parallel lines).
* **Audio Pulse:** A live, audio-reactive waveform or breathing ring pulsing around the button, reflecting vocal amplitude.
* **Timer:** A digital timer showing elapsed time in `mm:ss` format directly under the button.
* **Status Pill:** "🎧 AirPods · Live" with a small green status indicator.

![State B Mockup](./images/respeak_state_b_active.jpg)

### State C — Warning / No Headphones Connected (State 4)
* **Theme:** Light Theme (solid white background `#FFFFFF`).
* **Header:** Horizontal brand logo (`logo_horizontal.png`) centered at the top.
* **Pill:** `[⚠️ Icon] No Headphones Detected` (light orange `#FFF5EB` background, orange text `#FF9F1C`).
* **Visuals:** Center-aligned locked/disabled play button widget (`btn_play_disabled.png`) inside concentric wave lines.
* **Texts:**
  - Header: Bold black `"Connect Earphones"`.
  - Subtitle: Gray description detailing safety.
* **Buttons:** Stacked buttons:
  - `"Continue Anyway"` (dark teal `#042C34` background, white text).
  - `"Reconnect"` (white background, dark teal border `#042C34`, dark teal text `#042C34`).

### State D — Earphones Disconnected Mid-Session (State 5)
* **Theme:** Light Theme (solid white background `#FFFFFF`).
* **Pill:** `[⚠️ Icon] Earphones disconnected` (light red background `#FFEBEB`, text red `#FF4D4D`).
* **Visuals:** Center-aligned locked/disabled play button widget (`btn_play_disabled.png`) inside concentric wave lines.
* **Texts:**
  - Header: Bold black `"Connect Earphones"`.
  - Subtitle: Gray description detailing safety.
* **Buttons:** Stacked buttons:
  - `"Reconnect"` (dark teal `#042C34` background, white text).
  - `"Cancel Session"` (white background, red border `#FF4D4D`, red text `#FF4D4D`).

### State E — Permissions Required (State 1)
* **Theme:** Light Theme (solid white background `#FFFFFF`).
* **Header:** Horizontal brand logo (`logo_horizontal.png`) centered at the top, with a glassmorphic pill button underneath saying `Permission Required`.
* **Visuals:** Center-aligned microphone illustration with padlock (`mic_permission_illustration.png`) inside thin concentric sound wave circular graphics.
* **Text Info:**
  - Heading: Bold black `"Microphone Access Required"`.
  - Subtitle: Gray description detailing the necessity of microphone access.
* **Privacy Card:** Light glassmorphic teal/blue card: `"Your privacy matters / Your voice is processed live on your device and is never recorded or uploaded."` with a shield check icon.
* **Button:** Prominent dark teal `#042C34` full-width button with a microphone icon and text `"Grant Permission"`. Clicking it launches the system permission prompt.

### State F — Onboarding Screen
* **Theme:** Light Theme (solid white background `#FFFFFF`).
* **Multi-Slide Wizard:**
  1. **Slide 1 — Awareness:**
     - Visuals: Illustration of a guy wearing earphones (`onboarding_1.png`).
     - Heading: `"Hear Yourself\nin Real Time"`.
     - Subtitle: `"re: speak plays your voice back to you instantly so you can become more aware of your speech."`.
     - Indicator: Page 1 active (`● ○ ○`).
  2. **Slide 2 — Safety / Feedback Loop:**
     - Visuals: Feedback loop graphic showing mic to ear loop (`onboarding_2.png`).
     - Heading: `"Use Earphones to\nAvoid Feedback"`.
     - Subtitle: `"Wearing earphones prevents the mic from picking up the playback and ensures a clear, feedback - free experience."`.
     - Indicator: Page 2 active (`○ ● ○`).
  3. **Slide 3 — Growth / Completion:**
     - Visuals: Play button with icon bubbles illustration (`onboarding_3.png`).
     - Heading: `"Reflect . Improve\n. Grow"`.
     - Subtitle: `"Listen to your patterns, build confidence , and communicate with more clarity."`.
     - Indicator: Page 3 active (`○ ○ ●`).
     - Button: `"Get Started"`.
* **Flow:** Displayed immediately after the Splash Screen. Tapping `"Next"` progresses slides; Slide 3 `"Get Started"` navigates to the main loopback dashboard.

### State G — Audio Focus Interrupted (State 6)
* **Theme:** Light Theme (solid white background `#FFFFFF`).
* **Pill:** `[⚠️ Icon] Audio Interrupted` (light orange background `#FFF5EB`, text orange `#FF9F1C`).
* **Visuals:** Center-aligned play button widget inside concentric waves.
* **Texts:**
  - Timer: Ticking paused timer (e.g. `"00:52"`).
  - Status: Bold orange `"Paused"`.
  - Subtitle: Gray description `"Another app is using audio"`.
* **Overlay/Card:** Light card at the bottom: `"Audio Focus lost / re:speak is paused because another app needs audio right now"` with a music note icon.

### State H — Splash Screen (Launch Screen)
* **Background Color:** Solid dark teal `#042C34`.
* **Visuals:** Concentric thin circular sound waves radiating from the center.
* **Logo:** Centered brand logo containing the dots and vertical waveform graphic, with the text "re : speak" (cyan and white) and tagline "Listen . Reflect . Improve" below it.
* **Duration:** Displayed for 2.5 seconds on app launch before transitioning to the main loopback screen.

---

## 4. Notification & About Screens

### Foreground Notification
Must be persistent while loopback is active:
* **Icon:** Notification silhouette icon (white).
* **Title:** `re:speak`
* **Body:** `Listening · 02:14` (updates dynamic timer).
* **Action:** A `"Pause"` button allowing the user to pause directly from the lockscreen.

### About Screen
* **Layout:** Fullscreen screen with back arrow navigation `[←]`.
* **Branding:** Centered horizontal brand logo (`logo_horizontal.png`) with dark green/teal tagline `"Hear yourself . Improve yourself"` underneath.
* **About Text:** Bold black heading `"About re: speak"` and description text panel.
* **List Settings Container:** Card showing settings rows:
  - Version: `1.0.0`
  - License: `GNU GPL v3`
  - GitHub: Link with external icon.
  - Report an issue: Link with external icon.
  - Privacy: Navigation link.
* **Footer:** copyright and license summary centered at the bottom.
