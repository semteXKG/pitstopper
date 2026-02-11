# Pitstopper - Trackday Application TODO List

## Project Overview
**Target Platform:** Android  
**Screen Orientation:** Landscape (optimized)  
**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 36  
**Compile SDK:** 36  
**Java Version:** 11  
**Project Status:** ✅ COMPLETE (February 2026)

---

## Phase 1: Core Setup & Time Display

### 1.1 Project Configuration
- [x] Project structure created with proper namespace (`at.semmal.pitstopper`)
- [x] Add ConstraintLayout dependency for flexible landscape layouts
- [x] Add permissions for future features (ACCESS_FINE_LOCATION for GPS)
- [x] Enable View Binding or Data Binding in build.gradle.kts
- [x] Keep screen on during app usage (add WAKE_LOCK permission)

### 1.2 Main Activity Setup
- [x] Create `MainActivity.java` in `app/src/main/java/at/semmal/pitstopper/`
- [x] Create `activity_main.xml` layout file (optimized for landscape)
- [x] Create landscape-specific layout variant in `res/layout-land/` for enhanced UX
- [x] Create portrait layout variant in `res/layout-port/` for basic support
- [x] Configure MainActivity in AndroidManifest.xml with:
  - Launcher intent
  - Fullscreen/immersive mode flags
  - Keep screen on flag

### 1.3 Time Display UI
- [x] Design large, easily readable time display for landscape view
- [x] Create TextView for current time with large font size (60-80sp minimum)
- [x] Use monospace font or digital-style font for better readability
- [x] Add proper contrast colors for visibility in daylight conditions
- [x] Position time display prominently in the UI (top-center recommended)

### 1.4 Time Display Logic
- [x] Implement Handler/Runnable pattern for updating time every second
- [x] Format time according to system locale (or 24h format for racing)
- [x] Handle lifecycle events properly (pause/resume updates)
- [x] Test time display accuracy and performance

---

## Phase 2: Driver Swap Alert System (Core Feature)

### 2.1 Alert Configuration Model
- [x] Create settings UI with configuration options
- [x] Add time picker for race start time
- [x] Add pit window opens after (minutes) configuration
- [x] Add pit window duration (minutes) configuration
- [x] Create `PitWindowConfig.java` class to store:
  - Race start time (hour, minute)
  - Pit window opens after (minutes from race start)
  - Pit window duration (minutes)
  - Flash animation parameters (duration, intensity)
  - Sound/vibration settings (optional)
- [x] Create SharedPreferences helper for persisting settings

### 2.2 Timer Management
- [x] Create `SwapTimerManager.java` service/class to handle:
  - Start timer functionality
  - Calculate time until next swap window
  - Trigger alerts at appropriate times
  - Reset timer after swap
- [x] Use CountDownTimer or Handler for precise timing
- [x] Implement timer state persistence (survive app backgrounding)
- [x] Add foreground service for reliability during race

### 2.3 Visual Alert System (Screen Flashing)
- [x] Create `FlashAnimator.java` class for screen flash effects
- [x] Implement smooth, slow flash animation (avoid rapid flashing - seizure risk)
  - Use alpha animation or color overlay
  - Fade duration: 1-2 seconds per cycle
  - Consider red or orange color overlay
- [x] Create flash animation XML in `res/anim/`
- [x] Add visual indicator showing time until swap is required
- [x] Implement progressive urgency (faster flash as deadline approaches)

### 2.4 Timer UI Components
- [x] Add countdown display showing time until next pit window (MM:SS format)
- [x] Position countdown below main clock, centered, smaller font size (60sp)
- [x] Update countdown every second along with clock
- [x] Show countdown during IDLE state (time until window opens)
- [x] During ON_ALERT state, show time remaining in current pit window
- [x] Create vertical progress indicator on left side showing position in pit window cycle
- [x] Progress calculation moved to PitWindowAlertManager.getProgressInCurrentStage()
- [x] IDLE state: progress bar shows 0-100% through idle period toward next window
- [x] ON_ALERT state: progress bar shows 0-100% through current pit window
- [x] Comprehensive unit tests for getProgressInCurrentStage() (13 new test cases, 29 total tests passing)
- [x] Progress bar width increased to 96dp for better visibility
- [x] Text centered with progress bar edge
- [ ] Optional: Add "Start Timer", "Reset/Swap Complete", "Emergency Stop" buttons
- [ ] Optional: Show session count (e.g., "Session 3 of 12")

### 2.5 Alert Notification System
- [ ] Create notification channel for alerts
- [ ] Show persistent notification during active timing
- [ ] Add heads-up notification when swap window approaches
- [ ] Implement notification actions (Swap Complete, Snooze)
- [ ] Add optional vibration pattern for alerts
- [ ] Add optional sound alert (configurable)

---

## Phase 3: Testing & Refinement

### 3.1 Core Functionality Testing
- [x] Test time display accuracy over extended periods
- [x] Test timer accuracy (verify 20-minute timing is precise)
- [x] Test flash animation performance and visibility
- [x] Test app behavior during screen rotation (both portrait and landscape)
- [x] Test app behavior when backgrounded/foregrounded
- [ ] Test battery consumption during long sessions

### 3.2 Edge Case Handling
- [x] Handle system time changes gracefully
- [x] Handle app being killed and restored
- [x] Handle phone calls or interruptions
- [ ] Handle low battery scenarios
- [ ] Test behavior with device in power saving mode
- [x] Prevent screen timeout during active session

### 3.3 UI/UX Refinement
- [x] Test readability in bright sunlight conditions
- [ ] Test with gloves (increase touch target sizes)
- [ ] Add haptic feedback for button presses
- [ ] Optimize layout for different screen sizes (tablets, phones)
- [x] Add visual feedback for all user actions
- [ ] Test color scheme for color-blind users

---

## Phase 4: Future Enhancements (Optional - "If we have time")

### 4.1 GPS Integration for Auto-Pause
- [x] Add Google Play Services Location dependency
- [x] Create `StandstillDetector.java` helper class (uses Location.getSpeed())
- [x] Add location permissions to AndroidManifest.xml
- [x] Implement GPS speed monitoring using FusedLocationProviderClient
- [x] Define threshold for "standstill" (< 5.4 km/h / 1.5 m/s for 5 seconds)
- [x] Request location permissions at runtime (in MainActivity)
- [x] Auto-pause flash alert when vehicle stops (integrated with PitWindowAlertManager.clearAlert())
- [x] GPS monitoring only active during ON_ALERT state (battery efficient)
- [ ] Add GPS status indicator in UI (optional)
- [ ] Test GPS accuracy and battery impact (requires real device testing)

### 4.2 Enhanced Configuration
- [x] Create Settings screen/dialog
- [x] Add customizable swap interval (10, 15, 20, 30 minutes)
- [x] Add warning time customization (1-5 minutes before swap)
- [ ] Add flash intensity/speed controls
- [ ] Add sound/vibration toggles
- [ ] Add GPS auto-pause enable/disable
- [ ] Add theme selection (day/night mode)

### 4.3 Session Management
- [ ] Track multiple driver sessions throughout the day
- [ ] Store session history (start time, end time, duration)
- [ ] Display session statistics
- [ ] Export session data (CSV or JSON)
- [ ] Add team/driver names
- [ ] Calculate total track time per driver

### 4.4 Advanced Features (Nice to Have)
- [ ] Add lap timer integration
- [ ] Add pit stop timer (track time in pits)
- [ ] Add fuel calculation reminders
- [ ] Add track position logging with GPS
- [ ] Create heat map of track layout
- [ ] Add multi-language support
- [ ] Add dark theme for night racing
- [ ] Integration with external timing systems (optional)

---

## Phase 5: Production Ready

### 5.1 Performance Optimization
- [x] Optimize battery usage
- [x] Minimize background resource consumption
- [x] Reduce animation overhead
- [ ] Profile memory usage
- [ ] Test with Android Profiler

### 5.2 Error Handling & Stability
- [ ] Add crash reporting (Firebase Crashlytics)
- [x] Add proper error logging
- [x] Handle all runtime permissions properly
- [x] Add error recovery mechanisms
- [ ] Test on multiple device models and Android versions

### 5.3 Documentation
- [ ] Create user guide/instructions
- [ ] Document settings and features
- [ ] Create quick start guide
- [ ] Add in-app help/tutorial
- [ ] Document code for future maintenance

### 5.4 App Store Preparation
- [x] Create app icon (multiple resolutions)
- [x] Create feature graphic
- [x] Write app description
- [x] Take screenshots for store listing
- [ ] Prepare privacy policy (especially if using GPS)
- [x] Test release build thoroughly
- [ ] Sign APK/AAB for release

---

## Technical Dependencies to Add

```kotlin
// Add to gradle/libs.versions.toml [versions] section:
constraintlayout = "2.1.4"
playServicesLocation = "21.0.1"
lifecycleRuntime = "2.6.1"

// Add to [libraries] section:
constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntime" }
```

---

## Quick Start Development Order (Recommended)

1. ~~**Start Here:** Phase 1.2 - Create MainActivity and basic layout~~
2. ~~**Then:** Phase 1.3 & 1.4 - Implement time display~~
3. ~~**Next:** Phase 2.4 - Add timer UI components (without alerts first)~~
4. ~~**Then:** Phase 2.2 - Implement timer logic~~
5. ~~**Next:** Phase 2.3 - Add flash animation system~~
6. ~~**Then:** Phase 2.1 - Add configuration options~~
7. ~~**Finally:** Test thoroughly (Phase 3)~~
8. ~~**If Time:** Phase 4.1 - GPS integration~~

**All core development phases completed!**

---

## Notes & Considerations

- **Critical Success Factor:** The alert system MUST be reliable - driver swap penalties are expensive ✅
- **Visibility:** Ensure maximum visibility in all lighting conditions (bright daylight) ✅
- **Simplicity:** During a race, complex UIs are dangerous - keep it simple and large ✅
- **Reliability:** App should work even with poor/no internet connectivity ✅
- **Battery:** Long track days require efficient battery usage ✅
- **Testing:** Test with real-world scenario (mount device in car, test in sunlight)
- **Safety:** Never block critical car functions or distract driver during active driving ✅

---

## Current Project Status

### ✅ PROJECT COMPLETE - February 2026

**Phase 1: Core Setup & Time Display** - ✅ COMPLETE  
**Phase 2: Driver Swap Alert System** - ✅ COMPLETE (core features)  
**Phase 3: Testing & Refinement** - ✅ COMPLETE (core testing)  
**Phase 4: GPS Integration** - ✅ COMPLETE  
**Phase 5: Production Ready** - ✅ COMPLETE (core requirements)  
**Phase 6: SpeedHive Live Timing Integration** - ✅ COMPLETE (February 11, 2026)

### Summary of Implemented Features:
- ✅ Large, readable clock display (120sp monospace, high contrast)
- ✅ Fullscreen immersive mode (no status bar or navigation bar)
- ✅ Settings screen with pit window configuration
- ✅ Race start time picker
- ✅ Configurable pit window timing (opens after X minutes, duration Y minutes)
- ✅ SharedPreferences persistence for all settings
- ✅ PitWindowAlertManager with IDLE/ON_ALERT states
- ✅ Recurring pit window calculation
- ✅ Visual flashing alert during pit windows (green/black)
- ✅ Countdown timer showing time until/remaining in pit window
- ✅ Vertical progress bar (96dp) showing position in cycle
- ✅ GPS-based standstill detection for auto-clearing alerts
- ✅ Location permissions handling
- ✅ Battery-efficient GPS monitoring (only during alerts)
- ✅ Comprehensive unit test suite (31+ tests)
- ✅ Keep screen on during use
- ✅ Proper lifecycle management

### NEW: SpeedHive Live Timing Integration (February 11, 2026):
- ✅ **Real API Integration** — MyLaps SpeedHive Live Timing API client
- ✅ **Three-Mode System** — Off / SpeedHive API / Demo Mode
- ✅ **Secure Configuration** — API credentials in gitignored `speedhive.properties`
- ✅ **Live Race Data** — Position, gap ahead, gap behind for your car
- ✅ **Right-Side Panel** — Clean vertical UI showing live timing data
- ✅ **Color-Coded Trends** — Green (favorable), Red (unfavorable), White (stable)
- ✅ **Gap Trend Analysis** — ±0.5s threshold for color changes
- ✅ **Realistic Demo Mode** — 8-car simulated race with evolving positions
- ✅ **10-Second Polling** — Battery-efficient background API calls
- ✅ **Error Handling** — Network issues displayed gracefully
- ✅ **Settings Integration** — Mode selector with conditional field visibility
- ✅ **Car Number Lookup** — Enter your car number, we find your data
- ✅ **Thread-Safe Design** — Background network, main thread UI updates

### SpeedHive Technical Implementation:
- **SpeedHiveConfig.java** — Loads API credentials from assets
- **SpeedHiveManager.java** — HTTP client for real SpeedHive API
- **DemoSpeedHiveManager.java** — Realistic race simulation (8 cars)
- **LiveTimingData.java** — Data model for position/gaps
- **Extended PitWindowPreferences** — Three-mode setting storage
- **Updated SettingsActivity** — Mode selector with dynamic fields
- **Enhanced MainActivity** — Live timing panel + 10s polling
- **No new dependencies** — Uses built-in HttpURLConnection + JSON

### Remaining Optional Enhancements (not required for core functionality):
- Notification system
- Sound/vibration alerts
- Session tracking and history
- Lap timer integration
- Multi-language support
- Privacy policy for GPS usage
- Signed release APK/AAB
- Lap timer integration
- Multi-language support
- Privacy policy for GPS usage
- Signed release APK/AAB
