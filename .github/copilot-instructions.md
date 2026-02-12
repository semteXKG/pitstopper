# PitStopper - Android Race Pit Window Timer

PitStopper is an Android motorsports application designed for endurance racing and track days with mandatory driver swaps. It provides visual alerts during pit windows with GPS-based auto-clearing functionality and intelligent SpeedHive live timing integration.

## Build, Test & Development Commands

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release AAB (for Play Store)
./gradlew bundleRelease

# Clean project
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run unit tests for specific class
./gradlew test --tests "PitWindowAlertManagerTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Project Configuration
- **Namespace**: `at.semmal.pitstopper`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Java Version**: 11
- **Key Dependencies**: Material Design, ConstraintLayout, Google Play Services Location

## High-Level Architecture

### Core Components

**MainActivity** - Central activity with fullscreen immersive mode handling:
- Large clock display (120sp monospace font)
- Visual flashing alerts during pit windows
- Countdown timer and progress bar UI
- SpeedHive live timing integration (optional)
- GPS standstill detection integration

**PitWindowAlertManager** - Core timing logic with state management:
- Calculates recurring pit windows based on race start time
- Maintains IDLE/ON_ALERT states with window-specific suppression
- Handles alert clearing with GPS integration
- Returns progress percentage for UI progress bars

**StandstillDetector** - GPS-based auto-pause functionality:
- Monitors vehicle speed using FusedLocationProviderClient
- Triggers alert clearing when stationary (< 5.4 km/h for 5+ seconds)
- Battery-efficient: only active during ON_ALERT states

**SpeedHive Integration** - Live timing system with AUTO session detection:
- Real API mode: Connects to MyLaps SpeedHive API with intelligent session detection
- Demo mode: 8-car simulated race with realistic position changes
- AUTO mode: Automatically finds the latest live session containing your car number
- Manual mode: Allows specific session selection for expert users
- Displays position, gap ahead, gap behind with color-coded trends
- 10-second polling interval with background threading

### State Management Pattern

The app uses a sophisticated state management system centered around `PitWindowAlertManager`:

```java
// IDLE → ON_ALERT when entering pit window
// ON_ALERT → IDLE when exiting window or clearAlert() called
AlertState state = alertManager.getAlertState(hour, minute);

// GPS integration clears alerts automatically
if (gpsDetectsStandstill()) {
    alertManager.clearAlert(); // Suppresses for current window only
}
```

Window suppression ensures once an alert is cleared (driver has pitted), it won't resume until the next pit window cycle.

## Key Conventions

### File Organization
- **Core logic**: `app/src/main/java/at/semmal/pitstopper/`
- **Main classes**: `MainActivity.java`, `PitWindowAlertManager.java`, `StandstillDetector.java`
- **SpeedHive classes**: `SpeedHiveManager.java`, `DemoSpeedHiveManager.java`, `LiveTimingData.java`
- **UI helpers**: `*SpinnerAdapter.java`, `PitWindowPreferences.java`
- **Tests**: `app/src/test/java/at/semmal/pitstopper/PitWindowAlertManagerTest.java`

### Configuration Management
Uses SharedPreferences via `PitWindowPreferences` class for persistence:
- Race timing settings (start time, pit window timing)
- SpeedHive API configuration (mode, credentials, event/session/car selection)
- AUTO session detection preferences
- All settings accessible through simplified `SettingsActivity`

### SpeedHive Session Management
**AUTO Mode** (Default): 
- User selects event and enters car number
- App automatically detects latest live session containing that car
- Falls back to any session if car not found in active sessions
- Provides clear error messages if car not found anywhere

**Manual Mode**: 
- Expert users can override AUTO and select specific sessions
- Preserves full control for complex racing scenarios
- Maintains backward compatibility with existing configurations

### API Credentials
SpeedHive API credentials stored in `app/src/main/assets/speedhive.properties` (gitignored):
```properties
api.base.url=https://api.speedhive.mylaps.com
subscription.id=YOUR_SUBSCRIPTION_ID
api.key=YOUR_API_KEY
```

### Threading Pattern
- **Main thread**: All UI updates
- **Background thread**: Network calls (SpeedHive API, session auto-detection)
- **Handler/Runnable**: Timer updates every second
- **GPS callbacks**: Always dispatched to main thread via Handler

### Testing Approach
Comprehensive unit testing for `PitWindowAlertManager` with 42+ test cases covering:
- State transitions and window calculations
- Alert suppression and clearing
- Progress percentage calculations
- AUTO session detection logic
- Edge cases (hour boundaries, multiple calls)

### Visual Design Principles
- **High contrast**: Black/white/green for sunlight visibility
- **Large fonts**: 120sp for main clock, 60sp for countdown
- **Monospace fonts**: Precise time reading
- **Immersive fullscreen**: No status bar or navigation distractions
- **Battery optimization**: GPS only during alerts, efficient polling

### Settings UI Simplification (NEW)
**Simplified User Experience**:
1. Select SpeedHive Mode (Off/SpeedHive/Demo)
2. Choose Event from dropdown
3. Select Session: AUTO (recommended) or specific session
4. Enter Car Number (numeric input with validation)

**AUTO Session Benefits**:
- Eliminates guesswork about which session to select
- Automatically adapts to race format changes
- Reduces configuration errors
- Perfect for drivers who just want it to work

### Permissions & Privacy
- `ACCESS_FINE_LOCATION`: GPS standstill detection only
- `WAKE_LOCK`: Keep screen on during race sessions
- Location data never leaves device
- GPS monitoring only active during pit window alerts for battery efficiency