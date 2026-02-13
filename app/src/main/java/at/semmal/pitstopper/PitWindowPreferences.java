package at.semmal.pitstopper;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public class PitWindowPreferences {

    private static final String PREFS_NAME = "PitWindowPrefs";
    private static final String KEY_RACE_START_HOUR = "race_start_hour";
    private static final String KEY_RACE_START_MINUTE = "race_start_minute";
    private static final String KEY_PIT_WINDOW_OPENS = "pit_window_opens";
    private static final String KEY_PIT_WINDOW_DURATION = "pit_window_duration";
    
    // SpeedHive Live Timing settings
    private static final String KEY_SPEEDHIVE_MODE = "speedhive_mode";
    private static final String KEY_SPEEDHIVE_EVENT_ID = "speedhive_event_id";
    private static final String KEY_SPEEDHIVE_EVENT_NAME = "speedhive_event_name";
    private static final String KEY_SPEEDHIVE_SESSION_ID = "speedhive_session_id";
    private static final String KEY_SPEEDHIVE_SESSION_NAME = "speedhive_session_name";
    private static final String KEY_SPEEDHIVE_CAR_NUMBER = "speedhive_car_number";
    private static final String KEY_SPEEDHIVE_CAR_NAME = "speedhive_car_name";
    
    // MQTT Server settings
    private static final String KEY_MQTT_SERVER_PORT = "mqtt_server_port";
    private static final String KEY_MQTT_SERVER_ENABLED = "mqtt_server_enabled";

    // Default values
    private static final int DEFAULT_RACE_START_HOUR = 9;
    private static final int DEFAULT_RACE_START_MINUTE = 0;
    private static final int DEFAULT_PIT_WINDOW_OPENS = 17;
    private static final int DEFAULT_PIT_WINDOW_DURATION = 6;
    
    // SpeedHive defaults
    private static final String DEFAULT_SPEEDHIVE_MODE = "off";
    private static final String DEFAULT_SPEEDHIVE_EVENT_ID = "";
    private static final String DEFAULT_SPEEDHIVE_EVENT_NAME = "";
    private static final String DEFAULT_SPEEDHIVE_SESSION_ID = "";
    private static final String DEFAULT_SPEEDHIVE_SESSION_NAME = "";
    private static final String DEFAULT_SPEEDHIVE_CAR_NUMBER = "";
    private static final String DEFAULT_SPEEDHIVE_CAR_NAME = "";
    
    // MQTT Server defaults
    private static final int DEFAULT_MQTT_SERVER_PORT = 1883;
    private static final boolean DEFAULT_MQTT_SERVER_ENABLED = false;
    
    // SpeedHive mode constants
    public static final String SPEEDHIVE_MODE_OFF = "off";
    public static final String SPEEDHIVE_MODE_SPEEDHIVE = "speedhive";
    public static final String SPEEDHIVE_MODE_DEMO = "demo";
    
    // Special session ID for AUTO mode
    public static final String SPEEDHIVE_SESSION_AUTO = "AUTO";

    private final SharedPreferences prefs;

    public PitWindowPreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Save methods
    public void saveRaceStartTime(int hour, int minute) {
        prefs.edit()
            .putInt(KEY_RACE_START_HOUR, hour)
            .putInt(KEY_RACE_START_MINUTE, minute)
            .apply();
    }

    public void savePitWindowOpens(int minutes) {
        prefs.edit()
            .putInt(KEY_PIT_WINDOW_OPENS, minutes)
            .apply();
    }

    public void savePitWindowDuration(int minutes) {
        prefs.edit()
            .putInt(KEY_PIT_WINDOW_DURATION, minutes)
            .apply();
    }

    public void saveAll(int raceStartHour, int raceStartMinute, int pitWindowOpens, int pitWindowDuration) {
        prefs.edit()
            .putInt(KEY_RACE_START_HOUR, raceStartHour)
            .putInt(KEY_RACE_START_MINUTE, raceStartMinute)
            .putInt(KEY_PIT_WINDOW_OPENS, pitWindowOpens)
            .putInt(KEY_PIT_WINDOW_DURATION, pitWindowDuration)
            .apply();
    }

    // SpeedHive save methods
    public void saveSpeedHiveMode(String mode) {
        prefs.edit()
            .putString(KEY_SPEEDHIVE_MODE, mode)
            .apply();
    }

    public void saveSpeedHiveEventId(String eventId) {
        prefs.edit()
            .putString(KEY_SPEEDHIVE_EVENT_ID, eventId)
            .apply();
    }

    public void saveSpeedHiveSessionId(String sessionId) {
        prefs.edit()
            .putString(KEY_SPEEDHIVE_SESSION_ID, sessionId)
            .apply();
    }

    public void saveSpeedHiveCarNumber(String carNumber) {
        prefs.edit()
            .putString(KEY_SPEEDHIVE_CAR_NUMBER, carNumber)
            .apply();
    }

    public void saveAllSpeedHive(String mode, String eventId, String eventName, String sessionId, String sessionName, String carNumber, String carName) {
        prefs.edit()
            .putString(KEY_SPEEDHIVE_MODE, mode)
            .putString(KEY_SPEEDHIVE_EVENT_ID, eventId)
            .putString(KEY_SPEEDHIVE_EVENT_NAME, eventName)
            .putString(KEY_SPEEDHIVE_SESSION_ID, sessionId)
            .putString(KEY_SPEEDHIVE_SESSION_NAME, sessionName)
            .putString(KEY_SPEEDHIVE_CAR_NUMBER, carNumber)
            .putString(KEY_SPEEDHIVE_CAR_NAME, carName)
            .apply();
    }

    // Load methods
    public int getRaceStartHour() {
        return prefs.getInt(KEY_RACE_START_HOUR, DEFAULT_RACE_START_HOUR);
    }

    public int getRaceStartMinute() {
        return prefs.getInt(KEY_RACE_START_MINUTE, DEFAULT_RACE_START_MINUTE);
    }

    public int getPitWindowOpens() {
        return prefs.getInt(KEY_PIT_WINDOW_OPENS, DEFAULT_PIT_WINDOW_OPENS);
    }

    public int getPitWindowDuration() {
        return prefs.getInt(KEY_PIT_WINDOW_DURATION, DEFAULT_PIT_WINDOW_DURATION);
    }

    // SpeedHive load methods
    public String getSpeedHiveMode() {
        return prefs.getString(KEY_SPEEDHIVE_MODE, DEFAULT_SPEEDHIVE_MODE);
    }

    public String getSpeedHiveEventId() {
        return prefs.getString(KEY_SPEEDHIVE_EVENT_ID, DEFAULT_SPEEDHIVE_EVENT_ID);
    }

    public String getSpeedHiveEventName() {
        return prefs.getString(KEY_SPEEDHIVE_EVENT_NAME, DEFAULT_SPEEDHIVE_EVENT_NAME);
    }

    public String getSpeedHiveSessionId() {
        return prefs.getString(KEY_SPEEDHIVE_SESSION_ID, DEFAULT_SPEEDHIVE_SESSION_ID);
    }

    public String getSpeedHiveSessionName() {
        return prefs.getString(KEY_SPEEDHIVE_SESSION_NAME, DEFAULT_SPEEDHIVE_SESSION_NAME);
    }

    public String getSpeedHiveCarNumber() {
        return prefs.getString(KEY_SPEEDHIVE_CAR_NUMBER, DEFAULT_SPEEDHIVE_CAR_NUMBER);
    }

    public String getSpeedHiveCarName() {
        return prefs.getString(KEY_SPEEDHIVE_CAR_NAME, DEFAULT_SPEEDHIVE_CAR_NAME);
    }

    // MQTT Server methods
    public int getMqttServerPort() {
        return prefs.getInt(KEY_MQTT_SERVER_PORT, DEFAULT_MQTT_SERVER_PORT);
    }

    public boolean isMqttServerEnabled() {
        return prefs.getBoolean(KEY_MQTT_SERVER_ENABLED, DEFAULT_MQTT_SERVER_ENABLED);
    }

    public void saveMqttServerSettings(int port, boolean enabled) {
        prefs.edit()
            .putInt(KEY_MQTT_SERVER_PORT, port)
            .putBoolean(KEY_MQTT_SERVER_ENABLED, enabled)
            .apply();
    }

    public void setMqttServerEnabled(boolean enabled) {
        prefs.edit()
            .putBoolean(KEY_MQTT_SERVER_ENABLED, enabled)
            .apply();
    }

    // SpeedHive convenience methods
    public boolean isSpeedHiveEnabled() {
        String mode = getSpeedHiveMode();
        return SPEEDHIVE_MODE_SPEEDHIVE.equals(mode) || SPEEDHIVE_MODE_DEMO.equals(mode);
    }

    public boolean isSpeedHiveMode() {
        return SPEEDHIVE_MODE_SPEEDHIVE.equals(getSpeedHiveMode());
    }

    public boolean isDemoMode() {
        return SPEEDHIVE_MODE_DEMO.equals(getSpeedHiveMode());
    }

    // Convenience method to get race start time as formatted string
    public String getRaceStartTimeFormatted() {
        return String.format(Locale.getDefault(), "%02d:%02d", getRaceStartHour(), getRaceStartMinute());
    }

    // Check if settings have been configured (not using defaults)
    public boolean hasSettings() {
        return prefs.contains(KEY_RACE_START_HOUR);
    }

    // Clear all settings (for testing or reset)
    public void clear() {
        prefs.edit().clear().apply();
    }
}



