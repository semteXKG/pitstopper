package at.semmal.pitstopper.mqtt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages a bidirectional MQTT session over a public broker (broker.hivemq.com).
 * The session ID is a UUID used as the topic prefix — security through obscurity.
 *
 * Topic: {sessionId}/fiesta/chat
 * Payload: {"from":"pitstopper","text":"<message>","isNotification":<bool>,"isAlert":<bool>}
 */
public class ExternalSessionManager {

    private static final String TAG = "ExternalSessionManager";
    private static final String DEFAULT_BROKER = "broker.hivemq.com";
    private static final int DEFAULT_PORT = 1883;
    private static final String DEVICE_ID = "pitstopper";

    public interface SessionEventListener {
        /** Called on the main thread when a chat message arrives from another device. */
        void onMessage(String from, String text, boolean isNotification, boolean isAlert);
    }

    private final MqttClientManager mqtt = new MqttClientManager();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String sessionId;
    private SessionEventListener eventListener;

    /** Connect to the public broker using saved settings and subscribe to the session topic. */
    public void connect(String sessionId) {
        connect(sessionId, DEFAULT_BROKER, DEFAULT_PORT);
    }

    /** Connect to a specific broker and subscribe to the session topic. */
    public void connect(String sessionId, String host, int port) {
        this.sessionId = sessionId;
        mqtt.addStateListener((state, error) -> {
            if (state == MqttClientManager.State.CONNECTED) {
                subscribeToTopic();
            }
        });
        mqtt.connect(host, port);
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
     * Publish a plain chat message (no notification/alert flags).
     *
     * @param text message text
     */
    public void publishMessage(String text) {
        publishMessage(text, false, false);
    }

    /**
     * Publish a chat message to the session topic.
     *
     * @param text           message text, e.g. "PIT" or "FCK"
     * @param isNotification true for informational events (e.g. "PIT")
     * @param isAlert        true for urgent alerts (e.g. "FCK", "ALARM")
     */
    public void publishMessage(String text, boolean isNotification, boolean isAlert) {
        if (sessionId == null) {
            Log.w(TAG, "Cannot publish: no session ID");
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("from", DEVICE_ID);
            json.put("text", text);
            json.put("isNotification", isNotification);
            json.put("isAlert", isAlert);
            mqtt.publish(buildTopic(), json.toString().getBytes());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build chat payload", e);
        }
    }

    private void subscribeToTopic() {
        if (sessionId == null) return;
        mqtt.subscribe(buildTopic(), payload -> {
            try {
                JSONObject json = new JSONObject(new String(payload));
                String from = json.optString("from", "?");
                String text = json.optString("text", "");
                boolean isNotification = json.optBoolean("isNotification", false);
                boolean isAlert = json.optBoolean("isAlert", false);
                if (DEVICE_ID.equals(from)) return; // ignore own messages
                mainHandler.post(() -> {
                    if (eventListener != null) {
                        eventListener.onMessage(from, text, isNotification, isAlert);
                    }
                });
            } catch (JSONException e) {
                Log.w(TAG, "Malformed chat payload");
            }
        });
        Log.i(TAG, "Subscribed to " + buildTopic());
    }

    private String buildTopic() {
        return sessionId + "/fiesta/chat";
    }
}
