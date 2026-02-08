package at.semmal.pitstopper;

import java.util.Calendar;

/**
 * Manages alert states based on pit window timing.
 * Calculates recurring pit windows and determines if current time is within a window.
 *
 * Pattern: First window opens after pitWindowOpensAfterMinutes from race start,
 * then windows repeat at an interval calculated to ensure proper spacing.
 * For example: Race at 09:00, opens after 17 min, duration 6 min
 * gives windows at 09:17-09:23, 09:37-09:43, 09:57-10:03 (20-minute cycle).
 */
public class PitWindowAlertManager {

    public enum AlertState {
        IDLE,
        ON_ALERT
    }

    private final int raceStartHour;
    private final int raceStartMinute;
    private final int pitWindowOpensAfterMinutes;
    private final int pitWindowDurationMinutes;
    private final int windowRepeatCycleMinutes;

    /**
     * Creates a new PitWindowAlertManager.
     *
     * @param raceStartHour Hour when race starts (0-23)
     * @param raceStartMinute Minute when race starts (0-59)
     * @param pitWindowOpensAfterMinutes Minutes after race start when first pit window opens
     * @param pitWindowDurationMinutes Duration of each pit window in minutes
     */
    public PitWindowAlertManager(int raceStartHour, int raceStartMinute,
                                   int pitWindowOpensAfterMinutes, int pitWindowDurationMinutes) {
        this.raceStartHour = raceStartHour;
        this.raceStartMinute = raceStartMinute;
        this.pitWindowOpensAfterMinutes = pitWindowOpensAfterMinutes;
        this.pitWindowDurationMinutes = pitWindowDurationMinutes;
        // Windows repeat at an interval: opens after + half duration (rounded)
        // This gives 17 + 3 = 20 for the example (17, 6)
        this.windowRepeatCycleMinutes = pitWindowOpensAfterMinutes + (pitWindowDurationMinutes + 1) / 2;
    }

    /**
     * Determines the current alert state based on the given time.
     *
     * @param currentHour Current hour (0-23)
     * @param currentMinute Current minute (0-59)
     * @return AlertState.ON_ALERT if within a pit window, AlertState.IDLE otherwise
     */
    public AlertState getAlertState(int currentHour, int currentMinute) {
        if (isInPitWindow(currentHour, currentMinute)) {
            return AlertState.ON_ALERT;
        }
        return AlertState.IDLE;
    }

    /**
     * Checks if the given time is within any pit window.
     *
     * @param currentHour Current hour (0-23)
     * @param currentMinute Current minute (0-59)
     * @return true if within a pit window, false otherwise
     */
    public boolean isInPitWindow(int currentHour, int currentMinute) {
        int raceStartMinutes = raceStartHour * 60 + raceStartMinute;
        int currentMinutes = currentHour * 60 + currentMinute;

        // Calculate minutes since race start
        int minutesSinceRaceStart = currentMinutes - raceStartMinutes;

        // Handle negative values (current time before race start)
        if (minutesSinceRaceStart < 0) {
            return false;
        }

        // Before first pit window opens
        if (minutesSinceRaceStart < pitWindowOpensAfterMinutes) {
            return false;
        }

        // Calculate minutes since first pit window started
        int minutesSinceFirstWindow = minutesSinceRaceStart - pitWindowOpensAfterMinutes;

        // Windows repeat every windowRepeatCycleMinutes
        int positionInCycle = minutesSinceFirstWindow % windowRepeatCycleMinutes;

        // We're in a window if position is less than duration
        return positionInCycle < pitWindowDurationMinutes;
    }

    /**
     * Gets the next pit window start time.
     *
     * @param currentHour Current hour (0-23)
     * @param currentMinute Current minute (0-59)
     * @return Calendar instance representing the next pit window start
     */
    public Calendar getNextPitWindowStart(int currentHour, int currentMinute) {
        Calendar result = Calendar.getInstance();
        result.set(Calendar.HOUR_OF_DAY, raceStartHour);
        result.set(Calendar.MINUTE, raceStartMinute);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);

        int raceStartMinutes = raceStartHour * 60 + raceStartMinute;
        int currentMinutes = currentHour * 60 + currentMinute;
        int minutesSinceRaceStart = currentMinutes - raceStartMinutes;

        if (minutesSinceRaceStart < pitWindowOpensAfterMinutes) {
            // Before first window, return first window start
            result.add(Calendar.MINUTE, pitWindowOpensAfterMinutes);
            return result;
        }

        // Calculate minutes since first window started
        int minutesSinceFirstWindow = minutesSinceRaceStart - pitWindowOpensAfterMinutes;
        int positionInCycle = minutesSinceFirstWindow % windowRepeatCycleMinutes;

        // Calculate minutes to next window start
        int minutesToNextWindow = windowRepeatCycleMinutes - positionInCycle;
        result.add(Calendar.MINUTE, minutesSinceRaceStart + minutesToNextWindow);

        return result;
    }

    /**
     * Gets the current pit window end time (if currently in a window).
     *
     * @param currentHour Current hour (0-23)
     * @param currentMinute Current minute (0-59)
     * @return Calendar instance representing current pit window end, or null if not in window
     */
    public Calendar getCurrentPitWindowEnd(int currentHour, int currentMinute) {
        if (!isInPitWindow(currentHour, currentMinute)) {
            return null;
        }

        Calendar result = Calendar.getInstance();
        result.set(Calendar.HOUR_OF_DAY, raceStartHour);
        result.set(Calendar.MINUTE, raceStartMinute);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);

        int raceStartMinutes = raceStartHour * 60 + raceStartMinute;
        int currentMinutes = currentHour * 60 + currentMinute;
        int minutesSinceRaceStart = currentMinutes - raceStartMinutes;

        int minutesSinceFirstWindow = minutesSinceRaceStart - pitWindowOpensAfterMinutes;
        int positionInCycle = minutesSinceFirstWindow % windowRepeatCycleMinutes;

        // Calculate minutes until end of current window
        int minutesUntilEnd = pitWindowDurationMinutes - positionInCycle;

        result.add(Calendar.MINUTE, minutesSinceRaceStart + minutesUntilEnd);

        return result;
    }

    /**
     * Gets the race start time.
     *
     * @return Calendar instance representing race start time
     */
    public Calendar getRaceStartTime() {
        Calendar result = Calendar.getInstance();
        result.set(Calendar.HOUR_OF_DAY, raceStartHour);
        result.set(Calendar.MINUTE, raceStartMinute);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }
}
