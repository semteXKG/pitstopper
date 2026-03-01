package at.semmal.pitstopper;

import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Externalized MQTT client component. Holds connection state and notifies listeners.
 * Designed to be extended with publish/subscribe for future event retrieval.
 */
public class MqttClientManager {

    private static final String TAG = "MqttClientManager";

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        FAILING
    }

    public interface StateListener {
        void onStateChanged(State state, String error);
    }

    private Mqtt3AsyncClient client;
    private State currentState = State.DISCONNECTED;
    private String lastError = null;

    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();

    public void addStateListener(StateListener listener) {
        listeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        listeners.remove(listener);
    }

    public State getState() {
        return currentState;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isConnected() {
        return currentState == State.CONNECTED;
    }

    public void connect(String host, int port) {
        if (currentState == State.CONNECTING || currentState == State.CONNECTED) {
            Log.d(TAG, "Already connecting or connected — ignoring connect()");
            return;
        }

        Log.i(TAG, "Connecting to MQTT broker at " + host + ":" + port);
        setState(State.CONNECTING, null);

        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception ignored) {
            }
        }

        client = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(host)
                .serverPort(port)
                .automaticReconnect()
                    .initialDelay(3, java.util.concurrent.TimeUnit.SECONDS)
                    .maxDelay(30, java.util.concurrent.TimeUnit.SECONDS)
                    .applyAutomaticReconnect()
                .addConnectedListener((MqttClientConnectedContext ctx) -> {
                    Log.i(TAG, "MQTT connected");
                    setState(State.CONNECTED, null);
                })
                .addDisconnectedListener((MqttClientDisconnectedContext ctx) -> {
                    Throwable cause = ctx.getCause();
                    if (ctx.getReconnector().isReconnect()) {
                        Log.w(TAG, "MQTT disconnected, will reconnect: " + cause.getMessage());
                        setState(State.CONNECTING, null);
                    } else {
                        Log.i(TAG, "MQTT disconnected (not reconnecting)");
                        setState(State.DISCONNECTED, null);
                    }
                })
                .buildAsync();

        client.connect()
                .whenComplete((ack, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "MQTT connect failed: " + throwable.getMessage());
                        setState(State.FAILING, throwable.getMessage());
                    }
                    // On success, connectedListener already fired
                });
    }

    public void disconnect() {
        if (client == null) {
            setState(State.DISCONNECTED, null);
            return;
        }
        Log.i(TAG, "Disconnecting from MQTT broker");
        client.disconnect()
                .whenComplete((v, throwable) -> {
                    client = null;
                    setState(State.DISCONNECTED, null);
                });
    }

    // --- Future extension points ---

    /**
     * Publish a message to a topic. Call only when CONNECTED.
     */
    public void publish(String topic, byte[] payload) {
        if (client == null || currentState != State.CONNECTED) {
            Log.w(TAG, "Cannot publish: not connected");
            return;
        }
        client.publishWith()
                .topic(topic)
                .payload(payload)
                .send()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Publish failed: " + throwable.getMessage());
                    }
                });
    }

    /**
     * Subscribe to a topic with a message callback. Call only when CONNECTED.
     */
    public void subscribe(String topic, java.util.function.Consumer<byte[]> onMessage) {
        if (client == null || currentState != State.CONNECTED) {
            Log.w(TAG, "Cannot subscribe: not connected");
            return;
        }
        client.subscribeWith()
                .topicFilter(topic)
                .callback(publish -> {
                    if (publish.getPayload().isPresent()) {
                        byte[] bytes = new byte[publish.getPayload().get().remaining()];
                        publish.getPayload().get().get(bytes);
                        onMessage.accept(bytes);
                    }
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Subscribe failed for " + topic + ": " + throwable.getMessage());
                    }
                });
    }

    private void setState(State newState, String error) {
        currentState = newState;
        lastError = error;
        for (StateListener l : listeners) {
            l.onStateChanged(newState, error);
        }
    }
}
