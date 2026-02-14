package at.semmal.pitstopper;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Demo implementation of SpeedHive live timing that simulates a realistic race.
 * Returns consistent race data with evolving positions and lap times.
 */
public class DemoSpeedHiveManager extends SpeedHiveManager {
    
    private static final String TAG = "DemoSpeedHiveManager";
    
    // Demo race configuration
    private static final int TOTAL_CARS = 8;
    private static final double BASE_LAP_TIME_SECONDS = 88.0; // 1:28.000 base time
    private static final double LAP_TIME_VARIATION = 7.0; // ±7 seconds range
    private static final double JITTER_RANGE = 1.5; // ±1.5s per lap jitter
    
    // Internal state
    private final List<DemoCar> cars;
    private final Random random;
    private int pollCount = 0;
    
    /**
     * Demo car data structure
     */
    private static class DemoCar {
        final String carNumber;
        final String driverName;
        final double baseLapTime; // Base lap time in seconds
        double totalTime; // Accumulated race time
        int laps;
        int currentPosition;
        
        DemoCar(String carNumber, String driverName, double baseLapTime) {
            this.carNumber = carNumber;
            this.driverName = driverName;
            this.baseLapTime = baseLapTime;
            this.totalTime = 0.0;
            this.laps = 0;
            this.currentPosition = 1;
        }
    }
    
    /**
     * Create demo SpeedHive manager with simulated race data.
     */
    public DemoSpeedHiveManager(Context context) {
        super(context);
        
        this.random = new Random();
        this.cars = new ArrayList<>();
        
        // Initialize demo cars with realistic data
        initializeDemoCars();
        
        Log.i(TAG, "Demo SpeedHive manager initialized with " + TOTAL_CARS + " cars");
    }
    
    /**
     * Initialize the demo car lineup with varied lap times.
     */
    private void initializeDemoCars() {
        String[] carNumbers = {"88", "23", "77", "42", "15", "99", "7", "33"};
        String[] driverNames = {"JOHNSON", "RACER-X", "STEALTH", "MARTINEZ", "SPEEDSTER", "PHANTOM", "ACE", "VIPER"};
        
        for (int i = 0; i < TOTAL_CARS; i++) {
            // Spread lap times across the range
            double lapTimeVariation = (LAP_TIME_VARIATION * 2 * i / (TOTAL_CARS - 1)) - LAP_TIME_VARIATION;
            double baseLapTime = BASE_LAP_TIME_SECONDS + lapTimeVariation;
            
            cars.add(new DemoCar(carNumbers[i], driverNames[i], baseLapTime));
        }
    }
    
    @Override
    public void fetchLeaderboard(String eventId, String sessionId, String carNumber, LiveTimingCallback callback) {
        Log.d(TAG, "Demo poll #" + (++pollCount) + " for car #" + carNumber);
        
        // Simulate realistic lap progression
        simulateRaceProgression();
        
        // Calculate current positions
        updatePositions();
        
        // Find our car
        DemoCar ourCar = findCarByNumber(carNumber);
        if (ourCar == null) {
            callback.onError("Car #" + carNumber + " not found in demo race. Available cars: " + getAvailableCarNumbers());
            return;
        }
        
        // Build live timing data
        LiveTimingData data = buildLiveTimingData(ourCar);
        
        // Simulate API response delay (50-200ms)
        int delay = 50 + random.nextInt(150);
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> callback.onSuccess(data), delay);
    }
    
    /**
     * Simulate race progression - each car completes another partial lap.
     */
    private void simulateRaceProgression() {
        for (DemoCar car : cars) {
            // Generate a lap time with jitter
            double jitter = (random.nextDouble() - 0.5) * 2 * JITTER_RANGE;
            double lapTime = car.baseLapTime + jitter;
            
            // Occasionally have a significantly faster or slower lap (pit stops, mistakes, etc.)
            if (random.nextDouble() < 0.1) { // 10% chance
                if (random.nextBoolean()) {
                    // Fast lap (good setup, slipstream, etc.)
                    lapTime *= 0.97; // 3% faster
                } else {
                    // Slow lap (traffic, mistake, pit stop simulation)
                    lapTime *= 1.15; // 15% slower
                }
            }
            
            // Add lap time to total
            car.totalTime += lapTime;
            car.laps++;
        }
    }
    
    /**
     * Update positions based on total race time.
     */
    private void updatePositions() {
        // Sort by total time (less time = better position)
        cars.sort((a, b) -> Double.compare(a.totalTime, b.totalTime));
        
        // Assign positions
        for (int i = 0; i < cars.size(); i++) {
            cars.get(i).currentPosition = i + 1;
        }
    }
    
    /**
     * Find a car by its number.
     */
    private DemoCar findCarByNumber(String carNumber) {
        for (DemoCar car : cars) {
            if (car.carNumber.equals(carNumber)) {
                return car;
            }
        }
        return null;
    }
    
    /**
     * Get comma-separated list of available car numbers.
     */
    private String getAvailableCarNumbers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cars.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("#").append(cars.get(i).carNumber);
        }
        return sb.toString();
    }
    
    /**
     * Build LiveTimingData for our car.
     */
    private LiveTimingData buildLiveTimingData(DemoCar ourCar) {
        int position = ourCar.currentPosition;
        
        // Calculate gap ahead
        String gapAhead;
        if (position == 1) {
            gapAhead = SpeedHiveManager.LEADER_TEXT;
        } else {
            DemoCar carAhead = cars.get(position - 2); // position-1 index, then -1 more for car ahead
            double gapSeconds = ourCar.totalTime - carAhead.totalTime;
            gapAhead = formatGap(gapSeconds);
        }
        
        // Calculate gap behind
        String gapBehind;
        if (position == TOTAL_CARS) {
            gapBehind = SpeedHiveManager.LAST_TEXT;
        } else {
            DemoCar carBehind = cars.get(position); // position-1 index, then +1 for car behind
            double gapSeconds = carBehind.totalTime - ourCar.totalTime;
            gapBehind = formatGap(gapSeconds);
        }
        
        return new LiveTimingData(
            position,
            gapAhead,
            gapBehind,
            ourCar.carNumber,
            ourCar.driverName,
            TOTAL_CARS
        );
    }
    
    /**
     * Format a gap in seconds as a readable string.
     * 
     * @param gapSeconds Gap in seconds (positive number)
     * @return Formatted gap string (e.g., "1.234", "12.567")
     */
    private String formatGap(double gapSeconds) {
        if (gapSeconds < 0) {
            gapSeconds = Math.abs(gapSeconds);
        }
        
        if (gapSeconds >= 60) {
            // If gap is very large, show as laps (simplified)
            int laps = (int) (gapSeconds / BASE_LAP_TIME_SECONDS);
            return laps + " Lap" + (laps > 1 ? "s" : "");
        } else {
            // Show as seconds with 3 decimal places
            return String.format(Locale.US, "%.3f", gapSeconds);
        }
    }
    
    @Override
    public void shutdown() {
        Log.i(TAG, "Demo SpeedHive manager shut down after " + pollCount + " polls");
        super.shutdown();
    }
}