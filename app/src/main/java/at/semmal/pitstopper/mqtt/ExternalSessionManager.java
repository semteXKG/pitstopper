package at.semmal.pitstopper.mqtt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages a bidirectional MQTT session over a public broker (broker.hivemq.com).
 * The session ID is a UUID used as the topic suffix — security through obscurity.
 *
 * Topic: pitstopper/{sessionId}/events
 * Payload: {"event":"PIT_PRESSED","from":"Driver","ts":1234567890}
 */
public class ExternalSessionManager {

    private static final String TAG = "ExternalSessionManager";
    private static final String PUBLIC_BROKER = "broker.hivemq.com";
    private static final int PUBLIC_BROKER_PORT = 1883;
    private static final String TOPIC_PREFIX = "pitstopper/";
    private static final String TOPIC_SUFFIX = "/events";

    public interface SessionEventListener {
        /** Called on the main thread when an event arrives from another device. */
        void onEvent(String eventType, String from, long ts);
    }

    private final MqttClientManager mqtt = new MqttClientManager();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String sessionId;
    private SessionEventListener eventListener;

    /** Connect to the public broker and subscribe to the session topic. */
    public void connect(String sessionId) {
        this.sessionId = sessionId;
        mqtt.addStateListener((state, error) -> {
            if (state == MqttClientManager.State.CONNECTED) {
                subscribeToTopic();
            }
        });
        mqtt.connect(PUBLIC_BROKER, PUBLIC_BROKER_PORT);
    }

    /** Disconnect and clean up. */
    public void disconnect() {
        mqtt.disconnect();
        sessionId = null;
    }

    public boolean isConnected() {
        return mqtt.isConnected();
    }

    public MqttClientManager.State getState() {
        return mqtt.getState();
    }

    public void addStateListener(MqttClientManager.StateListener listener) {
        mqtt.addStateListener(listener);
    }

    public void removeStateListener(MqttClientManager.StateListener listener) {
        mqtt.removeStateListener(listener);
    }

    public void setEventListener(SessionEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Publish an event to the session topic.
     *
     * @param eventType e.g. "PIT_PRESSED", "ALARM"
     * @param from      human-readable device label, e.g. "Driver"
     */
    public void publishEvent(String eventType, String from) {
        if (sessionId == null) {
            Log.w(TAG, "Cannot publish: no session ID");
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("event", eventType);
            json.put("from", from);
            json.put("ts", System.currentTimeMillis());
            mqtt.publish(buildTopic(), json.toString().getBytes());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build event payload", e);
        }
    }

    private void subscribeToTopic() {
        if (sessionId == null) return;
        mqtt.subscribe(buildTopic(), payload -> {
            try {
                JSONObject json = new JSONObject(new String(payload));
                String event = json.optString("event", "");
                String from  = json.optString("from", "?");
                long   ts    = json.optLong("ts", 0);
                mainHandler.post(() -> {
                    if (eventListener != null) {
                        eventListener.onEvent(event, from, ts);
                    }
                });
            } catch (JSONException e) {
                Log.w(TAG, "Malformed session event payload");
            }
        });
        Log.i(TAG, "Subscribed to " + buildTopic());
    }

    private String buildTopic() {
        return TOPIC_PREFIX + sessionId + TOPIC_SUFFIX;
    }
}
