package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
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
        } else {
            if (preferences.isMqttEnabled()) {
                Log.i(TAG, "Auto-connecting to MQTT broker on startup");
                mqttClientManager.connect(preferences.getMqttHost(), preferences.getMqttPort());
            }
            // No WiFi binding — use system-level NetworkCallback for reconnect-on-network-available.
            if (preferences.isMqttEnabled()) {
                registerNetworkReconnectCallback(preferences);
            }
        }

        String sessionId = preferences.getSessionId();
        if (sessionId != null && preferences.isExtMqttEnabled()) {
            Log.i(TAG, "Restoring external session: " + sessionId.substring(0, 8) + "...");
            externalSessionManager.connect(sessionId, preferences.getExtMqttHost(), preferences.getExtMqttPort());
        }
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
}
