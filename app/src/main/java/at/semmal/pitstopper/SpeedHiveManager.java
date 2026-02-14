package at.semmal.pitstopper;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager for SpeedHive Live Timing API communication.
 * Handles authentication, HTTP requests, and data parsing.
 */
public class SpeedHiveManager {
    
    private static final String TAG = "SpeedHiveManager";
    
    /** Constant for leader position display text */
    public static final String LEADER_TEXT = "LEAD";
    
    /** Constant for last position display text */
    public static final String LAST_TEXT = "LAST";
    
    private final SpeedHiveConfig config;
    private final ExecutorService executor;
    
    /**
     * Callback interface for live timing data requests.
     */
    public interface LiveTimingCallback {
        /**
         * Called when live timing data is successfully retrieved.
         * @param data Live timing data for the requested car
         */
        void onSuccess(LiveTimingData data);
        
        /**
         * Called when an error occurs retrieving live timing data.
         * @param error Error message describing what went wrong
         */
        void onError(String error);
    }
    
    /**
     * Create a new SpeedHive manager.
     * @param context Application context for loading configuration
     */
    public SpeedHiveManager(Context context) {
        this.config = new SpeedHiveConfig(context);
        this.executor = Executors.newSingleThreadExecutor();
        Log.i(TAG, "SpeedHive manager initialized");
    }
    
    /**
     * Fetch current leaderboard data and extract information for a specific car.
     * This method makes an async HTTP call and returns results via callback.
     * 
     * @param eventId SpeedHive event ID (e.g., "QMJLNRVR-2147484360")
     * @param sessionId SpeedHive session ID (e.g., "EVENTID-1073741830")
     * @param carNumber Car number to find (e.g., "88")
     * @param callback Callback to receive results or errors
     */
    public void fetchLeaderboard(String eventId, String sessionId, String carNumber, LiveTimingCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError("Event ID is required");
            return;
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            callback.onError("Session ID is required");
            return;
        }
        
        if (carNumber == null || carNumber.trim().isEmpty()) {
            callback.onError("Car number is required");
            return;
        }
        
        // Execute HTTP request on background thread
        executor.execute(() -> {
            try {
                String endpoint = String.format("/events/%s/sessions/%s/data", 
                                               eventId.trim(), sessionId.trim());
                String urlString = config.buildUrl(endpoint);
                
                Log.d(TAG, "Fetching leaderboard from: " + urlString);
                
                HttpURLConnection connection = createConnection(urlString);
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseBody = readResponse(connection);
                    LiveTimingData data = parseLeaderboardResponse(responseBody, carNumber.trim());
                    callback.onSuccess(data);
                } else {
                    String errorMsg = String.format("HTTP %d: %s", responseCode, readErrorResponse(connection));
                    Log.e(TAG, "SpeedHive API error - " + errorMsg);
                    callback.onError(errorMsg);
                }
                
                connection.disconnect();
                
            } catch (IOException e) {
                Log.e(TAG, "Network error fetching leaderboard", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error fetching leaderboard", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Parse the JSON response from the leaderboard API and extract data for our car.
     * 
     * @param jsonResponse Raw JSON response from SpeedHive API
     * @param carNumber Car number to find in the leaderboard
     * @return LiveTimingData for the specified car
     * @throws JSONException if JSON parsing fails
     */
    private LiveTimingData parseLeaderboardResponse(String jsonResponse, String carNumber) throws JSONException {
        Log.d(TAG, "Parsing leaderboard response for car #" + carNumber);
        
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray leaderboard = root.getJSONArray("l"); // 'l' is the leaderboard array
        
        int totalCompetitors = leaderboard.length();
        
        // Find our car in the leaderboard
        for (int i = 0; i < leaderboard.length(); i++) {
            JSONObject competitor = leaderboard.getJSONObject(i);
            String competitorNumber = competitor.optString("no", ""); // 'no' is car number
            
            if (carNumber.equals(competitorNumber)) {
                // Found our car - extract data
                int position = competitor.optInt("pos", 0);
                String driverName = competitor.optString("nam", "Unknown");
                String gapAhead = extractGapAhead(competitor, position);
                String gapBehind = extractGapBehind(leaderboard, position);
                
                LiveTimingData data = new LiveTimingData(position, gapAhead, gapBehind, 
                                                        carNumber, driverName, totalCompetitors);
                
                Log.i(TAG, "Found car data: " + data.toString());
                return data;
            }
        }
        
        // Car not found in leaderboard
        throw new RuntimeException("Car #" + carNumber + " not found in current leaderboard");
    }
    
    /**
     * Extract gap to car ahead from competitor data.
     * 
     * @param competitor JSON object for our car
     * @param position Our car's position
     * @return Gap string ("LEAD" if P1, otherwise gap to car ahead)
     */
    private String extractGapAhead(JSONObject competitor, int position) {
        if (position == 1) {
            return LEADER_TEXT;
        } else {
            // 'gp' field is gap to car ahead (previous position)
            String gap = competitor.optString("gp", "");
            return gap.isEmpty() ? "Unknown" : gap;
        }
    }
    
    /**
     * Extract gap to car behind by finding the next position's 'gp' value.
     * 
     * @param leaderboard Full leaderboard array
     * @param position Our car's position
     * @return Gap string ("LAST" if last place, otherwise next car's gap to us)
     */
    private String extractGapBehind(JSONArray leaderboard, int position) throws JSONException {
        // Find the car in position+1 and get their 'gp' value (gap to us)
        for (int i = 0; i < leaderboard.length(); i++) {
            JSONObject competitor = leaderboard.getJSONObject(i);
            int competitorPos = competitor.optInt("pos", 0);
            
            if (competitorPos == position + 1) {
                String gap = competitor.optString("gp", "");
                return gap.isEmpty() ? "Unknown" : gap;
            }
        }
        
        // No car found behind us (we're last)
        return LAST_TEXT;
    }
    
    /**
     * Callback interface for events list requests.
     */
    public interface EventsCallback {
        void onSuccess(List<SpeedHiveEvent> events);
        void onError(String error);
    }

    /**
     * Callback interface for sessions list requests.
     */
    public interface SessionsCallback {
        void onSuccess(List<SpeedHiveSession> sessions);
        void onError(String error);
    }

    /**
     * Callback interface for cars list requests.
     */
    public interface CarsCallback {
        void onSuccess(List<SpeedHiveCar> cars);
        void onError(String error);
    }

    /**
     * Fetch all available events from the SpeedHive live timing API.
     * Returns events sorted with live events first.
     */
    public void fetchEvents(EventsCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = config.buildUrl("/events");
                Log.d(TAG, "Fetching events from: " + urlString);

                HttpURLConnection connection = createConnection(urlString);
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseBody = readResponse(connection);
                    List<SpeedHiveEvent> events = parseEventsResponse(responseBody);

                    // Sort: live events first, then by date descending
                    Collections.sort(events, (a, b) -> {
                        if (a.isLive() != b.isLive()) {
                            return a.isLive() ? -1 : 1;
                        }
                        return 0;
                    });

                    Log.i(TAG, "Fetched " + events.size() + " events");
                    callback.onSuccess(events);
                } else {
                    String errorMsg = "HTTP " + responseCode + ": " + readErrorResponse(connection);
                    Log.e(TAG, "Events API error - " + errorMsg);
                    callback.onError(errorMsg);
                }

                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Network error fetching events", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching events", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch sessions for a specific event.
     * Returns sessions sorted with active sessions first.
     */
    public void fetchSessions(String eventId, boolean parentEventLive, SessionsCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError("Event ID is required");
            return;
        }

        executor.execute(() -> {
            try {
                String endpoint = String.format("/events/%s?sessions=true", eventId.trim());
                String urlString = config.buildUrl(endpoint);
                Log.d(TAG, "Fetching sessions from: " + urlString);

                HttpURLConnection connection = createConnection(urlString);
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseBody = readResponse(connection);
                    List<SpeedHiveSession> sessions = parseSessionsResponse(responseBody, parentEventLive);

                    // Sort: active sessions first
                    Collections.sort(sessions, (a, b) -> {
                        if (a.isActive() != b.isActive()) {
                            return a.isActive() ? -1 : 1;
                        }
                        return 0;
                    });

                    Log.i(TAG, "Fetched " + sessions.size() + " sessions for event " + eventId);
                    callback.onSuccess(sessions);
                } else {
                    String errorMsg = "HTTP " + responseCode + ": " + readErrorResponse(connection);
                    Log.e(TAG, "Sessions API error - " + errorMsg);
                    callback.onError(errorMsg);
                }

                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Network error fetching sessions", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching sessions", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch cars/competitors for a specific session.
     * Returns cars sorted by number.
     */
    public void fetchCars(String eventId, String sessionId, CarsCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError("Event ID is required");
            return;
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            callback.onError("Session ID is required");
            return;
        }

        executor.execute(() -> {
            try {
                String endpoint = String.format("/events/%s/sessions/%s/data", 
                                               eventId.trim(), sessionId.trim());
                String urlString = config.buildUrl(endpoint);
                Log.d(TAG, "Fetching cars from: " + urlString);

                HttpURLConnection connection = createConnection(urlString);
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseBody = readResponse(connection);
                    List<SpeedHiveCar> cars = parseCarsResponse(responseBody);
                    callback.onSuccess(cars);
                } else {
                    String errorMsg = "HTTP " + responseCode + ": " + readErrorResponse(connection);
                    Log.e(TAG, "Cars API error - " + errorMsg);
                    callback.onError(errorMsg);
                }

                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Network error fetching cars", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching cars", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    private HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("ApiKey", config.getApiKey());
        connection.setRequestProperty("User-Agent", config.getUserAgent());
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private String readErrorResponse(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private List<SpeedHiveEvent> parseEventsResponse(String jsonResponse) throws JSONException {
        List<SpeedHiveEvent> events = new ArrayList<>();
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray liveEvents = root.optJSONArray("LiveEvents");

        if (liveEvents == null) return events;

        for (int i = 0; i < liveEvents.length(); i++) {
            JSONObject ev = liveEvents.getJSONObject(i);
            JSONObject location = ev.optJSONObject("l");
            JSONObject track = ev.optJSONObject("t");

            events.add(new SpeedHiveEvent(
                ev.optString("id", ""),
                ev.optString("n", "Unknown Event"),
                ev.optString("dt", ""),
                location != null ? location.optString("c", "") : "",
                location != null ? location.optString("ct", "") : "",
                ev.optInt("s", 0),
                track != null ? track.optString("n", "") : ""
            ));
        }
        return events;
    }

    private List<SpeedHiveSession> parseSessionsResponse(String jsonResponse, boolean parentEventLive) throws JSONException {
        List<SpeedHiveSession> sessions = new ArrayList<>();
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray ss = root.optJSONArray("ss");

        if (ss == null) return sessions;

        for (int i = 0; i < ss.length(); i++) {
            JSONObject s = ss.getJSONObject(i);
            sessions.add(new SpeedHiveSession(
                s.optString("id", ""),
                s.optString("eId", ""),
                s.optString("rnNam", "Session"),
                s.optString("gNam", ""),
                s.optInt("ls", 0),
                s.optString("btLpTim", null),
                s.optString("rcTm", null),
                s.optString("stod", ""),
                s.optInt("f", 3), // Default to 3 (finished) if not present
                parentEventLive
            ));
        }
        return sessions;
    }

    private List<SpeedHiveCar> parseCarsResponse(String jsonResponse) throws JSONException {
        List<SpeedHiveCar> cars = new ArrayList<>();
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray leaderboard = root.optJSONArray("l"); // 'l' is the leaderboard array

        if (leaderboard == null) return cars;

        // Extract all cars from leaderboard
        for (int i = 0; i < leaderboard.length(); i++) {
            JSONObject competitor = leaderboard.getJSONObject(i);
            String carNumber = competitor.optString("no", "");
            String driverName = competitor.optString("nam", "Unknown");
            
            if (!carNumber.isEmpty()) {
                cars.add(new SpeedHiveCar(carNumber, driverName));
            }
        }

        // Sort cars by car number (numerical sort)
        cars.sort((car1, car2) -> {
            try {
                int num1 = Integer.parseInt(car1.getNumber());
                int num2 = Integer.parseInt(car2.getNumber());
                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                // If not numeric, sort alphabetically
                return car1.getNumber().compareTo(car2.getNumber());
            }
        });

        return cars;
    }

    /**
     * Callback interface for auto-detecting sessions containing a specific car.
     */
    public interface AutoSessionCallback {
        void onSuccess(String sessionId, String sessionName);
        void onError(String error);
    }
    
    /**
     * Automatically find the latest live session containing the specified car number.
     * Checks all sessions for an event and finds the most recent one with live data
     * that contains the given car number.
     * 
     * @param eventId SpeedHive event ID
     * @param carNumber Car number to search for
     * @param callback Callback to receive the session ID or error
     */
    public void findSessionWithCar(String eventId, String carNumber, AutoSessionCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError("Event ID is required");
            return;
        }
        
        if (carNumber == null || carNumber.trim().isEmpty()) {
            callback.onError("Car number is required");
            return;
        }
        
        Log.d(TAG, "Auto-detecting session for car #" + carNumber + " in event " + eventId);
        
        // First, fetch all sessions for the event
        fetchSessions(eventId, true, new SessionsCallback() {
            @Override
            public void onSuccess(List<SpeedHiveSession> sessions) {
                // Filter to only active/live sessions and search them
                searchSessionsForCar(eventId, carNumber, sessions, callback);
            }
            
            @Override
            public void onError(String error) {
                callback.onError("Failed to fetch sessions: " + error);
            }
        });
    }
    
    /**
     * Search through sessions to find one containing the specified car.
     * Prioritizes active sessions and checks them in reverse order (newest first).
     */
    private void searchSessionsForCar(String eventId, String carNumber, List<SpeedHiveSession> sessions, AutoSessionCallback callback) {
        executor.execute(() -> {
            // Filter to active sessions first, then all sessions as fallback
            List<SpeedHiveSession> activeSessions = new ArrayList<>();
            for (SpeedHiveSession session : sessions) {
                if (session.isActive()) {
                    activeSessions.add(session);
                }
            }
            
            // Try active sessions first (most likely to have current data)
            List<SpeedHiveSession> sessionsToCheck = activeSessions.isEmpty() ? sessions : activeSessions;
            
            Log.d(TAG, "Checking " + sessionsToCheck.size() + " sessions for car #" + carNumber);
            
            for (SpeedHiveSession session : sessionsToCheck) {
                try {
                    // Check if this session contains our car
                    if (sessionContainsCar(eventId, session.getId(), carNumber)) {
                        Log.i(TAG, "Found car #" + carNumber + " in session: " + session.getRunName());
                        callback.onSuccess(session.getId(), session.getRunName());
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error checking session " + session.getId() + " for car #" + carNumber, e);
                    // Continue checking other sessions
                }
            }
            
            // Car not found in any session
            callback.onError("Car #" + carNumber + " not found in any available session");
        });
    }
    
    /**
     * Check if a specific session contains the given car number.
     * @param eventId SpeedHive event ID
     * @param sessionId SpeedHive session ID
     * @param carNumber Car number to search for
     * @return true if session contains the car, false otherwise
     */
    private boolean sessionContainsCar(String eventId, String sessionId, String carNumber) throws Exception {
        String endpoint = String.format("/events/%s/sessions/%s/data", eventId.trim(), sessionId.trim());
        String urlString = config.buildUrl(endpoint);
        
        Log.d(TAG, "Checking session " + sessionId + " for car #" + carNumber);
        
        HttpURLConnection connection = createConnection(urlString);
        int responseCode = connection.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String responseBody = readResponse(connection);
            connection.disconnect();
            
            // Parse response and look for our car number
            JSONObject root = new JSONObject(responseBody);
            JSONArray leaderboard = root.getJSONArray("l");
            
            for (int i = 0; i < leaderboard.length(); i++) {
                JSONObject competitor = leaderboard.getJSONObject(i);
                String competitorNumber = competitor.optString("no", "");
                if (carNumber.equals(competitorNumber)) {
                    return true;
                }
            }
        } else {
            connection.disconnect();
            Log.w(TAG, "Session " + sessionId + " returned HTTP " + responseCode);
        }
        
        return false;
    }

    /**
     * Clean up resources when no longer needed.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            Log.i(TAG, "SpeedHive manager shut down");
        }
    }
}