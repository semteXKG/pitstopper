package at.semmal.pitstopper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PitStopper";

    private TextView textCurrentTime;
    private TextView textCountdown;
    private ImageButton buttonSettings;
    private ConstraintLayout rootLayout;
    private View progressBar;
    private FrameLayout progressBarContainer;
    private Handler handler;
    private Runnable updateTimeRunnable;
    private SimpleDateFormat timeFormat;

    private PitWindowPreferences preferences;
    private PitWindowAlertManager alertManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        textCurrentTime = findViewById(R.id.textCurrentTime);
        textCountdown = findViewById(R.id.textCountdown);
        buttonSettings = findViewById(R.id.buttonSettings);
        rootLayout = findViewById(R.id.rootLayout);
        progressBar = findViewById(R.id.progressBar);
        progressBarContainer = findViewById(R.id.progressBarContainer);

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

        // Set up settings button click listener
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Enable fullscreen immersive mode
        hideSystemUI();
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

        // Start updating the clock when activity becomes visible
        updateTime(); // Update immediately
        handler.postDelayed(updateTimeRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updating the clock when activity is no longer visible
        handler.removeCallbacks(updateTimeRunnable);
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

        // Update progress bar using alert manager
        int stageProgress = alertManager.getProgressInCurrentStage(currentHour, currentMinute, currentSecond);
        updateProgressBar(stageProgress);

        // Log progress value for debugging
        Log.d(TAG, String.format(Locale.getDefault(), "Progress: %d%% | State: %s | Time: %02d:%02d:%02d",
                stageProgress, alertState.name(), currentHour, currentMinute, currentSecond));

        if (alertState == PitWindowAlertManager.AlertState.ON_ALERT) {
            // Flash effect: if seconds % 4 < 2, show black; otherwise show green
            if (currentSecond % 4 < 2) {
                rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.background_primary));
            } else {
                rootLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.alert_green));
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
     * Updates the progress bar height based on the progress percentage.
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

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
