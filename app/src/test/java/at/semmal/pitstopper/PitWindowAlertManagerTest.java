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

    // ========== Tests for getProgressInCurrentStage() ==========

    // Test progress before race start
    @Test
    public void testProgressBeforeRaceStart() {
        int progress = alertManager.getProgressInCurrentStage(8, 30, 0);
        assertEquals(0, progress);
    }

    // Test progress at race start
    @Test
    public void testProgressAtRaceStart() {
        int progress = alertManager.getProgressInCurrentStage(9, 0, 0);
        assertEquals(0, progress);
    }

    // Test progress during first idle period (before first pit window)
    @Test
    public void testProgressDuringFirstIdlePeriod() {
        // At 09:00 (race start): 0%
        assertEquals(0, alertManager.getProgressInCurrentStage(9, 0, 0));

        // At 09:08 (8 min into 17 min idle)
        int progress = alertManager.getProgressInCurrentStage(9, 8, 30);
        assertTrue("Progress at 09:08:30 should be positive", progress > 0);
        assertTrue("Progress at 09:08:30 should be less than 100", progress < 100);

        // At 09:16 (16 min into 17 min idle)
        progress = alertManager.getProgressInCurrentStage(9, 16, 0);
        assertTrue("Progress at 09:16 should be high (>80%)", progress > 80);
        assertTrue("Progress at 09:16 should be less than 100", progress < 100);
    }

    // Test progress at start of first pit window
    @Test
    public void testProgressAtFirstWindowStart() {
        // At 09:17:00 - just entered pit window, should be 0%
        int progress = alertManager.getProgressInCurrentStage(9, 17, 0);
        assertEquals(0, progress);
    }

    // Test progress during first pit window
    @Test
    public void testProgressDuringFirstPitWindow() {
        // At 09:17 (start): 0%
        assertEquals(0, alertManager.getProgressInCurrentStage(9, 17, 0));

        // At 09:18 (1 min into 6 min window): ~16%
        int progress = alertManager.getProgressInCurrentStage(9, 18, 0);
        assertTrue("Progress should be around 16%", progress >= 16 && progress <= 17);

        // At 09:20 (3 min into 6 min window): 50%
        progress = alertManager.getProgressInCurrentStage(9, 20, 0);
        assertEquals(50, progress);

        // At 09:22 (5 min into 6 min window): ~83%
        progress = alertManager.getProgressInCurrentStage(9, 22, 0);
        assertTrue("Progress should be around 83%", progress >= 83 && progress <= 84);

        // At 09:22:59 (almost end of 6 min window): ~99%
        progress = alertManager.getProgressInCurrentStage(9, 22, 59);
        assertTrue("Progress should be around 99%", progress >= 99 && progress <= 100);
    }

    // Test progress at end of first pit window
    @Test
    public void testProgressAtFirstWindowEnd() {
        // At 09:23 - just exited pit window, back to IDLE, should be 0%
        int progress = alertManager.getProgressInCurrentStage(9, 23, 0);
        assertEquals(0, progress);
    }

    // Test progress during idle period between first and second windows
    @Test
    public void testProgressBetweenFirstAndSecondWindows() {
        // Cycle: 20 minutes, Window: 6 minutes, Idle: 14 minutes
        // First window ends at 09:23, second window starts at 09:37
        // Idle period: 09:23 - 09:37 (14 minutes)

        // At 09:23 (start of idle): 0%
        assertEquals(0, alertManager.getProgressInCurrentStage(9, 23, 0));

        // At 09:30 (7 min into 14 min idle): 50%
        int progress = alertManager.getProgressInCurrentStage(9, 30, 0);
        assertEquals(50, progress);

        // At 09:36 (13 min into 14 min idle): ~92%
        progress = alertManager.getProgressInCurrentStage(9, 36, 0);
        assertTrue("Progress should be around 92%", progress >= 92 && progress <= 93);
    }

    // Test progress during second pit window
    @Test
    public void testProgressDuringSecondPitWindow() {
        // Second window: 09:37 - 09:43

        // At 09:37 (start): 0%
        assertEquals(0, alertManager.getProgressInCurrentStage(9, 37, 0));

        // At 09:40 (3 min into 6 min window): should be around 50%
        int progress = alertManager.getProgressInCurrentStage(9, 40, 0);
        assertTrue("Progress at 09:40 should be around 50%", progress >= 48 && progress <= 52);

        // At 09:42 (5 min into 6 min window): should be high (70-90%)
        progress = alertManager.getProgressInCurrentStage(9, 42, 30);
        assertTrue("Progress at 09:42:30 should be high", progress >= 70 && progress <= 95);
    }

    // Test progress during third pit window (crosses hour boundary)
    @Test
    public void testProgressDuringThirdPitWindow() {
        // Third window: 09:57 - 10:03

        // At 09:57 (start): 0%
        assertEquals(0, alertManager.getProgressInCurrentStage(9, 57, 0));

        // At 10:00 (3 min into 6 min window): 50%
        int progress = alertManager.getProgressInCurrentStage(10, 0, 0);
        assertEquals(50, progress);

        // At 10:02 (5 min into 6 min window): ~83%
        progress = alertManager.getProgressInCurrentStage(10, 2, 0);
        assertTrue("Progress should be around 83%", progress >= 83 && progress <= 84);
    }

    // Test progress with seconds precision
    @Test
    public void testProgressWithSecondsPrecision() {
        // During pit window at 09:17:30 (30 seconds into window)
        int progress = alertManager.getProgressInCurrentStage(9, 17, 30);
        System.out.println("Progress at 09:17:30 = " + progress + "%");
        assertTrue("Progress at 09:17:30 should be small", progress >= 0 && progress <= 10);

        // During pit window at 09:19:59 (almost 3 minutes)
        progress = alertManager.getProgressInCurrentStage(9, 19, 59);
        System.out.println("Progress at 09:19:59 = " + progress + "%");
        assertTrue("Progress at 09:19:59 should be around 40-60%", progress >= 40 && progress <= 60);
    }

    // Test progress bounds (never exceeds 0-100)
    @Test
    public void testProgressBounds() {
        // Test various times to ensure progress is always 0-100
        for (int hour = 9; hour <= 11; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                int progress = alertManager.getProgressInCurrentStage(hour, minute, 0);
                assertTrue("Progress should be >= 0", progress >= 0);
                assertTrue("Progress should be <= 100", progress <= 100);
            }
        }
    }

    // Test progress with different configuration
    @Test
    public void testProgressWithDifferentConfiguration() {
        // Race at 14:30, window after 20 min, duration 10 min
        // Cycle: 20 + 5 = 25 minutes
        PitWindowAlertManager customManager = new PitWindowAlertManager(14, 30, 20, 10);

        // At race start: 0%
        assertEquals(0, customManager.getProgressInCurrentStage(14, 30, 0));

        // At 14:40 (10 min into 20 min idle): should be around 50%
        int progress = customManager.getProgressInCurrentStage(14, 40, 0);
        System.out.println("Progress at 14:40 = " + progress + "%");
        assertTrue("Progress at 14:40 should be positive", progress > 0 && progress <= 100);

        // At 14:50 (start of pit window): 0%
        assertEquals(0, customManager.getProgressInCurrentStage(14, 50, 0));

        // At 14:55 (5 min into 10 min window): should be around 50%
        progress = customManager.getProgressInCurrentStage(14, 55, 0);
        System.out.println("Progress at 14:55 = " + progress + "%");
        assertTrue("Progress at 14:55 should be around 40-60%", progress >= 40 && progress <= 60);

        // At 15:00 (end of window): back to 0%
        assertEquals(0, customManager.getProgressInCurrentStage(15, 0, 0));
    }

    // Test progress resets at window transitions
    @Test
    public void testProgressResetsAtTransitions() {
        // Progress should reset to 0 when entering pit window (IDLE -> ON_ALERT)
        int progressBefore = alertManager.getProgressInCurrentStage(9, 16, 59); // Just before window
        int progressAtStart = alertManager.getProgressInCurrentStage(9, 17, 0); // Window starts

        assertTrue("Progress before window should be high", progressBefore > 90);
        assertEquals("Progress at window start should be 0", 0, progressAtStart);

        // Progress should reset to 0 when exiting pit window (ON_ALERT -> IDLE)
        int progressInWindow = alertManager.getProgressInCurrentStage(9, 22, 59); // Just before window ends
        int progressAfterWindow = alertManager.getProgressInCurrentStage(9, 23, 0); // Window ends

        assertTrue("Progress in window should be high", progressInWindow > 90);
        assertEquals("Progress after window should be 0", 0, progressAfterWindow);
    }

    // ========== STATE MANAGEMENT TESTS ==========

    // Test state transitions from IDLE to ON_ALERT when entering pit window
    @Test
    public void testStateTransitionIdleToOnAlert() {
        // Before first window - should be IDLE
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 16));

        // Enter first window - should transition to ON_ALERT
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));

        // Stay in first window - should remain ON_ALERT
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 20));
    }

    // Test state transitions from ON_ALERT to IDLE when exiting pit window
    @Test
    public void testStateTransitionOnAlertToIdleNaturally() {
        // Enter first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 20));

        // Exit first window - should transition to IDLE
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 23));

        // Stay outside window - should remain IDLE
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 30));
    }

    // Test clearAlert() transitions state to IDLE
    @Test
    public void testClearAlertTransitionsToIdle() {
        // Enter first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 20));

        // Call clearAlert - should transition to IDLE
        alertManager.clearAlert(9, 20);

        // Next check should be IDLE (even though still in window time-wise)
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));
    }

    // Test clearAlert() suppresses alert for remainder of current pit window
    @Test
    public void testClearAlertSuppressesCurrentWindow() {
        // Enter first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));

        // Clear the alert
        alertManager.clearAlert(9, 17);

        // Should stay IDLE for remainder of first window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 18));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 22));
    }

    // Test alert resumes in next pit window after clearAlert()
    @Test
    public void testAlertResumesInNextWindow() {
        // Enter and clear first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));
        alertManager.clearAlert(9, 17);
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));

        // Exit first window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 23));

        // Enter second window - alert should resume
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 37));
    }

    // Test clearAlert() called outside pit window has no effect
    @Test
    public void testClearAlertOutsideWindowNoEffect() {
        // Before first window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 10));

        // Call clearAlert outside window
        alertManager.clearAlert(9, 10);

        // Should still enter ON_ALERT when window opens
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));
    }

    // Test clearAlert() only suppresses specific window, not all windows
    @Test
    public void testClearAlertOnlyAffectsCurrentWindow() {
        // Clear first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));
        alertManager.clearAlert(9, 17);

        // First window suppressed
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));

        // Second window should still alert
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 37));

        // Third window should still alert
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 57));
    }

    // Test multiple consecutive clearAlert() calls
    @Test
    public void testMultipleClearAlertCalls() {
        // Enter first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));

        // Call clearAlert multiple times
        alertManager.clearAlert(9, 17);
        alertManager.clearAlert(9, 17);
        alertManager.clearAlert(9, 17);

        // Should stay IDLE
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));
    }

    // Test clearAlert() at start of window
    @Test
    public void testClearAlertAtWindowStart() {
        // Enter first window at exact start time
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));

        // Clear immediately
        alertManager.clearAlert(9, 17);

        // Should suppress for entire window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 17));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 22));
    }

    // Test clearAlert() at end of window
    @Test
    public void testClearAlertAtWindowEnd() {
        // Enter first window near end
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 22));

        // Clear at end
        alertManager.clearAlert(9, 22);

        // Should be IDLE
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 22));

        // Naturally IDLE after window
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 23));
    }

    // Test state persistence across multiple checks
    @Test
    public void testStatePersistenceAcrossChecks() {
        // Multiple checks in same window should maintain ON_ALERT
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 18));
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 19));

        // Clear and check multiple times
        alertManager.clearAlert(9, 19);
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 19));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 21));
    }

    // Test clearAlert in different windows
    @Test
    public void testClearAlertInDifferentWindows() {
        // Clear first window
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));
        alertManager.clearAlert(9, 17);
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));

        // Second window should alert normally
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 37));

        // Clear second window
        alertManager.clearAlert(9, 37);
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 40));

        // Third window should alert normally
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 57));
    }

    // Test window index transitions
    @Test
    public void testWindowIndexTransitions() {
        // First window (index 0)
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 17));
        alertManager.clearAlert(9, 17);
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 20));

        // Move past first window to between windows
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(9, 25));

        // Second window (index 1) - suppression should be cleared
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 37));
    }

    // Test clearAlert() with hour boundary crossing
    @Test
    public void testClearAlertAcrossHourBoundary() {
        // Third window crosses hour boundary (09:57 - 10:03)
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(9, 57));

        // Clear alert
        alertManager.clearAlert(9, 57);

        // Should remain IDLE even after hour change
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(10, 0));
        assertEquals(PitWindowAlertManager.AlertState.IDLE,
                alertManager.getAlertState(10, 2));

        // Fourth window should alert normally
        assertEquals(PitWindowAlertManager.AlertState.ON_ALERT,
                alertManager.getAlertState(10, 17));
    }
}


