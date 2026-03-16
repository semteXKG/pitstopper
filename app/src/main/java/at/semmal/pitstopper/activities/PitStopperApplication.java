package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
import at.semmal.pitstopper.mqtt.LocalTcpProxy;
import at.semmal.pitstopper.mqtt.WifiNetworkManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

public class PitStopperApplication extends Application {

    private static final String TAG = "PitStopperApplication";

    private MqttClientManager mqttClientManager;
    private ExternalSessionManager externalSessionManager;
    private WifiNetworkManager wifiNetworkManager;
    private LocalTcpProxy cellularProxy;
    private ConnectivityManager.NetworkCallback cellularCallback;
    private volatile Network cellularNetwork;

    @Override
    public void onCreate() {
        super.onCreate();
        mqttClientManager = new MqttClientManager();
        externalSessionManager = new ExternalSessionManager();
        wifiNetworkManager = new WifiNetworkManager(this);

        PitWindowPreferences preferences = new PitWindowPreferences(this);
        String preferredSsid = preferences.getPreferredWifiSsid();

        if (preferredSsid != null) {
            // WiFi binding mode: proxy will start once network is BOUND.
            // MQTT connects to the proxy — do not auto-connect directly.
            Log.i(TAG, "Restoring WiFi binding to SSID: " + preferredSsid);
            setupWifiMqttCoordinator(preferences);
            wifiNetworkManager.bind(preferredSsid,
                    preferences.getMqttHost(), preferences.getMqttPort());

            // Request cellular network for SpeedHive API + external MQTT
            requestCellularNetwork(preferences);
        } else {
            if (preferences.isMqttEnabled()) {
                Log.i(TAG, "Auto-connecting to MQTT broker on startup");
                mqttClientManager.connect(preferences.getMqttHost(), preferences.getMqttPort());
            }
            // No WiFi binding — use system-level NetworkCallback for reconnect-on-network-available.
            if (preferences.isMqttEnabled()) {
                registerNetworkReconnectCallback(preferences);
            }
            // External session can connect directly when WiFi isn't bound
            String sessionId = preferences.getSessionId();
            if (sessionId != null && preferences.isExtMqttEnabled()) {
                Log.i(TAG, "Restoring external session: " + sessionId.substring(0, 8) + "...");
                externalSessionManager.connect(sessionId,
                        preferences.getExtMqttHost(), preferences.getExtMqttPort());
            }
        }
    }

    /**
     * Requests the cellular network so that SpeedHive API and external MQTT can
     * route through GSM even when WiFi (car hotspot, no internet) is the default.
     * Stores the Network for SpeedHive's HttpURLConnection and starts a proxy
     * for external MQTT if a session is configured.
     */
    private void requestCellularNetwork(PitWindowPreferences preferences) {
        cellularProxy = new LocalTcpProxy();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        NetworkRequest cellRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cellularCallback = new ConnectivityManager.NetworkCallback() {
            private volatile boolean proxyStarted = false;

            @Override
            public void onAvailable(Network network) {
                cellularNetwork = network;
                Log.i(TAG, "Cellular network available");

                // Start external MQTT proxy if session is configured
                String sid = preferences.getSessionId();
                if (sid != null && preferences.isExtMqttEnabled() && !proxyStarted) {
                    proxyStarted = true;
                    String host = preferences.getExtMqttHost();
                    int port = preferences.getExtMqttPort();
                    try {
                        cellularProxy.start(network, host, port);
                        int proxyPort = cellularProxy.getLocalPort();
                        Log.i(TAG, "Cellular proxy started on port " + proxyPort
                                + " → " + host + ":" + port);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(
                                () -> externalSessionManager.connect(sid, "127.0.0.1", proxyPort));
                    } catch (java.io.IOException e) {
                        Log.e(TAG, "Failed to start cellular proxy: " + e.getMessage());
                        proxyStarted = false;
                    }
                }
            }

            @Override
            public void onLost(Network network) {
                Log.w(TAG, "Cellular network lost");
                cellularNetwork = null;
                proxyStarted = false;
                cellularProxy.stop();
                externalSessionManager.disconnect();
            }
        };

        cm.requestNetwork(cellRequest, cellularCallback);
    }

    /**
     * Listens to WiFi binding state changes and switches the MQTT connection between
     * the local proxy (when BOUND) and the real broker (when the binding is lost/inactive).
     */
    private void setupWifiMqttCoordinator(PitWindowPreferences preferences) {
        wifiNetworkManager.addStateListener((state, ssid) -> {
            if (!preferences.isMqttEnabled()) return;
            switch (state) {
                case BOUND:
                case WRONG_NETWORK:
                    // Proxy is running in both cases — connect MQTT through it.
                    int proxyPort = wifiNetworkManager.getProxyPort();
                    Log.i(TAG, "WiFi " + state + " — connecting MQTT via proxy on port " + proxyPort);
                    mqttClientManager.disconnect();
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            () -> mqttClientManager.connect("127.0.0.1", proxyPort), 300);
                    break;
                case UNAVAILABLE:
                    Log.i(TAG, "WiFi UNAVAILABLE — falling back to direct MQTT connection");
                    mqttClientManager.disconnect();
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            () -> mqttClientManager.connect(
                                    preferences.getMqttHost(), preferences.getMqttPort()), 300);
                    break;
                case INACTIVE:
                    Log.i(TAG, "WiFi binding released — switching to direct MQTT connection");
                    mqttClientManager.disconnect();
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            () -> mqttClientManager.connect(
                                    preferences.getMqttHost(), preferences.getMqttPort()), 300);
                    break;
                default:
                    break;
            }
        });
    }

    private void registerNetworkReconnectCallback(PitWindowPreferences preferences) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                MqttClientManager.State state = mqttClientManager.getState();
                if (preferences.isMqttEnabled()
                        && state != MqttClientManager.State.CONNECTED
                        && state != MqttClientManager.State.CONNECTING) {
                    Log.i(TAG, "Network available — triggering MQTT reconnect");
                    mqttClientManager.reconnect();
                }
            }
        });
    }

    public MqttClientManager getMqttClientManager() {
        return mqttClientManager;
    }

    public ExternalSessionManager getExternalSessionManager() {
        return externalSessionManager;
    }

    public WifiNetworkManager getWifiNetworkManager() {
        return wifiNetworkManager;
    }

    /** Returns the cellular Network for routing HTTP calls, or null if unavailable. */
    public Network getCellularNetwork() {
        return cellularNetwork;
    }

    /**
     * Connect the external session through the cellular proxy.
     * Called from SessionActivity when the user connects/creates/restores a session.
     */
    public void connectExternalSession(String sessionId, String host, int port) {
        PitWindowPreferences prefs = new PitWindowPreferences(this);
        prefs.saveExtMqttSettings(host, port, true);
        if (cellularProxy != null && cellularProxy.isRunning()) {
            int proxyPort = cellularProxy.getLocalPort();
            Log.i(TAG, "Connecting external session via cellular proxy port " + proxyPort);
            externalSessionManager.connect(sessionId, "127.0.0.1", proxyPort);
        } else if (cellularNetwork != null) {
            // Have cellular network but proxy not started yet — start it
            try {
                cellularProxy.start(cellularNetwork, host, port);
                int proxyPort = cellularProxy.getLocalPort();
                Log.i(TAG, "Cellular proxy started on port " + proxyPort);
                externalSessionManager.connect(sessionId, "127.0.0.1", proxyPort);
            } catch (java.io.IOException e) {
                Log.e(TAG, "Failed to start cellular proxy: " + e.getMessage());
            }
        } else {
            // No cellular yet — request it
            Log.i(TAG, "Requesting cellular network for external session");
            requestCellularNetwork(prefs);
        }
    }

    /**
     * Disconnect the external session and stop the cellular proxy.
     */
    public void disconnectExternalSession() {
        externalSessionManager.disconnect();
        if (cellularProxy != null) {
            cellularProxy.stop();
        }
        // Don't unregister cellular callback — SpeedHive still needs it
    }
}
