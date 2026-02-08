package at.semmal.pitstopper;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Calendar;

/**
 * Unit tests for PitWindowAlertManager.
 * Tests the scenario from TODO.md:
 * Race start: 09:00
 * Pit window opens after: 17 minutes
 * Pit window duration: 6 minutes
 * Expected windows: 09:17-09:23, 09:37-09:43, 09:57-10:03, etc.
 */
public class PitWindowAlertManagerTest {

    private PitWindowAlertManager alertManager;

    @Before
    public void setUp() {
        // Race start: 09:00, pit window opens after 17 min, duration 6 min
        alertManager = new PitWindowAlertManager(9, 0, 17, 6);
    }

    // Test IDLE state before race start
    @Test
    public void testIdleBeforeRaceStart() {
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(8, 59));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(8, 30));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(7, 0));
    }

    // Test IDLE state between race start and first pit window
    @Test
    public void testIdleBetweenRaceStartAndFirstWindow() {
        // 09:00 - race start
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 0));

        // 09:01 - 09:16 - before first pit window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 1));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 10));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 16));
    }

    // Test ON_ALERT during first pit window (09:17 - 09:23)
    @Test
    public void testOnAlertFirstPitWindow() {
        // 09:17 - start of first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 17));

        // 09:20 - middle of first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 20));

        // 09:22 - near end of first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 22));
    }

    // Test IDLE after first pit window closes
    @Test
    public void testIdleAfterFirstWindow() {
        // 09:23 - just after first window closes (17 + 6 = 23)
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 23));

        // 09:30 - between first and second window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 30));

        // 09:36 - just before second window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 36));
    }

    // Test ON_ALERT during second pit window (09:37 - 09:43)
    @Test
    public void testOnAlertSecondPitWindow() {
        // 09:37 - start of second window (17 + 6 + 14 = 37, or 17 + 20)
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 37));

        // 09:40 - middle of second window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 40));

        // 09:42 - near end of second window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 42));
    }

    // Test IDLE after second pit window
    @Test
    public void testIdleAfterSecondWindow() {
        // 09:43 - just after second window closes
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 43));

        // 09:50 - between second and third window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 50));

        // 09:56 - just before third window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(9, 56));
    }

    // Test ON_ALERT during third pit window (09:57 - 10:03)
    @Test
    public void testOnAlertThirdPitWindow() {
        // 09:57 - start of third window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 57));

        // 09:59 - middle of third window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(9, 59));

        // 10:00 - crosses hour boundary, still in window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(10, 0));

        // 10:02 - near end of third window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(10, 2));
    }

    // Test IDLE after third pit window
    @Test
    public void testIdleAfterThirdWindow() {
        // 10:03 - just after third window closes
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(10, 3));

        // 10:10 - between windows
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    alertManager.getAlertState(10, 10));
    }

    // Test ON_ALERT during fourth pit window (10:17 - 10:23)
    @Test
    public void testOnAlertFourthPitWindow() {
        // 10:17 - start of fourth window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(10, 17));

        // 10:20 - middle of fourth window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    alertManager.getAlertState(10, 20));
    }

    // Test isInPitWindow method directly
    @Test
    public void testIsInPitWindow() {
        // Before race
        assertFalse(alertManager.isInPitWindow(8, 59));

        // During first window
        assertTrue(alertManager.isInPitWindow(9, 17));
        assertTrue(alertManager.isInPitWindow(9, 22));

        // Between windows
        assertFalse(alertManager.isInPitWindow(9, 23));
        assertFalse(alertManager.isInPitWindow(9, 36));

        // During second window
        assertTrue(alertManager.isInPitWindow(9, 37));
        assertTrue(alertManager.isInPitWindow(9, 42));

        // During third window crossing hour
        assertTrue(alertManager.isInPitWindow(9, 57));
        assertTrue(alertManager.isInPitWindow(10, 2));
    }

    // Test next pit window calculation
    @Test
    public void testGetNextPitWindowStart() {
        Calendar next;

        // Before race start
        next = alertManager.getNextPitWindowStart(8, 30);
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(17, next.get(Calendar.MINUTE));

        // At race start
        next = alertManager.getNextPitWindowStart(9, 0);
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(17, next.get(Calendar.MINUTE));

        // Between race start and first window
        next = alertManager.getNextPitWindowStart(9, 10);
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(17, next.get(Calendar.MINUTE));

        // During first window - should get next window start
        next = alertManager.getNextPitWindowStart(9, 20);
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(37, next.get(Calendar.MINUTE));

        // Between first and second window
        next = alertManager.getNextPitWindowStart(9, 30);
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY));
        assertEquals(37, next.get(Calendar.MINUTE));
    }

    // Test current pit window end calculation
    @Test
    public void testGetCurrentPitWindowEnd() {
        Calendar end;

        // Not in window - should return null
        end = alertManager.getCurrentPitWindowEnd(9, 10);
        assertNull(end);

        // In first window
        end = alertManager.getCurrentPitWindowEnd(9, 17);
        assertEquals(9, end.get(Calendar.HOUR_OF_DAY));
        assertEquals(23, end.get(Calendar.MINUTE));

        end = alertManager.getCurrentPitWindowEnd(9, 20);
        assertEquals(9, end.get(Calendar.HOUR_OF_DAY));
        assertEquals(23, end.get(Calendar.MINUTE));

        // Not in window
        end = alertManager.getCurrentPitWindowEnd(9, 25);
        assertNull(end);

        // In second window
        end = alertManager.getCurrentPitWindowEnd(9, 40);
        assertEquals(9, end.get(Calendar.HOUR_OF_DAY));
        assertEquals(43, end.get(Calendar.MINUTE));
    }

    // Test with different configuration (race at 14:30, window after 20 min, duration 10 min)
    @Test
    public void testDifferentConfiguration() {
        PitWindowAlertManager customManager = new PitWindowAlertManager(14, 30, 20, 10);

        // Before first window (14:30 + 20 = 14:50)
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    customManager.getAlertState(14, 30));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    customManager.getAlertState(14, 49));

        // First window: 14:50 - 15:00
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    customManager.getAlertState(14, 50));
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    customManager.getAlertState(14, 55));
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    customManager.getAlertState(14, 59));

        // After first window, cycle = 20 + ceil(10/2) = 20 + 5 = 25 min
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                    customManager.getAlertState(15, 0));

        // Second window: 15:15 - 15:25 (14:50 + 25 min cycle)
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    customManager.getAlertState(15, 15));
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                    customManager.getAlertState(15, 24));
    }

    // Test edge case: window at exact minute boundary
    @Test
    public void testWindowBoundaries() {
        // Test exact start of window
        assertTrue(alertManager.isInPitWindow(9, 17));

        // Test one minute before window ends (still in window)
        assertTrue(alertManager.isInPitWindow(9, 22));

        // Test exact minute window ends (17 + 6 = 23, so 09:23 is out)
        assertFalse(alertManager.isInPitWindow(9, 23));
    }

    // Test race start time getter
    @Test
    public void testGetRaceStartTime() {
        Calendar raceStart = alertManager.getRaceStartTime();
        assertEquals(9, raceStart.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, raceStart.get(Calendar.MINUTE));
        assertEquals(0, raceStart.get(Calendar.SECOND));
    }
}


