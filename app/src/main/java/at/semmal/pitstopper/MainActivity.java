package at.semmal.pitstopper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private LiveTimingData previousTimingData; // For gap trend comparison
    private static final int SPEEDHIVE_POLL_INTERVAL_MS = 10000; // 10 seconds

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
        
        // Stop SpeedHive polling
        handler.removeCallbacks(speedHivePollingRunnable);

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
            // Hide live timing panel
            liveTimingPanel.setVisibility(View.GONE);
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
                    return;
                }
            } else if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(mode)) {
                // Create demo SpeedHive manager
                speedHiveManager = new DemoSpeedHiveManager(this);
                Log.i(TAG, "Demo SpeedHive manager initialized");
            }
            
            // Start polling if we have a manager
            if (speedHiveManager != null) {
                // Reset previous data
                previousTimingData = null;
                
                // Start polling immediately, then every 10 seconds
                pollSpeedHive();
                handler.postDelayed(speedHivePollingRunnable, SPEEDHIVE_POLL_INTERVAL_MS);
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
        
        if (eventId.isEmpty() || sessionId.isEmpty() || carNumber.isEmpty()) {
            Log.w(TAG, "SpeedHive settings incomplete - skipping poll");
            updateLiveTimingUI(null, "Settings incomplete");
            return;
        }
        
        Log.d(TAG, "Polling SpeedHive for car #" + carNumber);
        
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
            // Update position
            textPosition.setText(data.getFormattedPosition());
            
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
        textView.setText(currentGap);
        
        if (previousGap == null || currentGap.equals(previousGap)) {
            // No previous data or no change - use white
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            return;
        }
        
        // Try to parse gaps as seconds for comparison
        Double currentSeconds = parseGapToSeconds(currentGap);
        Double previousSeconds = parseGapToSeconds(previousGap);
        
        if (currentSeconds == null || previousSeconds == null) {
            // Can't compare (probably "LEADER", "LAST", or "X Laps") - use white
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
     * @param gap Gap string (e.g., "1.234", "+2.567", "LEADER", "2 Laps")
     * @return Seconds as Double, or null if not parseable
     */
    private Double parseGapToSeconds(String gap) {
        if (gap == null || gap.isEmpty()) {
            return null;
        }
        
        // Remove leading + sign if present
        String cleaned = gap.startsWith("+") ? gap.substring(1) : gap;
        
        // Skip special values
        if (cleaned.equalsIgnoreCase("LEADER") || cleaned.equalsIgnoreCase("LAST") ||
            cleaned.contains("Lap") || cleaned.contains("lap")) {
            return null;
        }
        
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
