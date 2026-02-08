# PitWindowAlertManager State Management

## Overview
The `PitWindowAlertManager` now maintains internal state to track alert status across pit windows. This enables GPS-based automatic alert clearing where the alert won't resume once the car has stopped in the pits.

## State Management Logic

### States
- **IDLE**: Not currently in a pit window, or alert has been cleared for current window
- **ON_ALERT**: Currently in a pit window and alert is active

### State Transitions

#### IDLE → ON_ALERT
Occurs when:
- Current time enters a pit window
- The pit window has not been suppressed by `clearAlert()`

#### ON_ALERT → IDLE
Occurs when:
- Current time exits the pit window naturally, OR
- `clearAlert()` method is called

### Window Suppression
When `clearAlert()` is called during a pit window:
1. The current window's index is stored in `suppressedWindowIndex`
2. The state immediately transitions to IDLE
3. Subsequent checks during the same pit window remain IDLE
4. When moving to the next pit window, suppression is cleared
5. The next pit window will trigger ON_ALERT normally

## API

### `getAlertState(int currentHour, int currentMinute)`
Returns the current alert state and updates internal state.
- **MUST** be called on every check to maintain proper state
- Returns: `AlertState.IDLE` or `AlertState.ON_ALERT`

### `clearAlert()`
Clears the current alert and suppresses it for the current pit window.
- Uses current system time
- Call this when GPS detects car has stopped in pits
- Safe to call multiple times
- Safe to call outside pit window (has no effect)

### `clearAlert(int currentHour, int currentMinute)`
Test-friendly overload that accepts specific time.
- Used in unit tests to avoid dependency on system time
- Same behavior as no-arg version

## Usage Example

```java
// In your activity or service
PitWindowAlertManager alertManager = new PitWindowAlertManager(9, 0, 17, 6);

// Regular check (every second)
Calendar now = Calendar.getInstance();
int hour = now.get(Calendar.HOUR_OF_DAY);
int minute = now.get(Calendar.MINUTE);

AlertState state = alertManager.getAlertState(hour, minute);

if (state == AlertState.ON_ALERT) {
    // Show flashing alert
    showAlert();
} else {
    // Hide alert
    hideAlert();
}

// When GPS detects car stopped
if (gpsDetectsStandstill()) {
    alertManager.clearAlert(); // Suppresses alert for current window
}
```

## Implementation Details

### Window Index Tracking
Each pit window is assigned an index:
- First window (e.g., 09:17-09:23): index 0
- Second window (e.g., 09:37-09:43): index 1
- Third window (e.g., 09:57-10:03): index 2
- etc.

The `suppressedWindowIndex` stores which specific window has been cleared. This allows:
- Clearing window 0 doesn't affect window 1
- Moving from window 0 to window 1 automatically clears suppression
- Same window can be cleared again in next cycle (though this would be hours later)

### Edge Cases Handled
- ✅ Calling `clearAlert()` outside a pit window has no effect
- ✅ Calling `clearAlert()` multiple times in same window is safe
- ✅ Clearing at window start, middle, or end all work correctly
- ✅ Hour boundary crossings (e.g., 09:57 → 10:03) handled properly
- ✅ State persists across multiple checks within same window
- ✅ Suppression automatically clears when moving to next window

## Testing
42 comprehensive unit tests cover all scenarios:
- Basic state transitions (IDLE ↔ ON_ALERT)
- Window suppression via `clearAlert()`
- Edge cases (hour boundaries, multiple calls, etc.)
- Window index transitions
- State persistence

All tests pass. See `PitWindowAlertManagerTest.java` for details.

## Future GPS Integration
This state management system is designed to support GPS-based auto-pause:

```java
// Pseudo-code for future GPS integration
LocationManager locationManager = ...;
float speed = location.getSpeed(); // m/s

if (speed < 1.4f) { // < 5 km/h
    standstillDuration++;
    if (standstillDuration > 5) { // 5 seconds
        alertManager.clearAlert(); // Auto-clear when stopped
    }
} else {
    standstillDuration = 0; // Reset counter when moving
}
```

The alert will not resume until the next pit window, giving the driver time to complete the swap without distraction.

