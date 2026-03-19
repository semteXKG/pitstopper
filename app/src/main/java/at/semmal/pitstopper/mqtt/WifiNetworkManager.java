package at.semmal.pitstopper.mqtt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Routes MQTT traffic through a specific WiFi SSID without affecting other connections.
 *
 * Monitors WiFi connectivity using registerNetworkCallback (no system dialog).
 * When the device is connected to the target SSID, a LocalTcpProxy is started that
 * forwards MQTT to the real broker via that network's socket factory. All other traffic
 * (SpeedHive, etc.) uses the default system network (including mobile data).
 *
 * If the device is connected to a different SSID, enters WRONG_NETWORK state so the UI
 * can prompt the user to switch WiFi manually.
 */
public class WifiNetworkManager {

    private static final String TAG = "WifiNetworkManager";

    public enum State {
        INACTIVE,
        REQUESTING,
        BOUND,
        UNAVAILABLE,
        WRONG_NETWORK
    }

    public interface StateListener {
        void onStateChanged(State state, String ssid);
    }

    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();
    private final LocalTcpProxy proxy = new LocalTcpProxy();

    private State currentState = State.INACTIVE;
    private String currentSsid = null;
    private String connectedSsid = null;
    private String targetHost = null;
    private int targetPort = 1883;
    private volatile boolean proxyStarted = false;
    private volatile Network boundNetwork = null;
    private ConnectivityManager.NetworkCallback networkCallback = null;

    public WifiNetworkManager(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public State getState() { return currentState; }
    public String getCurrentSsid() { return currentSsid; }

    /** Returns the SSID the device is actually connected to (useful for WRONG_NETWORK messages). */
    public String getConnectedSsid() { return connectedSsid; }

    /**
     * Returns the local proxy port to use as the MQTT broker address, or -1 if not bound.
     * When bound, connect MQTT to "127.0.0.1" : getProxyPort() instead of the real broker.
     */
    public int getProxyPort() { return proxy.getLocalPort(); }

    /** Returns the bound WiFi Network object, or null if not bound. */
    public Network getBoundNetwork() { return boundNetwork; }

    public void addStateListener(StateListener listener) { listeners.add(listener); }
    public void removeStateListener(StateListener listener) { listeners.remove(listener); }

    /**
     * Start the proxy on the currently connected WiFi network.
     * The expectedSsid is only used for a cross-check warning — if the device is on
     * a different SSID, the proxy STILL starts, but the state is WRONG_NETWORK so the
     * UI can warn the user. If expectedSsid is null, no warning is shown.
     */
    public void bind(String expectedSsid, String brokerHost, int brokerPort) {
        releaseInternal();
        currentSsid = expectedSsid;
        targetHost = brokerHost;
        targetPort = brokerPort;
        setState(State.REQUESTING);

        Log.i(TAG, "Starting WiFi proxy (expected SSID: "
                + (expectedSsid != null ? expectedSsid : "<any>") + ")");

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                bindToNetwork(network, expectedSsid, brokerHost, brokerPort);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                if (currentState != State.BOUND && currentState != State.WRONG_NETWORK) {
                    bindToNetwork(network, expectedSsid, brokerHost, brokerPort);
                }
            }

            @Override
            public void onLost(Network network) {
                if (currentState == State.BOUND || currentState == State.WRONG_NETWORK) {
                    Log.w(TAG, "WiFi lost");
                    proxyStarted = false;
                    proxy.stop();
                    connectedSsid = null;
                    mainHandler.post(() -> setState(State.UNAVAILABLE));
                }
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);

        // Fallback: check immediately after 500ms in case callback didn't fire with SSID
        mainHandler.postDelayed(() -> {
            if (!proxyStarted && targetHost != null) {
                checkCurrentConnection(expectedSsid, brokerHost, brokerPort);
            }
        }, 500);
    }

    /** Start proxy on the given network. Always starts — warns if SSID doesn't match expected. */
    private synchronized void bindToNetwork(Network network, String expectedSsid,
                                            String brokerHost, int brokerPort) {
        if (proxyStarted) return;
        proxyStarted = true;
        boundNetwork = network;

        String ssid = readSsid(network);
        if (ssid != null) connectedSsid = ssid;

        try {
            proxy.start(network, brokerHost, brokerPort);
            Log.i(TAG, "Proxy started on " + ssid + " port " + proxy.getLocalPort());
        } catch (IOException e) {
            Log.e(TAG, "Failed to start proxy: " + e.getMessage());
            proxyStarted = false;
            mainHandler.post(() -> setState(State.UNAVAILABLE));
            return;
        }

        boolean wrongNetwork = expectedSsid != null && ssid != null
                && !ssidMatches(ssid, expectedSsid);
        if (wrongNetwork) {
            Log.w(TAG, "Proxy running but on '" + ssid + "', expected '" + expectedSsid + "'");
            mainHandler.post(() -> setState(State.WRONG_NETWORK));
        } else {
            mainHandler.post(() -> setState(State.BOUND));
        }
    }

    private void checkCurrentConnection(String expectedSsid, String brokerHost, int brokerPort) {
        if (wifiManager == null) return;
        @SuppressWarnings("deprecation")
        android.net.wifi.WifiInfo wi = wifiManager.getConnectionInfo();
        if (wi == null) return;

        connectedSsid = cleanSsid(wi.getSSID());

        for (Network net : connectivityManager.getAllNetworks()) {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(net);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i(TAG, "Fallback: found WiFi (" + connectedSsid + ")");
                bindToNetwork(net, expectedSsid, brokerHost, brokerPort);
                return;
            }
        }
    }

    /** Read the SSID from a Network via capabilities, with WifiManager fallback. */
    private String readSsid(Network network) {
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TransportInfo info = caps.getTransportInfo();
            if (info instanceof WifiInfo) {
                String raw = ((WifiInfo) info).getSSID();
                if (raw != null && !raw.equals("<unknown ssid>") && !raw.startsWith("0x")) {
                    return cleanSsid(raw);
                }
            }
        }
        if (wifiManager != null) {
            @SuppressWarnings("deprecation")
            android.net.wifi.WifiInfo wi = wifiManager.getConnectionInfo();
            if (wi != null) return cleanSsid(wi.getSSID());
        }
        return null;
    }

    private boolean ssidMatches(String fromSystem, String target) {
        if (fromSystem == null || target == null) return false;
        return target.equals(cleanSsid(fromSystem));
    }

    private String cleanSsid(String ssid) {
        if (ssid == null) return null;
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    /** Release the WiFi binding and stop the proxy. */
    public void release() {
        releaseInternal();
        connectedSsid = null;
        setState(State.INACTIVE);
    }

    private void releaseInternal() {
        proxyStarted = false;
        boundNetwork = null;
        proxy.stop();
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
            networkCallback = null;
        }
        currentSsid = null;
        targetHost = null;
    }

    private void setState(State newState) {
        currentState = newState;
        for (StateListener l : listeners) {
            l.onStateChanged(newState, currentSsid);
        }
    }
}
