# PitStopper

<img src="app_icon.png" width="128" alt="PitStopper App Icon" />

**Race pit window timer with visual alerts and GPS-based auto-stop for track days.**

---

## What is PitStopper?

PitStopper is the essential pit window timer for endurance racing and track day events with mandatory driver swaps.

During long endurance races, keeping track of pit windows while focusing on driving is nearly impossible. PitStopper solves this with a simple, high-visibility display that shows exactly when your pit window opens and how much time you have left.

## Features

### 🏁 Large, Readable Display
- Massive clock visible from anywhere in the cockpit
- High-contrast black and white design for bright sunlight
- Monospace font for precise time reading at a glance

### ⏱️ Smart Pit Window Alerts
- Configure your race start time and pit window schedule
- Set when the pit window opens (minutes after race start)
- Set pit window duration
- Automatic recurring window calculations throughout your race

### 🚨 Impossible-to-Miss Visual Alerts
- Full-screen green/black flashing when pit window is open
- Countdown timer shows time until window opens (or time remaining)
- Vertical progress bar shows your position in the current cycle

### 📍 GPS Auto-Stop
- Automatically detects when you've stopped in the pits
- Clears the alert so you know your stop was registered
- Battery-efficient: GPS only active during pit windows

### 📱 Race-Optimized Design
- Fullscreen immersive mode - no distractions
- Screen stays on during entire session
- Works offline - no internet required
- Simple settings - configure once and race

## How It Works

1. Set your race start time (e.g., 09:00)
2. Set when the pit window opens (e.g., 17 minutes after start)
3. Set the pit window duration (e.g., 6 minutes)
4. Mount your phone where the driver can see it
5. Focus on driving - PitStopper handles the timing

When your pit window opens, the screen flashes to alert you. The countdown shows exactly how much time remains. When you stop in the pits, GPS detects your standstill and confirms your stop. Then it automatically starts counting down to your next window.

## Perfect For

- Endurance racing (24h, 12h, 6h events)
- Track days with mandatory driver rotations
- Rental kart endurance races
- Any motorsport event with timed pit windows

## Installation

### From Google Play
*Coming soon*

### Direct APK Install
Download the latest APK from the [releases](releases/) folder and install it on your Android device.

**Requirements:**
- Android 7.0 (API 24) or higher

## Privacy

PitStopper uses GPS **only** to detect when you've stopped in the pits. Location data never leaves your device and is only accessed during active pit windows to save battery.

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/pitstopper.git
cd pitstopper

# Build debug APK
./gradlew assembleDebug

# Build release AAB (for Play Store)
./gradlew bundleRelease
```

## License

This project is licensed under the [Apache License 2.0](LICENSE).

---

## Changelog

### Version 1.0.3 (February 2026)

#### 🚀 Major Features

**Intelligent SpeedHive Integration**
- **AUTO Session Detection**: Automatically finds the right session containing your car number - no more manual session selection
- **Dynamic Session Switching**: Automatically transitions between sessions (e.g., qualifying → race) every 60 seconds
- **Event & Session Display**: Shows current event and session name at the top of the main screen (e.g., "Hagenberg Race - Training")

**Enhanced Demo Mode**
- **Fixed Critical Bugs**: Demo mode now works correctly (previously showed errors)
- **Car Selection Dropdown**: Choose from 8 realistic demo cars with driver names (#88 JOHNSON, #23 RACER-X, etc.)
- **Realistic Race Simulation**: 8-car race with evolving positions, lap time variations, and proper gap calculations

**Improved Live Timing Display**
- **Position with Total**: Shows your position relative to field size (P3/8 format)
- **Smart Text Sizing**: Total car count displays smaller to prevent layout issues in large races
- **Cleaner Gap Formatting**: Removed unnecessary + signs from gap behind values

#### 🔧 Technical Improvements

- **Simplified Settings**: Manual car number entry replaces complex dropdown for SpeedHive live mode
- **Mode-Specific Validation**: Proper validation logic for each SpeedHive mode (Off/Live/Demo)
- **Thread-Safe Session Tracking**: Background session detection with main thread UI updates
- **Constraint Barrier Layouts**: Dynamic field visibility without layout issues
- **Enhanced Error Handling**: Graceful degradation when session detection fails
- **All 42 Unit Tests Passing**: Comprehensive test coverage maintained

### Version 1.0.2 (February 2026)
- Scrollable settings screen for better keyboard handling

### Version 1.0.1 (February 2026)
- Fixed flash sequence to start with green for better visibility

### Version 1.0.0 (February 2026)
- Initial release with pit window timing, GPS standstill detection, and visual alerts

---

**Built by racers, for racers.** 🏎️

*Get to the pits on time, every time.*


