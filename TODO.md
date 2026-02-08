# Pitstopper - Trackday Application TODO List

## Project Overview
**Target Platform:** Android  
**Screen Orientation:** Landscape (optimized)  
**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 36  
**Compile SDK:** 36  
**Java Version:** 11  

---

## Phase 1: Core Setup & Time Display

### 1.1 Project Configuration
- [x] Project structure created with proper namespace (`at.semmal.pitstopper`)
- [ ] Add ConstraintLayout dependency for flexible landscape layouts
- [ ] Add permissions for future features (ACCESS_FINE_LOCATION for GPS)
- [ ] Enable View Binding or Data Binding in build.gradle.kts
- [ ] Keep screen on during app usage (add WAKE_LOCK permission)

### 1.2 Main Activity Setup
- [ ] Create `MainActivity.java` in `app/src/main/java/at/semmal/pitstopper/`
- [ ] Create `activity_main.xml` layout file (optimized for landscape)
- [ ] Create landscape-specific layout variant in `res/layout-land/` for enhanced UX
- [ ] Create portrait layout variant in `res/layout-port/` for basic support
- [ ] Configure MainActivity in AndroidManifest.xml with:
  - Launcher intent
  - Fullscreen/immersive mode flags
  - Keep screen on flag

### 1.3 Time Display UI
- [ ] Design large, easily readable time display for landscape view
- [ ] Create TextView for current time with large font size (60-80sp minimum)
- [ ] Use monospace font or digital-style font for better readability
- [ ] Add proper contrast colors for visibility in daylight conditions
- [ ] Position time display prominently in the UI (top-center recommended)

### 1.4 Time Display Logic
- [ ] Implement Handler/Runnable pattern for updating time every second
- [ ] Format time according to system locale (or 24h format for racing)
- [ ] Handle lifecycle events properly (pause/resume updates)
- [ ] Test time display accuracy and performance

---

## Phase 2: Driver Swap Alert System (Core Feature)

### 2.1 Alert Configuration Model
- [x] Create settings UI with configuration options
- [x] Add time picker for race start time
- [x] Add pit window opens after (minutes) configuration
- [x] Add pit window duration (minutes) configuration
- [ ] Create `PitWindowConfig.java` class to store:
  - Race start time (hour, minute)
  - Pit window opens after (minutes from race start)
  - Pit window duration (minutes)
  - Flash animation parameters (duration, intensity)
  - Sound/vibration settings (optional)
- [ ] Create SharedPreferences helper for persisting settings

### 2.2 Timer Management
- [ ] Create `SwapTimerManager.java` service/class to handle:
  - Start timer functionality
  - Calculate time until next swap window
  - Trigger alerts at appropriate times
  - Reset timer after swap
- [ ] Use CountDownTimer or Handler for precise timing
- [ ] Implement timer state persistence (survive app backgrounding)
- [ ] Add foreground service for reliability during race

### 2.3 Visual Alert System (Screen Flashing)
- [ ] Create `FlashAnimator.java` class for screen flash effects
- [ ] Implement smooth, slow flash animation (avoid rapid flashing - seizure risk)
  - Use alpha animation or color overlay
  - Fade duration: 1-2 seconds per cycle
  - Consider red or orange color overlay
- [ ] Create flash animation XML in `res/anim/`
- [ ] Add visual indicator showing time until swap is required
- [ ] Implement progressive urgency (faster flash as deadline approaches)

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
- [ ] Test time display accuracy over extended periods
- [ ] Test timer accuracy (verify 20-minute timing is precise)
- [ ] Test flash animation performance and visibility
- [ ] Test app behavior during screen rotation (both portrait and landscape)
- [ ] Test app behavior when backgrounded/foregrounded
- [ ] Test battery consumption during long sessions

### 3.2 Edge Case Handling
- [ ] Handle system time changes gracefully
- [ ] Handle app being killed and restored
- [ ] Handle phone calls or interruptions
- [ ] Handle low battery scenarios
- [ ] Test behavior with device in power saving mode
- [ ] Prevent screen timeout during active session

### 3.3 UI/UX Refinement
- [ ] Test readability in bright sunlight conditions
- [ ] Test with gloves (increase touch target sizes)
- [ ] Add haptic feedback for button presses
- [ ] Optimize layout for different screen sizes (tablets, phones)
- [ ] Add visual feedback for all user actions
- [ ] Test color scheme for color-blind users

---

## Phase 4: Future Enhancements (Optional - "If we have time")

### 4.1 GPS Integration for Auto-Pause
- [ ] Add Google Play Services Location dependency
- [ ] Create `LocationManager.java` helper class
- [ ] Request location permissions at runtime
- [ ] Implement GPS speed monitoring
- [ ] Define threshold for "standstill" (e.g., < 5 km/h for 10 seconds)
- [ ] Auto-pause flash alert when vehicle stops
- [ ] Resume alert if vehicle doesn't remain stopped
- [ ] Add GPS status indicator in UI
- [ ] Test GPS accuracy and battery impact

### 4.2 Enhanced Configuration
- [ ] Create Settings screen/dialog
- [ ] Add customizable swap interval (10, 15, 20, 30 minutes)
- [ ] Add warning time customization (1-5 minutes before swap)
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
- [ ] Optimize battery usage
- [ ] Minimize background resource consumption
- [ ] Reduce animation overhead
- [ ] Profile memory usage
- [ ] Test with Android Profiler

### 5.2 Error Handling & Stability
- [ ] Add crash reporting (Firebase Crashlytics)
- [ ] Add proper error logging
- [ ] Handle all runtime permissions properly
- [ ] Add error recovery mechanisms
- [ ] Test on multiple device models and Android versions

### 5.3 Documentation
- [ ] Create user guide/instructions
- [ ] Document settings and features
- [ ] Create quick start guide
- [ ] Add in-app help/tutorial
- [ ] Document code for future maintenance

### 5.4 App Store Preparation
- [ ] Create app icon (multiple resolutions)
- [ ] Create feature graphic
- [ ] Write app description
- [ ] Take screenshots for store listing
- [ ] Prepare privacy policy (especially if using GPS)
- [ ] Test release build thoroughly
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

1. **Start Here:** Phase 1.2 - Create MainActivity and basic layout
2. **Then:** Phase 1.3 & 1.4 - Implement time display
3. **Next:** Phase 2.4 - Add timer UI components (without alerts first)
4. **Then:** Phase 2.2 - Implement timer logic
5. **Next:** Phase 2.3 - Add flash animation system
6. **Then:** Phase 2.1 - Add configuration options
7. **Finally:** Test thoroughly (Phase 3)
8. **If Time:** Phase 4.1 - GPS integration

---

## Notes & Considerations

- **Critical Success Factor:** The alert system MUST be reliable - driver swap penalties are expensive
- **Visibility:** Ensure maximum visibility in all lighting conditions (bright daylight)
- **Simplicity:** During a race, complex UIs are dangerous - keep it simple and large
- **Reliability:** App should work even with poor/no internet connectivity
- **Battery:** Long track days require efficient battery usage
- **Testing:** Test with real-world scenario (mount device in car, test in sunlight)
- **Safety:** Never block critical car functions or distract driver during active driving

---

## Current Project Status
✅ **Phase 1 Complete: Core Setup & Time Display**  
✅ Project initialized with proper Android configuration  
✅ Namespace configured: `at.semmal.pitstopper`  
✅ Min SDK 24 (Android 7.0+) - supports 94%+ of devices  
✅ Target SDK 36 (latest)  
✅ MainActivity created with centered clock UI (120sp monospace)  
✅ ConstraintLayout dependency added  
✅ Keep screen on functionality implemented  
✅ High contrast colors for daylight visibility (black/white)  
✅ Fullscreen immersive mode enabled (no action bar, hidden system bars)  
✅ Clock updates every second with proper lifecycle management  
✅ Clock stops updating when app is paused (battery efficient)  
✅ Clock resumes updating when app is visible  
✅ Settings button (cog icon - 64dp) in top right  
✅ SettingsActivity created with pit window configuration UI  
✅ Time picker for race start time (e.g., 09:00)  
✅ Pit window opens after X minutes configuration (e.g., 17 minutes)  
✅ Pit window duration configuration (e.g., 6 minutes)  
✅ Input validation implemented  
✅ SharedPreferences fully integrated - settings persist across app restarts  
✅ PitWindowPreferences helper class created  
✅ Settings load defaults on first launch (09:00, 17min, 6min)  
✅ Settings persist even if app is force closed  
✅ **PitWindowAlertManager implemented with two states: IDLE and ON_ALERT**  
✅ Alert manager calculates recurring pit windows correctly  
✅ Example: Race at 09:00, opens after 17min, duration 6min → windows at 09:17-09:23, 09:37-09:43, 09:57-10:03  
✅ Window cycle calculation: opensAfter + ceil(duration/2) = 17 + 3 = 20 minutes  
✅ Comprehensive unit tests (16 tests, all passing)  
✅ Tests cover: before race, between windows, during windows, hour boundaries, edge cases  
✅ Helper methods: getNextPitWindowStart(), getCurrentPitWindowEnd(), getRaceStartTime()  
✅ **Visual flashing alert integrated into MainActivity**  
✅ Screen flashes green/black every 2 seconds during pit window (ON_ALERT)  
✅ Flash pattern: seconds % 4 < 2 = black, else = darker green (#00AA00)  
✅ Alert manager reloaded on resume (picks up settings changes)  
✅ **Countdown timer added below main clock (60sp, MM:SS format)**  
✅ IDLE: Shows time until next pit window opens  
✅ ON_ALERT: Shows time remaining in current pit window  
✅ Updates every second with precise millisecond calculation  
✅ **Vertical progress bar on left side (24dp wide, full height)**  
✅ Shows position in current pit window cycle (0-100%)  
✅ Green progress indicator (#00AA00)  
✅ Updates every second with precise calculation  
✅ Fills from bottom to top (rotation 180°)  
✅ App builds successfully  
✅ All tests passing  
⏳ **Phase 2 Core Functionality Complete!** Optional enhancements remaining.


