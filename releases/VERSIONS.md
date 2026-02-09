# PitStopper Version History

## Version 1.0.2 (February 9, 2026)

### Changes

- **Scrollable settings screen** - The settings activity is now wrapped in a ScrollView, allowing users to scroll when content doesn't fit on screen (e.g., when keyboard is open or on smaller devices).

---

## Version 1.0.1 (February 9, 2026)

### Changes

- **Fixed flash sequence** - When entering a pit window alert, the screen now starts with green instead of black for the first two seconds. This makes the alert more immediately noticeable.

---

## Version 1.0.0 (February 8, 2026)

**Initial Release** 🏁

### Features

- **Large Clock Display**
  - 120sp monospace font for maximum visibility
  - High-contrast black/white design for bright sunlight conditions
  - 24-hour format optimized for racing

- **Pit Window Timer**
  - Configurable race start time
  - Configurable pit window opening time (minutes after race start)
  - Configurable pit window duration
  - Automatic recurring pit window calculations

- **Visual Alert System**
  - Full-screen green/black flashing during open pit windows
  - Countdown timer showing time until window opens or time remaining
  - Vertical progress bar (96dp) showing position in current cycle

- **GPS Standstill Detection**
  - Automatically detects when vehicle stops in pits
  - Clears alert to confirm pit stop was registered
  - Battery-efficient: GPS only active during pit windows
  - Threshold: < 5.4 km/h for 5 seconds

- **Settings Screen**
  - Race start time picker
  - Pit window timing configuration
  - Settings persist across app restarts

- **User Experience**
  - Fullscreen immersive mode (no status bar or navigation)
  - Screen stays on during entire session
  - Works completely offline
  - Proper lifecycle management (pause/resume)

### Technical Details

- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36
- **Compile SDK:** 36
- **Java Version:** 11

### Files

- `pitstopper-1.0.0.aab` - Signed Android App Bundle for Google Play
- `pitstopper-1.0.0.apk` - APK for direct installation

---

*Built for endurance racers who can't afford to miss a pit window.*


