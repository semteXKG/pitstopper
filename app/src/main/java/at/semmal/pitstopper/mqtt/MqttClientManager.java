package at.semmal.pitstopper.mqtt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Externalized MQTT client component. Holds connection state and notifies listeners.
 *
 * Reconnection is handled by HiveMQ's automatic reconnect (3s → 30s backoff).
 * Additionally, if the initial connection fails (FAILING state), a 5-second auto-retry
 * is scheduled so the app recovers without user interaction.
 *
 * Keep-alive is set to 15 seconds so dead connections are detected within ~30 seconds.
 * Socket connect timeout is 8 seconds to fail fast on unreachable brokers.
 */
public class MqttClientManager {

    private static final String TAG = "MqttClientManager";

    /** Auto-retry delay after a FAILING state (initial connect failed). */
    private static final long FAILING_RETRY_DELAY_MS = 5_000;

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
    private volatile boolean disconnecting = false;

    private String lastHost = null;
    private int lastPort = 1883;

    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private final Runnable retryRunnable = () -> {
        if (currentState == State.FAILING && lastHost != null) {
            Log.i(TAG, "Auto-retrying MQTT connection after FAILING state");
            connectInternal(lastHost, lastPort);
        }
    };

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
        connectInternal(host, port);
    }

    /**
     * Reconnect using the last known host and port. No-op if no previous connection
     * was attempted or if already connecting/connected.
     */
    public void reconnect() {
        if (lastHost == null) return;
        if (currentState == State.CONNECTING || currentState == State.CONNECTED) return;
        Log.i(TAG, "Reconnecting to last known broker: " + lastHost + ":" + lastPort);
        connectInternal(lastHost, lastPort);
    }

    private void connectInternal(String host, int port) {
        retryHandler.removeCallbacks(retryRunnable);

        lastHost = host;
        lastPort = port;
        disconnecting = false;
        setState(State.CONNECTING, null);

        Log.i(TAG, "Connecting to MQTT broker at " + host + ":" + port);

        if (client != null) {
            try { client.disconnect(); } catch (Exception ignored) {}
        }

        client = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(host)
                .serverPort(port)
                .automaticReconnect()
                    .initialDelay(3, TimeUnit.SECONDS)
                    .maxDelay(30, TimeUnit.SECONDS)
                    .applyAutomaticReconnect()
                .addConnectedListener((MqttClientConnectedContext ctx) -> {
                    Log.i(TAG, "MQTT connected to " + host + ":" + port);
                    retryHandler.removeCallbacks(retryRunnable);
                    setState(State.CONNECTED, null);
                })
                .addDisconnectedListener((MqttClientDisconnectedContext ctx) -> {
                    Throwable cause = ctx.getCause();
                    if (ctx.getReconnector().isReconnect()) {
                        if (disconnecting) {
                            // User explicitly disconnected — cancel the pending auto-reconnect.
                            ctx.getReconnector().reconnect(false);
                            return;
                        }
                        Log.w(TAG, "MQTT disconnected, will reconnect: "
                                + (cause != null ? cause.getMessage() : "unknown"));
                        setState(State.CONNECTING, null);
                    } else {
                        Log.i(TAG, "MQTT disconnected (not reconnecting)");
                        setState(State.DISCONNECTED, null);
                    }
                })
                .buildAsync();

        client.connectWith()
                .keepAlive(15)           // detect dead connections within ~30 seconds
                .send()
                .whenComplete((ack, throwable) -> {
                    if (throwable != null) {
                        // Only set FAILING if HiveMQ is not already scheduling a reconnect.
                        // The disconnectedListener fires separately with isReconnect()=true
                        // and will set state to CONNECTING — so we only reach here on a
                        // hard failure (e.g. host not found, no route to host).
                        if (currentState != State.CONNECTING && currentState != State.CONNECTED) {
                            Log.e(TAG, "MQTT initial connect failed: " + throwable.getMessage());
                            setState(State.FAILING, throwable.getMessage());
                            retryHandler.postDelayed(retryRunnable, FAILING_RETRY_DELAY_MS);
                        }
                    }
                });
    }

    public void disconnect() {
        retryHandler.removeCallbacks(retryRunnable);
        if (client == null) {
            setState(State.DISCONNECTED, null);
            return;
        }
        Log.i(TAG, "Disconnecting from MQTT broker");
        disconnecting = true;
        client.disconnect()
                .whenComplete((v, throwable) -> {
                    disconnecting = false;
                    client = null;
                    setState(State.DISCONNECTED, null);
                });
    }

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
                .qos(MqttQos.AT_MOST_ONCE)
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
                .qos(MqttQos.AT_MOST_ONCE)
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

