package at.semmal.pitstopper.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

/**
 * Detects when the device has been standing still (not moving) for a specified duration.
 * Uses GPS location and the built-in speed measurement from Location.getSpeed().
 *
 * Usage:
 * 1. Create instance with context and listener
 * 2. Call startMonitoring() when pit window opens
 * 3. Call stopMonitoring() when pit window closes or alert is cleared
 * 4. Listener's onStandstillDetected() called when car stops for required duration
 */
public class StandstillDetector {

    private static final String TAG = "StandstillDetector";

    /**
     * Listener interface for standstill detection events.
     */
    public interface StandstillListener {
        /**
         * Called when the device has been standing still for the required duration.
         */
        void onStandstillDetected();

        /**
         * Called when movement is detected after being still.
         */
        void onMovementDetected();
    }

    // Speed threshold in m/s (5 km/h = 1.39 m/s, using 1.5 for safety margin)
    private static final float STANDSTILL_SPEED_THRESHOLD_MS = 1.5f;

    // How long device must be still before triggering (in milliseconds)
    private static final long STANDSTILL_DURATION_MS = 5000; // 5 seconds

    // Location update interval
    private static final long LOCATION_UPDATE_INTERVAL_MS = 1000; // 1 second

    private final Context context;
    private final StandstillListener listener;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isMonitoring = false;
    private long standstillStartTime = 0;
    private boolean isCurrentlyStandstill = false;
    private boolean standstillReported = false;

    /**
     * Creates a new StandstillDetector.
     *
     * @param context  Application context
     * @param listener Listener for standstill events
     */
    public StandstillDetector(@NonNull Context context, @NonNull StandstillListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        setupLocationCallback();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    processLocation(location);
                }
            }
        };
    }

    /**
     * Process incoming location update and check for standstill.
     * Uses Location.getSpeed() which is provided directly by GPS.
     */
    private void processLocation(Location location) {
        float speed = 0;

        // Location.getSpeed() returns speed in m/s
        // It's only valid if hasSpeed() returns true
        if (location.hasSpeed()) {
            speed = location.getSpeed();
        }

        // Log current speed for debugging
        Log.d(TAG, String.format(Locale.US, "Speed: %.2f m/s (%.1f km/h) | hasSpeed: %b | isStill: %b | reported: %b",
                speed, speed * 3.6f, location.hasSpeed(), isCurrentlyStandstill, standstillReported));

        // Check if below standstill threshold
        if (speed < STANDSTILL_SPEED_THRESHOLD_MS) {
            if (!isCurrentlyStandstill) {
                // Just started being still
                standstillStartTime = System.currentTimeMillis();
                isCurrentlyStandstill = true;
            } else if (!standstillReported) {
                // Check if we've been still long enough
                long stillDuration = System.currentTimeMillis() - standstillStartTime;
                if (stillDuration >= STANDSTILL_DURATION_MS) {
                    standstillReported = true;
                    listener.onStandstillDetected();
                }
            }
        } else {
            // Moving - reset standstill tracking
            if (isCurrentlyStandstill && standstillReported) {
                listener.onMovementDetected();
            }
            isCurrentlyStandstill = false;
            standstillStartTime = 0;
            standstillReported = false;
        }
    }

    /**
     * Start monitoring for standstill.
     * Call this when pit window opens or when you want to detect stopping.
     *
     * @return true if monitoring started successfully, false if permissions missing
     */
    public boolean startMonitoring() {
        if (isMonitoring) {
            return true; // Already monitoring
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false; // Permission not granted
        }

        // Reset state
        isCurrentlyStandstill = false;
        standstillStartTime = 0;
        standstillReported = false;

        // Create location request
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL_MS / 2)
                .build();

        // Start location updates
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );

        isMonitoring = true;
        return true;
    }

    /**
     * Stop monitoring for standstill.
     * Call this when pit window closes or alert is cleared.
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback);
        isMonitoring = false;
        isCurrentlyStandstill = false;
        standstillStartTime = 0;
        standstillReported = false;
    }

    /**
     * Check if currently monitoring.
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * Check if device is currently in standstill state.
     */
    public boolean isStandstill() {
        return isCurrentlyStandstill && standstillReported;
    }

    /**
     * Get the speed threshold used for standstill detection (in m/s).
     */
    public static float getSpeedThresholdMs() {
        return STANDSTILL_SPEED_THRESHOLD_MS;
    }

    /**
     * Get the speed threshold in km/h for display purposes.
     */
    public static float getSpeedThresholdKmh() {
        return STANDSTILL_SPEED_THRESHOLD_MS * 3.6f; // Convert m/s to km/h
    }

    /**
     * Get the required standstill duration in seconds.
     */
    public static long getStandstillDurationSeconds() {
        return STANDSTILL_DURATION_MS / 1000;
    }
}

