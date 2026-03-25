package at.semmal.pitstopper.mqtt;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import at.semmal.pitstopper.timing.PitWindowPreferences;

/**
 * Tracks per-metric telemetry alert levels (OK / WARNING / CRITICAL),
 * detects transitions, and publishes events to {@code fiesta/events}
 * and configuration to {@code fiesta/config} on the local MQTT broker.
 */
public class TelemetryAlertTracker {

    private static final String TAG = "TelemetryAlertTracker";
    private static final String TOPIC_EVENTS = "fiesta/events";

    public enum AlertLevel { OK, WARNING, CRITICAL }

    private final MqttClientManager mqtt;

    // Per-metric current levels
    private AlertLevel rpmLevel       = AlertLevel.OK;
    private AlertLevel coolantLevel   = AlertLevel.OK;
    private AlertLevel oilTempLevel   = AlertLevel.OK;
    private AlertLevel oilPresLevel   = AlertLevel.OK;
    private AlertLevel batteryLevel   = AlertLevel.OK;

    private AlertLevel overallLevel   = AlertLevel.OK;

    // Thresholds
    private float rpmWarn, rpmCrit;
    private boolean rpmAlarm;
    private float coolantWarn, coolantCrit;
    private boolean coolantAlarm;
    private float oilTempWarn, oilTempCrit;
    private boolean oilTempAlarm;
    private float oilPresWarn, oilPresCrit;
    private boolean oilPresAlarm;
    private float batteryWarn, batteryCrit;
    private boolean batteryAlarm;

    public TelemetryAlertTracker(MqttClientManager mqtt, PitWindowPreferences prefs) {
        this.mqtt = mqtt;
        loadThresholds(prefs);
    }

    public void reloadThresholds(PitWindowPreferences prefs) {
        loadThresholds(prefs);
    }

    private void loadThresholds(PitWindowPreferences prefs) {
        rpmWarn      = prefs.getRpmWarn();
        rpmCrit      = prefs.getRpmCrit();
        rpmAlarm     = prefs.isRpmAlarm();
        coolantWarn  = prefs.getCoolantWarn();
        coolantCrit  = prefs.getCoolantCrit();
        coolantAlarm = prefs.isCoolantAlarm();
        oilTempWarn  = prefs.getOilTempWarn();
        oilTempCrit  = prefs.getOilTempCrit();
        oilTempAlarm = prefs.isOilTempAlarm();
        oilPresWarn  = prefs.getOilPresWarn();
        oilPresCrit  = prefs.getOilPresCrit();
        oilPresAlarm = prefs.isOilPresAlarm();
        batteryWarn  = prefs.getBatteryWarn();
        batteryCrit  = prefs.getBatteryCrit();
        batteryAlarm = prefs.isBatteryAlarm();
    }

    // --- Public update methods (called from MainActivity handlers) ---

    public void updateRpm(int rpm) {
        AlertLevel newLevel = evaluateHigh(rpm, rpmWarn, rpmCrit, rpmAlarm);
        checkTransition("rpm", rpmLevel, newLevel, rpm,
                newLevel == AlertLevel.CRITICAL ? rpmCrit : rpmWarn);
        rpmLevel = newLevel;
    }

    public void updateCoolant(int coolantC) {
        AlertLevel newLevel = evaluateHigh(coolantC, coolantWarn, coolantCrit, coolantAlarm);
        checkTransition("coolant", coolantLevel, newLevel, coolantC,
                newLevel == AlertLevel.CRITICAL ? coolantCrit : coolantWarn);
        coolantLevel = newLevel;
    }

    public void updateOilTemp(int oilTemp) {
        AlertLevel newLevel = evaluateHigh(oilTemp, oilTempWarn, oilTempCrit, oilTempAlarm);
        checkTransition("oil_temp", oilTempLevel, newLevel, oilTemp,
                newLevel == AlertLevel.CRITICAL ? oilTempCrit : oilTempWarn);
        oilTempLevel = newLevel;
    }

    public void updateOilPres(float oilPres) {
        AlertLevel newLevel = evaluateLow(oilPres, oilPresWarn, oilPresCrit, oilPresAlarm);
        checkTransition("oil_pres", oilPresLevel, newLevel, oilPres,
                newLevel == AlertLevel.CRITICAL ? oilPresCrit : oilPresWarn);
        oilPresLevel = newLevel;
    }

    public void updateBattery(float batteryV) {
        AlertLevel newLevel = evaluateLow(batteryV, batteryWarn, batteryCrit, batteryAlarm);
        checkTransition("battery", batteryLevel, newLevel, batteryV,
                newLevel == AlertLevel.CRITICAL ? batteryCrit : batteryWarn);
        batteryLevel = newLevel;
    }

    // --- Chat event ---

    public void publishChatEvent(String from, String text, boolean isNotification, boolean isAlert) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "chat");
            json.put("from", from);
            json.put("text", text);
            json.put("isNotification", isNotification);
            json.put("isAlert", isAlert);
            json.put("ts", System.currentTimeMillis());
            publish(TOPIC_EVENTS, json);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to build chat event JSON", e);
        }
    }

    // --- Internal helpers ---

    private void checkTransition(String metric, AlertLevel oldLevel, AlertLevel newLevel,
                                 float value, float threshold) {
        if (oldLevel == newLevel) return;

        // Publish per-metric event
        publishMetricEvent(metric, oldLevel, newLevel, value, threshold);

        // Recompute overall (use newLevel since the field hasn't been updated yet)
        AlertLevel newOverall = worstOf(
                "rpm".equals(metric) ? newLevel : rpmLevel,
                "coolant".equals(metric) ? newLevel : coolantLevel,
                "oil_temp".equals(metric) ? newLevel : oilTempLevel,
                "oil_pres".equals(metric) ? newLevel : oilPresLevel,
                "battery".equals(metric) ? newLevel : batteryLevel);

        if (newOverall != overallLevel) {
            publishOverallEvent(overallLevel, newOverall);
            overallLevel = newOverall;
        }
    }

    private void publishMetricEvent(String metric, AlertLevel oldLevel, AlertLevel newLevel,
                                    float value, float threshold) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "telemetry_alert");
            json.put("metric", metric);

            if (newLevel.ordinal() > oldLevel.ordinal()) {
                // Escalating: entered warning or critical
                json.put("transition", "entered");
                json.put("level", newLevel.name().toLowerCase());
            } else {
                // De-escalating: left the old level
                json.put("transition", "left");
                json.put("level", oldLevel.name().toLowerCase());
            }

            json.put("value", value);
            json.put("threshold", threshold);
            json.put("ts", System.currentTimeMillis());
            publish(TOPIC_EVENTS, json);
            Log.i(TAG, metric + " " + json.optString("transition") + " " + json.optString("level"));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to build metric event JSON", e);
        }
    }

    private void publishOverallEvent(AlertLevel previous, AlertLevel current) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "overall_status");
            json.put("status", current.name().toLowerCase());
            json.put("previous", previous.name().toLowerCase());
            json.put("ts", System.currentTimeMillis());
            publish(TOPIC_EVENTS, json);
            Log.i(TAG, "Overall status: " + previous.name() + " -> " + current.name());
        } catch (JSONException e) {
            Log.w(TAG, "Failed to build overall event JSON", e);
        }
    }

    private void publish(String topic, JSONObject json) {
        if (mqtt.isConnected()) {
            mqtt.publish(topic, json.toString().getBytes());
        }
    }

    private static AlertLevel evaluateHigh(float value, float warn, float crit, boolean alarmEnabled) {
        if (!alarmEnabled) return AlertLevel.OK;
        if (value >= crit) return AlertLevel.CRITICAL;
        if (value >= warn) return AlertLevel.WARNING;
        return AlertLevel.OK;
    }

    private static AlertLevel evaluateLow(float value, float warn, float crit, boolean alarmEnabled) {
        if (!alarmEnabled) return AlertLevel.OK;
        if (value <= crit) return AlertLevel.CRITICAL;
        if (value <= warn) return AlertLevel.WARNING;
        return AlertLevel.OK;
    }

    private static AlertLevel worstOf(AlertLevel... levels) {
        AlertLevel worst = AlertLevel.OK;
        for (AlertLevel l : levels) {
            if (l.ordinal() > worst.ordinal()) worst = l;
        }
        return worst;
    }
}
