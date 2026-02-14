package at.semmal.pitstopper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PitStopper";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView textCurrentTime;
    private TextView textCountdown;
    private TextView textEventSession; // NEW: Event/Session display
    private ImageButton buttonSettings;
    private ConstraintLayout rootLayout;
    private View progressBar;
    private FrameLayout progressBarContainer;
    private Handler handler;
    private Runnable updateTimeRunnable;
    private SimpleDateFormat timeFormat;
    
    // SpeedHive Live Timing UI
    private LinearLayout liveTimingPanel;
    private TextView textPosition;
    private TextView textGapAhead;
    private TextView textGapBehind;

    private PitWindowPreferences preferences;
    private PitWindowAlertManager alertManager;
    private StandstillDetector standstillDetector;
    private boolean wasInAlertState = false;
    
    // SpeedHive Live Timing
    private SpeedHiveManager speedHiveManager;
    private Runnable speedHivePollingRunnable;
    private Runnable sessionCheckRunnable; // NEW: Session change detection
    private LiveTimingData previousTimingData; // For gap trend comparison
    private static final int SPEEDHIVE_POLL_INTERVAL_MS = 10000; // 10 seconds
    private static final int SESSION_CHECK_INTERVAL_MS = 60000; // 60 seconds
    
    // Session tracking for AUTO mode
    private String currentSessionId = ""; // Currently active session
    private String currentSessionName = ""; // Currently active session name
    private String currentEventName = ""; // Currently active event name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge immersive mode to hide status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars());
            insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_main);

        // Initialize views
        textCurrentTime = findViewById(R.id.textCurrentTime);
        textCountdown = findViewById(R.id.textCountdown);
        textEventSession = findViewById(R.id.textEventSession); // NEW: Event/Session display
        buttonSettings = findViewById(R.id.buttonSettings);
        rootLayout = findViewById(R.id.rootLayout);
        progressBar = findViewById(R.id.progressBar);
        progressBarContainer = findViewById(R.id.progressBarContainer);
        
        // Initialize SpeedHive UI elements
        liveTimingPanel = findViewById(R.id.liveTimingPanel);
        textPosition = findViewById(R.id.textPosition);
        textGapAhead = findViewById(R.id.textGapAhead);
        textGapBehind = findViewById(R.id.textGapBehind);

        // Initialize preferences
        preferences = new PitWindowPreferences(this);

        // Initialize time format (24-hour format for racing)
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Initialize handler for time updates
        handler = new Handler(Looper.getMainLooper());

        // Create runnable for updating time
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                // Schedule next update in 1 second
                handler.postDelayed(this, 1000);
            }
        };
        
        // Create runnable for SpeedHive polling
        speedHivePollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollSpeedHive();
                // Schedule next poll in 10 seconds
                handler.postDelayed(this, SPEEDHIVE_POLL_INTERVAL_MS);
            }
        };
        
        // Create runnable for session change detection (AUTO mode only)
        sessionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkForSessionChange();
                // Schedule next session check in 60 seconds
                handler.postDelayed(this, SESSION_CHECK_INTERVAL_MS);
            }
        };

        // Set up settings button click listener
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Initialize StandstillDetector for GPS-based pit stop detection
        standstillDetector = new StandstillDetector(this, new StandstillDetector.StandstillListener() {
            @Override
            public void onStandstillDetected() {
                Log.i(TAG, "Standstill detected - car stopped in pits, clearing alert");
                runOnUiThread(() -> {
                    alertManager.clearAlert();
                    Toast.makeText(MainActivity.this, "Pit stop detected - alert cleared", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onMovementDetected() {
                Log.i(TAG, "Movement detected - car is moving again");
                // Note: Alert won't resume automatically (it's cleared for this window)
            }
        });

        // Request location permissions
        checkAndRequestLocationPermission();

        // Enable fullscreen immersive mode
        hideSystemUI();
        
        // Auto-start MQTT server if enabled
        initializeMqttServer();
    }
    
    private void initializeMqttServer() {
        if (preferences.isMqttServerEnabled()) {
            int port = preferences.getMqttServerPort();
            Log.i(TAG, "Auto-starting MQTT server on port " + port);
            MqttServerService.startMqttServer(this, port);
        }
    }

    /**
     * Check for location permission and request if not granted.
     */
    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.i(TAG, "Location permission already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Location permission granted");
            } else {
                Log.w(TAG, "Location permission denied - GPS auto-clear will not work");
                Toast.makeText(this, "Location permission denied - GPS auto-pause disabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload settings and recreate alert manager (settings might have changed)
        alertManager = new PitWindowAlertManager(
                preferences.getRaceStartHour(),
                preferences.getRaceStartMinute(),
                preferences.getPitWindowOpens(),
                preferences.getPitWindowDuration()
        );

        // Reset alert state tracking
        wasInAlertState = false;
        
        // Initialize SpeedHive based on settings
        initializeSpeedHive();

        // Start updating the clock when activity becomes visible
        updateTime(); // Update immediately
        handler.postDelayed(updateTimeRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updating the clock when activity is no longer visible
        handler.removeCallbacks(updateTimeRunnable);
        
        // Stop SpeedHive polling and session checking
        handler.removeCallbacks(speedHivePollingRunnable);
        handler.removeCallbacks(sessionCheckRunnable);

        // Stop GPS monitoring to save battery
        if (standstillDetector != null) {
            standstillDetector.stopMonitoring();
        }
        
        // Clean up SpeedHive manager
        if (speedHiveManager != null) {
            speedHiveManager.shutdown();
            speedHiveManager = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void updateTime() {
        // Get current time
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentSecond = now.get(Calendar.SECOND);

        // Update time display
        String currentTime = timeFormat.format(now.getTime());
        textCurrentTime.setText(currentTime);

        // Check alert state
        PitWindowAlertManager.AlertState alertState = alertManager.getAlertState(currentHour, currentMinute);

        // Start/stop GPS monitoring based on alert state
        handleGpsMonitoring(alertState);

        // Update progress bar using alert manager
        int stageProgress = alertManager.getProgressInCurrentStage(currentHour, currentMinute, currentSecond);
        updateProgressBar(stageProgress);

        // Log progress value for debugging
        Log.d(TAG, String.format(Locale.getDefault(), "Progress: %d%% | State: %s | Time: %02d:%02d:%02d",
                stageProgress, alertState.name(), currentHour, currentMinute, currentSecond));

        if (alertState == PitWindowAlertManager.AlertState.ON_ALERT) {
            // Flash effect: if seconds % 4 < 2, show green; otherwise show black
            if (currentSecond % 4 < 2) {
                rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert_green));
            } else {
                rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background_primary));
            }

            // Show time remaining in current pit window
            Calendar windowEnd = alertManager.getCurrentPitWindowEnd(currentHour, currentMinute);
            if (windowEnd != null) {
                long remainingMillis = windowEnd.getTimeInMillis() - now.getTimeInMillis();
                int remainingMinutes = (int) (remainingMillis / 60000);
                int remainingSeconds = (int) ((remainingMillis % 60000) / 1000);
                textCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", remainingMinutes, remainingSeconds));
            }
        } else {
            // IDLE state - always black background
            rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background_primary));

            // Show time until next pit window
            Calendar nextWindow = alertManager.getNextPitWindowStart(currentHour, currentMinute);
            if (nextWindow != null) {
                long untilMillis = nextWindow.getTimeInMillis() - now.getTimeInMillis();
                int untilMinutes = (int) (untilMillis / 60000);
                int untilSeconds = (int) ((untilMillis % 60000) / 1000);
                textCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", untilMinutes, untilSeconds));
            }
        }
    }

    /**
     * Manages GPS monitoring based on alert state.
     * Starts monitoring when entering ON_ALERT state, stops when exiting.
     *
     * @param alertState Current alert state
     */
    private void handleGpsMonitoring(PitWindowAlertManager.AlertState alertState) {
        boolean isInAlertState = (alertState == PitWindowAlertManager.AlertState.ON_ALERT);

        if (isInAlertState && !wasInAlertState) {
            // Just entered ON_ALERT state - start GPS monitoring
            Log.i(TAG, "Entering pit window - starting GPS monitoring for standstill detection");
            if (standstillDetector != null) {
                boolean started = standstillDetector.startMonitoring();
                if (!started) {
                    Log.w(TAG, "Failed to start GPS monitoring - permission may not be granted");
                }
            }
        } else if (!isInAlertState && wasInAlertState) {
            // Just exited ON_ALERT state - stop GPS monitoring
            Log.i(TAG, "Exiting pit window - stopping GPS monitoring");
            if (standstillDetector != null) {
                standstillDetector.stopMonitoring();
            }
        }

        wasInAlertState = isInAlertState;
    }

    /**
     * Updates the progress bar height based on the progress percentage.
     *
     * @param progress Progress value from 0 to 100
     */
    private void updateProgressBar(int progress) {
        int containerHeight = progressBarContainer.getHeight();
        if (containerHeight > 0) {
            int progressHeight = (containerHeight * progress) / 100;
            ViewGroup.LayoutParams params = progressBar.getLayoutParams();
            params.height = progressHeight;
            progressBar.setLayoutParams(params);
        }
    }
    
    /**
     * Initialize SpeedHive manager and UI based on current preferences.
     */
    private void initializeSpeedHive() {
        String mode = preferences.getSpeedHiveMode();
        
        if (PitWindowPreferences.SPEEDHIVE_MODE_OFF.equals(mode)) {
            // Hide live timing panel and event/session display
            liveTimingPanel.setVisibility(View.GONE);
            textEventSession.setVisibility(View.GONE);
            speedHiveManager = null;
            previousTimingData = null;
        } else {
            // Show live timing panel
            liveTimingPanel.setVisibility(View.VISIBLE);
            
            if (PitWindowPreferences.SPEEDHIVE_MODE_SPEEDHIVE.equals(mode)) {
                // Create real SpeedHive manager
                try {
                    speedHiveManager = new SpeedHiveManager(this);
                    Log.i(TAG, "SpeedHive API manager initialized");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize SpeedHive manager", e);
                    Toast.makeText(this, "SpeedHive config error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    liveTimingPanel.setVisibility(View.GONE);
                    textEventSession.setVisibility(View.GONE);
                    return;
                }
            } else if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(mode)) {
                // Create demo SpeedHive manager
                speedHiveManager = new DemoSpeedHiveManager(this);
                Log.i(TAG, "Demo SpeedHive manager initialized");
            }
            
            // Start polling if we have a manager
            if (speedHiveManager != null) {
                // Reset previous data and session tracking
                previousTimingData = null;
                currentSessionId = "";
                currentSessionName = "";
                
                // Initialize event/session display
                initializeEventSessionDisplay();
                
                // Start polling immediately, then every 10 seconds
                pollSpeedHive();
                handler.postDelayed(speedHivePollingRunnable, SPEEDHIVE_POLL_INTERVAL_MS);
                
                // Start session change checking (AUTO mode only) after 60 seconds
                handler.postDelayed(sessionCheckRunnable, SESSION_CHECK_INTERVAL_MS);
            }
        }
    }
    
    /**
     * Poll SpeedHive API for updated leaderboard data.
     */
    private void pollSpeedHive() {
        if (speedHiveManager == null) {
            return;
        }
        
        String eventId = preferences.getSpeedHiveEventId();
        String sessionId = preferences.getSpeedHiveSessionId();
        String carNumber = preferences.getSpeedHiveCarNumber();
        String mode = preferences.getSpeedHiveMode();
        
        // For demo mode, we only need car number
        if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(mode)) {
            if (carNumber.isEmpty()) {
                Log.w(TAG, "Demo mode: car number not set - skipping poll");
                updateLiveTimingUI(null, "Car number not set");
                return;
            }
            
            Log.d(TAG, "Demo mode - polling for car #" + carNumber);
            speedHiveManager.fetchLeaderboard("", "", carNumber, new SpeedHiveManager.LiveTimingCallback() {
                @Override
                public void onSuccess(LiveTimingData data) {
                    runOnUiThread(() -> updateLiveTimingUI(data, null));
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> updateLiveTimingUI(null, error));
                }
            });
            return;
        }
        
        // For SpeedHive live mode, we need eventId and carNumber
        if (eventId.isEmpty() || carNumber.isEmpty()) {
            Log.w(TAG, "SpeedHive settings incomplete - skipping poll");
            updateLiveTimingUI(null, "Settings incomplete");
            return;
        }
        
        // Handle AUTO session detection
        if (PitWindowPreferences.SPEEDHIVE_SESSION_AUTO.equals(sessionId) || sessionId.isEmpty()) {
            Log.d(TAG, "AUTO session mode - detecting session for car #" + carNumber);
            autoDetectAndPoll(eventId, carNumber);
        } else {
            // Manual session selection
            Log.d(TAG, "Manual session mode - polling session " + sessionId + " for car #" + carNumber);
            pollWithSession(eventId, sessionId, carNumber);
        }
    }
    
    /**
     * Auto-detect the appropriate session and then poll for data.
     */
    private void autoDetectAndPoll(String eventId, String carNumber) {
        if (speedHiveManager instanceof SpeedHiveManager) {
            SpeedHiveManager realManager = (SpeedHiveManager) speedHiveManager;
            realManager.findSessionWithCar(eventId, carNumber, new SpeedHiveManager.AutoSessionCallback() {
                @Override
                public void onSuccess(String detectedSessionId, String sessionName) {
                    Log.i(TAG, "Auto-detected session: " + sessionName + " (" + detectedSessionId + ")");
                    runOnUiThread(() -> {
                        // Update session tracking
                        currentSessionId = detectedSessionId;
                        currentSessionName = sessionName;
                        updateEventSessionDisplay();
                        
                        // Poll the detected session
                        pollWithSession(eventId, detectedSessionId, carNumber);
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Auto-detection failed: " + error);
                    runOnUiThread(() -> updateLiveTimingUI(null, "Auto-detect failed: " + error));
                }
            });
        } else {
            // Demo mode doesn't need session detection
            currentSessionId = "DEMO";
            currentSessionName = "Practice Session";
            updateEventSessionDisplay();
            pollWithSession(eventId, "DEMO", carNumber);
        }
    }
    
    /**
     * Poll SpeedHive with a specific session ID.
     */
    private void pollWithSession(String eventId, String sessionId, String carNumber) {
        speedHiveManager.fetchLeaderboard(eventId, sessionId, carNumber, new SpeedHiveManager.LiveTimingCallback() {
            @Override
            public void onSuccess(LiveTimingData data) {
                Log.i(TAG, "SpeedHive data received: " + data.toString());
                runOnUiThread(() -> updateLiveTimingUI(data, null));
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "SpeedHive error: " + error);
                runOnUiThread(() -> updateLiveTimingUI(null, error));
            }
        });
    }
    
    /**
     * Update the live timing UI with new data or error message.
     * 
     * @param data New timing data (null if error)
     * @param error Error message (null if success)
     */
    private void updateLiveTimingUI(LiveTimingData data, String error) {
        if (data != null) {
            // Update position with total count (e.g., "P3/8") - make total part smaller
            String positionText = data.getFormattedPositionWithTotal();
            int slashIndex = positionText.indexOf('/');
            
            if (slashIndex != -1) {
                // Create spannable string with smaller text for total part
                SpannableString spannablePosition = new SpannableString(positionText);
                // Make the "/8" part 60% of normal size
                spannablePosition.setSpan(new RelativeSizeSpan(0.6f), slashIndex, positionText.length(), 0);
                textPosition.setText(spannablePosition);
            } else {
                // Fallback to regular text if no slash found
                textPosition.setText(positionText);
            }
            
            // Update gaps with color coding
            updateGapWithTrend(textGapAhead, data.getGapAhead(), 
                              previousTimingData != null ? previousTimingData.getGapAhead() : null, true);
            updateGapWithTrend(textGapBehind, data.getGapBehind(),
                              previousTimingData != null ? previousTimingData.getGapBehind() : null, false);
            
            // Store for next comparison
            previousTimingData = data;
            
        } else {
            // Show error state
            textPosition.setText("ERR");
            textGapAhead.setText(error != null ? "ERROR" : "---");
            textGapAhead.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            textGapBehind.setText("---");
            textGapBehind.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }
    
    /**
     * Update a gap text view with color coding based on trend.
     * 
     * @param textView TextView to update
     * @param currentGap Current gap value
     * @param previousGap Previous gap value (for comparison)
     * @param isGapAhead true if this is gap ahead, false if gap behind
     */
    private void updateGapWithTrend(TextView textView, String currentGap, String previousGap, boolean isGapAhead) {
        // Format gap for display (1 decimal place)
        String displayGap = formatGapForDisplay(currentGap);
        textView.setText(displayGap);
        
        if (previousGap == null || currentGap.equals(previousGap)) {
            // No previous data or no change - use white
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            return;
        }
        
        // Try to parse gaps as seconds for comparison
        Double currentSeconds = parseGapToSeconds(currentGap);
        Double previousSeconds = parseGapToSeconds(previousGap);
        
        if (currentSeconds == null || previousSeconds == null) {
            // Can't compare (probably "LEAD", "LAST", or "X Laps") - use white
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            return;
        }
        
        double delta = currentSeconds - previousSeconds;
        
        // Apply 0.5 second threshold
        if (Math.abs(delta) < 0.5) {
            // Change too small - white
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            return;
        }
        
        // Determine if change is favorable
        boolean isFavorable;
        if (isGapAhead) {
            // Gap ahead shrinking is good (catching up)
            isFavorable = delta < 0;
        } else {
            // Gap behind growing is good (pulling away)
            isFavorable = delta > 0;
        }
        
        // Apply color
        int color = isFavorable ? R.color.gap_positive : R.color.gap_negative;
        textView.setTextColor(ContextCompat.getColor(this, color));
    }
    
    /**
     * Parse a gap string to seconds for trend comparison.
     * 
     * @param gap Gap string (e.g., "1.234", "+2.567", "LEAD", "2 Laps")
     * @return Seconds as Double, or null if not parseable
     */
    private Double parseGapToSeconds(String gap) {
        if (gap == null || gap.isEmpty()) {
            return null;
        }
        
        // Remove leading + sign if present
        String cleaned = gap.startsWith("+") ? gap.substring(1) : gap;
        
        // Skip special values
        if (cleaned.equalsIgnoreCase(SpeedHiveManager.LEADER_TEXT) || cleaned.equalsIgnoreCase(SpeedHiveManager.LAST_TEXT) ||
            cleaned.contains("Lap") || cleaned.contains("lap")) {
            return null;
        }
        
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Format a gap value for display (1 decimal place).
     * Preserves special values like LEAD, LAST, and lap counts.
     * 
     * @param gap Raw gap string from API or demo
     * @return Formatted gap string (e.g., "1.234" → "1.2")
     */
    private String formatGapForDisplay(String gap) {
        if (gap == null || gap.isEmpty()) {
            return gap;
        }
        
        // Preserve special values
        if (gap.equalsIgnoreCase(SpeedHiveManager.LEADER_TEXT) || gap.equalsIgnoreCase(SpeedHiveManager.LAST_TEXT) ||
            gap.contains("Lap") || gap.contains("lap") ||
            gap.equalsIgnoreCase("Unknown") || gap.equals("---")) {
            return gap;
        }
        
        // Try to parse and reformat numeric gaps
        try {
            // Remove leading + if present
            String cleaned = gap.startsWith("+") ? gap.substring(1) : gap;
            double seconds = Double.parseDouble(cleaned);
            // Format to 1 decimal place
            return String.format(Locale.US, "%.1f", seconds);
        } catch (NumberFormatException e) {
            // Can't parse - return original
            return gap;
        }
    }

    /**
     * Initialize the event/session display based on current settings.
     */
    private void initializeEventSessionDisplay() {
        String eventName = preferences.getSpeedHiveEventName();
        String sessionId = preferences.getSpeedHiveSessionId();
        String sessionName = preferences.getSpeedHiveSessionName();
        String mode = preferences.getSpeedHiveMode();
        
        if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(mode)) {
            // Demo mode
            currentEventName = "Demo Race";
            currentSessionName = "Practice Session";
            updateEventSessionDisplay();
        } else if (!eventName.isEmpty()) {
            // Real SpeedHive mode
            currentEventName = eventName;
            if (PitWindowPreferences.SPEEDHIVE_SESSION_AUTO.equals(sessionId)) {
                currentSessionName = "AUTO"; // Will be updated when session is detected
            } else {
                currentSessionName = sessionName.isEmpty() ? "Unknown Session" : sessionName;
            }
            updateEventSessionDisplay();
        } else {
            // No event configured yet
            textEventSession.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update the event/session display text.
     */
    private void updateEventSessionDisplay() {
        if (currentEventName.isEmpty()) {
            textEventSession.setVisibility(View.GONE);
            return;
        }
        
        String displayText;
        if (currentSessionName.isEmpty()) {
            displayText = currentEventName;
        } else {
            displayText = currentEventName + " - " + currentSessionName;
        }
        
        textEventSession.setText(displayText);
        textEventSession.setVisibility(View.VISIBLE);
        
        Log.d(TAG, "Updated event/session display: " + displayText);
    }
    
    /**
     * Check for session changes in AUTO mode (runs every 60 seconds).
     * Detects if we should switch to a different session (e.g., qualifying → race).
     */
    private void checkForSessionChange() {
        String mode = preferences.getSpeedHiveMode();
        String sessionId = preferences.getSpeedHiveSessionId();
        
        // Don't check for session changes in demo mode
        if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(mode)) {
            return;
        }
        
        // Only check for changes in AUTO mode
        if (!PitWindowPreferences.SPEEDHIVE_SESSION_AUTO.equals(sessionId)) {
            return;
        }
        
        String eventId = preferences.getSpeedHiveEventId();
        String carNumber = preferences.getSpeedHiveCarNumber();
        
        if (eventId.isEmpty() || carNumber.isEmpty() || !(speedHiveManager instanceof SpeedHiveManager)) {
            return;
        }
        
        Log.d(TAG, "Checking for session changes (AUTO mode) for car #" + carNumber);
        
        SpeedHiveManager realManager = (SpeedHiveManager) speedHiveManager;
        realManager.findSessionWithCar(eventId, carNumber, new SpeedHiveManager.AutoSessionCallback() {
            @Override
            public void onSuccess(String detectedSessionId, String sessionName) {
                runOnUiThread(() -> handleSessionChangeCheck(detectedSessionId, sessionName));
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Session change check failed: " + error);
                // Don't update UI for session check errors - keep current session
            }
        });
    }
    
    /**
     * Handle the result of a session change check.
     * @param detectedSessionId The newly detected optimal session ID
     * @param detectedSessionName The newly detected session name
     */
    private void handleSessionChangeCheck(String detectedSessionId, String detectedSessionName) {
        // Compare with current session
        if (detectedSessionId.equals(currentSessionId)) {
            // Same session - no change needed
            Log.d(TAG, "Session unchanged: " + currentSessionName);
            return;
        }
        
        // Different session detected
        Log.i(TAG, "Session change detected: " + currentSessionName + " → " + detectedSessionName);
        
        // Update tracking variables
        currentSessionId = detectedSessionId;
        currentSessionName = detectedSessionName;
        
        // Update display
        updateEventSessionDisplay();
        
        // Note: We don't need to update preferences or restart polling - 
        // the polling logic will automatically use the new detected session
        Toast.makeText(this, "Switched to session: " + detectedSessionName, Toast.LENGTH_LONG).show();
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
