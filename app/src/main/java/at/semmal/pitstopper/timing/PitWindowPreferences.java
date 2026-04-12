package at.semmal.pitstopper.timing;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

import at.semmal.pitstopper.ui.TelemetryLayout;
import at.semmal.pitstopper.ui.TelemetrySensor;

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
    
    // MQTT remote broker settings
    private static final String KEY_MQTT_HOST = "mqtt_host";
    private static final String KEY_MQTT_PORT = "mqtt_port";
    private static final String KEY_MQTT_ENABLED = "mqtt_enabled";

    // External (public) MQTT broker settings
    private static final String KEY_EXT_MQTT_HOST = "ext_mqtt_host";
    private static final String KEY_EXT_MQTT_PORT = "ext_mqtt_port";
    private static final String KEY_EXT_MQTT_ENABLED = "ext_mqtt_enabled";

    // WiFi binding — null/absent means "use system WiFi" (no binding)
    private static final String KEY_PREFERRED_WIFI_SSID = "preferred_wifi_ssid";

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
    
    // Pit stop duration
    private static final String KEY_MIN_PIT_STOP_SECONDS = "min_pit_stop_seconds";
    private static final int DEFAULT_MIN_PIT_STOP_SECONDS = 60;

    // External session (team sync)
    private static final String KEY_SESSION_ID = "external_session_id";
    private static final String KEY_DEVICE_LABEL = "device_label";

    // Brightness (0-100)
    private static final String KEY_BRIGHTNESS = "brightness";
    private static final int DEFAULT_BRIGHTNESS = 100;

    // Telemetry alarm thresholds
    private static final String KEY_RPM_WARN = "rpm_warn";
    private static final String KEY_RPM_CRIT = "rpm_crit";
    private static final String KEY_RPM_ALARM = "rpm_alarm_enabled";
    private static final String KEY_COOLANT_WARN = "coolant_warn";
    private static final String KEY_COOLANT_CRIT = "coolant_crit";
    private static final String KEY_COOLANT_ALARM = "coolant_alarm_enabled";
    private static final String KEY_OIL_TEMP_WARN = "oil_temp_warn";
    private static final String KEY_OIL_TEMP_CRIT = "oil_temp_crit";
    private static final String KEY_OIL_TEMP_ALARM = "oil_temp_alarm_enabled";
    private static final String KEY_OIL_PRES_WARN = "oil_pres_warn";
    private static final String KEY_OIL_PRES_CRIT = "oil_pres_crit";
    private static final String KEY_OIL_PRES_ALARM = "oil_pres_alarm_enabled";
    private static final String KEY_BATTERY_WARN = "battery_warn";
    private static final String KEY_BATTERY_CRIT = "battery_crit";
    private static final String KEY_BATTERY_ALARM = "battery_alarm_enabled";

    private static final String KEY_TELEMETRY_LAYOUT = "telemetry_layout";
    private static final String KEY_SLOT_PREFIX = "telemetry_slot_";

    // Default slot assignments per layout (sensor names matching TelemetrySensor enum)
    private static final String[] DEFAULT_SLOTS_1_2_4 = {
        "RPM", "SPEED", "THROTTLE_BRAKE", "COOLANT", "OIL_TEMP", "OIL_PRES", "BATTERY"
    };
    private static final String[] DEFAULT_SLOTS_2_4 = {
        "RPM", "THROTTLE_BRAKE", "COOLANT", "OIL_TEMP", "OIL_PRES", "BATTERY"
    };

    private static final int DEFAULT_RPM_WARN = 5500;
    private static final int DEFAULT_RPM_CRIT = 6500;
    private static final int DEFAULT_COOLANT_WARN = 100;
    private static final int DEFAULT_COOLANT_CRIT = 110;
    private static final int DEFAULT_OIL_TEMP_WARN = 110;
    private static final int DEFAULT_OIL_TEMP_CRIT = 120;
    private static final float DEFAULT_OIL_PRES_WARN = 2.0f;
    private static final float DEFAULT_OIL_PRES_CRIT = 1.0f;
    private static final float DEFAULT_BATTERY_WARN = 12.5f;
    private static final float DEFAULT_BATTERY_CRIT = 11.5f;
    private static final boolean DEFAULT_ALARM_ENABLED = true;

    private static final String DEFAULT_MQTT_HOST = "broker";
    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final boolean DEFAULT_MQTT_ENABLED = false;

    private static final String DEFAULT_EXT_MQTT_HOST = "broker.hivemq.com";
    private static final int DEFAULT_EXT_MQTT_PORT = 1883;
    private static final boolean DEFAULT_EXT_MQTT_ENABLED = false;
    
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

    // MQTT remote broker methods
    public String getMqttHost() {
        return prefs.getString(KEY_MQTT_HOST, DEFAULT_MQTT_HOST);
    }

    public int getMqttPort() {
        return prefs.getInt(KEY_MQTT_PORT, DEFAULT_MQTT_PORT);
    }

    public boolean isMqttEnabled() {
        return prefs.getBoolean(KEY_MQTT_ENABLED, DEFAULT_MQTT_ENABLED);
    }

    public void saveMqttSettings(String host, int port, boolean enabled) {
        prefs.edit()
            .putString(KEY_MQTT_HOST, host)
            .putInt(KEY_MQTT_PORT, port)
            .putBoolean(KEY_MQTT_ENABLED, enabled)
            .apply();
    }

    // External (public) MQTT broker methods
    public String getExtMqttHost() {
        return prefs.getString(KEY_EXT_MQTT_HOST, DEFAULT_EXT_MQTT_HOST);
    }

    public int getExtMqttPort() {
        return prefs.getInt(KEY_EXT_MQTT_PORT, DEFAULT_EXT_MQTT_PORT);
    }

    public boolean isExtMqttEnabled() {
        return prefs.getBoolean(KEY_EXT_MQTT_ENABLED, DEFAULT_EXT_MQTT_ENABLED);
    }

    public void saveExtMqttSettings(String host, int port, boolean enabled) {
        prefs.edit()
            .putString(KEY_EXT_MQTT_HOST, host)
            .putInt(KEY_EXT_MQTT_PORT, port)
            .putBoolean(KEY_EXT_MQTT_ENABLED, enabled)
            .apply();
    }

    /** Returns the preferred WiFi SSID for MQTT binding, or null if disabled. */
    public String getPreferredWifiSsid() {
        return prefs.getString(KEY_PREFERRED_WIFI_SSID, null);
    }

    /** Save preferred WiFi SSID. Pass null to disable WiFi binding. */
    public void savePreferredWifiSsid(String ssid) {
        if (ssid == null) {
            prefs.edit().remove(KEY_PREFERRED_WIFI_SSID).apply();
        } else {
            prefs.edit().putString(KEY_PREFERRED_WIFI_SSID, ssid).apply();
        }
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

    public int getMinPitStopSeconds() {
        return prefs.getInt(KEY_MIN_PIT_STOP_SECONDS, DEFAULT_MIN_PIT_STOP_SECONDS);
    }

    public void saveMinPitStopSeconds(int seconds) {
        prefs.edit().putInt(KEY_MIN_PIT_STOP_SECONDS, seconds).apply();
    }

    public String getSessionId() {
        return prefs.getString(KEY_SESSION_ID, null);
    }

    public void saveSessionId(String sessionId) {
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply();
    }

    /** Generates a new random UUID, persists it, and returns it. */
    public String generateAndSaveNewSessionId() {
        String id = java.util.UUID.randomUUID().toString();
        saveSessionId(id);
        return id;
    }

    public String getDeviceLabel() {
        return prefs.getString(KEY_DEVICE_LABEL, null);
    }

    public void saveDeviceLabel(String label) {
        prefs.edit().putString(KEY_DEVICE_LABEL, label).apply();
    }

    // --- Brightness ---

    public int getBrightness() {
        return prefs.getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS);
    }

    public void saveBrightness(int brightness) {
        prefs.edit().putInt(KEY_BRIGHTNESS, brightness).apply();
    }

    // --- Telemetry alarm thresholds ---

    public int getRpmWarn()           { return prefs.getInt(KEY_RPM_WARN, DEFAULT_RPM_WARN); }
    public int getRpmCrit()           { return prefs.getInt(KEY_RPM_CRIT, DEFAULT_RPM_CRIT); }
    public boolean isRpmAlarm()       { return prefs.getBoolean(KEY_RPM_ALARM, DEFAULT_ALARM_ENABLED); }

    public int getCoolantWarn()       { return prefs.getInt(KEY_COOLANT_WARN, DEFAULT_COOLANT_WARN); }
    public int getCoolantCrit()       { return prefs.getInt(KEY_COOLANT_CRIT, DEFAULT_COOLANT_CRIT); }
    public boolean isCoolantAlarm()   { return prefs.getBoolean(KEY_COOLANT_ALARM, DEFAULT_ALARM_ENABLED); }

    public int getOilTempWarn()       { return prefs.getInt(KEY_OIL_TEMP_WARN, DEFAULT_OIL_TEMP_WARN); }
    public int getOilTempCrit()       { return prefs.getInt(KEY_OIL_TEMP_CRIT, DEFAULT_OIL_TEMP_CRIT); }
    public boolean isOilTempAlarm()   { return prefs.getBoolean(KEY_OIL_TEMP_ALARM, DEFAULT_ALARM_ENABLED); }

    public float getOilPresWarn()     { return prefs.getFloat(KEY_OIL_PRES_WARN, DEFAULT_OIL_PRES_WARN); }
    public float getOilPresCrit()     { return prefs.getFloat(KEY_OIL_PRES_CRIT, DEFAULT_OIL_PRES_CRIT); }
    public boolean isOilPresAlarm()   { return prefs.getBoolean(KEY_OIL_PRES_ALARM, DEFAULT_ALARM_ENABLED); }

    public float getBatteryWarn()     { return prefs.getFloat(KEY_BATTERY_WARN, DEFAULT_BATTERY_WARN); }
    public float getBatteryCrit()     { return prefs.getFloat(KEY_BATTERY_CRIT, DEFAULT_BATTERY_CRIT); }
    public boolean isBatteryAlarm()   { return prefs.getBoolean(KEY_BATTERY_ALARM, DEFAULT_ALARM_ENABLED); }

    public TelemetryLayout getTelemetryLayout() {
        return TelemetryLayout.fromKey(prefs.getString(KEY_TELEMETRY_LAYOUT, TelemetryLayout.LAYOUT_1_2_4.key));
    }

    public TelemetrySensor[] getSlotSensors(TelemetryLayout layout) {
        String[] defaults = layout == TelemetryLayout.LAYOUT_2_4 ? DEFAULT_SLOTS_2_4 : DEFAULT_SLOTS_1_2_4;
        TelemetrySensor[] result = new TelemetrySensor[layout.slotCount];
        for (int i = 0; i < result.length; i++) {
            String key = KEY_SLOT_PREFIX + layout.key + "_" + i;
            result[i] = TelemetrySensor.fromString(prefs.getString(key, defaults[i]));
        }
        return result;
    }

    public void saveLayoutConfig(TelemetryLayout layout, TelemetrySensor[] sensors) {
        SharedPreferences.Editor editor = prefs.edit()
            .putString(KEY_TELEMETRY_LAYOUT, layout.key);
        for (int i = 0; i < sensors.length; i++) {
            editor.putString(KEY_SLOT_PREFIX + layout.key + "_" + i, sensors[i].name());
        }
        editor.apply();
    }

    public void saveTelemetryAlarms(
            int rpmWarn, int rpmCrit, boolean rpmAlarm,
            int coolantWarn, int coolantCrit, boolean coolantAlarm,
            int oilTempWarn, int oilTempCrit, boolean oilTempAlarm,
            float oilPresWarn, float oilPresCrit, boolean oilPresAlarm,
            float batteryWarn, float batteryCrit, boolean batteryAlarm) {
        prefs.edit()
            .putInt(KEY_RPM_WARN, rpmWarn)
            .putInt(KEY_RPM_CRIT, rpmCrit)
            .putBoolean(KEY_RPM_ALARM, rpmAlarm)
            .putInt(KEY_COOLANT_WARN, coolantWarn)
            .putInt(KEY_COOLANT_CRIT, coolantCrit)
            .putBoolean(KEY_COOLANT_ALARM, coolantAlarm)
            .putInt(KEY_OIL_TEMP_WARN, oilTempWarn)
            .putInt(KEY_OIL_TEMP_CRIT, oilTempCrit)
            .putBoolean(KEY_OIL_TEMP_ALARM, oilTempAlarm)
            .putFloat(KEY_OIL_PRES_WARN, oilPresWarn)
            .putFloat(KEY_OIL_PRES_CRIT, oilPresCrit)
            .putBoolean(KEY_OIL_PRES_ALARM, oilPresAlarm)
            .putFloat(KEY_BATTERY_WARN, batteryWarn)
            .putFloat(KEY_BATTERY_CRIT, batteryCrit)
            .putBoolean(KEY_BATTERY_ALARM, batteryAlarm)
            .apply();
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



