package at.semmal.pitstopper.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.WifiNetworkManager;
import at.semmal.pitstopper.network.SubnetScanner;
import at.semmal.pitstopper.timing.PitWindowPreferences;

/**
 * Troubleshoot module: swipable diagnostic panel showing WiFi, MQTT,
 * topic discovery, and subnet device scanning.
 */
public class TroubleshootModule extends CenterModule {

    private static final String TAG = "TroubleshootModule";
    private static final int COLOR_OK = Color.parseColor("#4CAF50");
    private static final int COLOR_FAIL = Color.parseColor("#F44336");
    private static final int COLOR_WARN = Color.parseColor("#FF9800");
    private static final int COLOR_PROGRESS = Color.parseColor("#FFC107");
    private static final int COLOR_NEUTRAL = Color.parseColor("#9E9E9E");
    private static final int TCP_PROBE_TIMEOUT_MS = 5000;

    private final MqttClientManager mqttClientManager;
    private final WifiNetworkManager wifiNetworkManager;
    private final PitWindowPreferences preferences;
    private final WifiManager wifiManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Views
    private final TextView textWifiResult;
    private final TextView textMqttResult;
    private final TextView textTopicsStatus;
    private final TextView textTopicsList;
    private final Button buttonTopicScan;
    private final TextView textDevicesStatus;
    private final TextView textDevicesList;
    private final Button buttonDeviceScan;

    // Topic scan state — maps topic name to last-message timestamp (epoch ms)
    private Mqtt3AsyncClient topicScanClient;
    private final Map<String, Long> topicLastSeen = new LinkedHashMap<>();
    private volatile boolean topicScanRunning = false;

    // Device scan state
    private SubnetScanner subnetScanner;
    private volatile boolean deviceScanRunning = false;
    private final StringBuilder deviceListBuilder = new StringBuilder();
    private int devicesFound = 0;
    private long deviceLastUpdateTime = 0;

    // Timestamp ticker
    private final Runnable timestampTicker = new Runnable() {
        @Override
        public void run() {
            updateTimestamps();
            mainHandler.postDelayed(this, 1000);
        }
    };
    private boolean tickerRunning = false;

    // Background executor for TCP probe
    private final ExecutorService probeExecutor = Executors.newSingleThreadExecutor();

    public TroubleshootModule(Context context,
                              MqttClientManager mqttClientManager,
                              WifiNetworkManager wifiNetworkManager,
                              PitWindowPreferences preferences) {
        super(context);
        this.mqttClientManager = mqttClientManager;
        this.wifiNetworkManager = wifiNetworkManager;
        this.preferences = preferences;
        this.wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        LayoutInflater.from(context).inflate(R.layout.module_troubleshoot, this, true);

        textWifiResult = findViewById(R.id.textWifiResult);
        textMqttResult = findViewById(R.id.textMqttResult);
        textTopicsStatus = findViewById(R.id.textTopicsStatus);
        textTopicsList = findViewById(R.id.textTopicsList);
        buttonTopicScan = findViewById(R.id.buttonTopicScan);
        textDevicesStatus = findViewById(R.id.textDevicesStatus);
        textDevicesList = findViewById(R.id.textDevicesList);
        buttonDeviceScan = findViewById(R.id.buttonDeviceScan);

        findViewById(R.id.buttonRefresh).setOnClickListener(v -> runAllChecks());

        buttonTopicScan.setOnClickListener(v -> {
            if (topicScanRunning) {
                stopTopicScan();
            } else {
                startTopicScan();
            }
        });

        buttonDeviceScan.setOnClickListener(v -> {
            if (deviceScanRunning) {
                stopDeviceScan();
            } else {
                startDeviceScan();
            }
        });

        // Initial idle state
        textTopicsStatus.setText(R.string.troubleshoot_topics_idle);
        textDevicesStatus.setText(R.string.troubleshoot_devices_idle);
    }

    @Override
    public void onActivate() {
        setVisibility(View.VISIBLE);
        runAllChecks();
        startTimestampTicker();
        // Auto-start wildcard topic scan if MQTT is connected
        if (mqttClientManager.isConnected() && !topicScanRunning) {
            startTopicScan();
        }
    }

    @Override
    public void onDeactivate() {
        animate().cancel();
        setTranslationY(0);
        setVisibility(View.GONE);
        stopTimestampTicker();
        stopTopicScan();
        stopDeviceScan();
    }

    /** Run WiFi + MQTT checks (instant/fast). Does not auto-start heavy scans. */
    public void runAllChecks() {
        checkWifi();
        checkMqtt();
    }

    // ======================== WiFi Check ========================

    private void checkWifi() {
        String expectedSsid = preferences.getPreferredWifiSsid();
        String connectedSsid = wifiNetworkManager.getConnectedSsid();
        WifiNetworkManager.State wifiState = wifiNetworkManager.getState();

        if (expectedSsid == null || expectedSsid.isEmpty()) {
            if (connectedSsid != null && !connectedSsid.isEmpty()) {
                textWifiResult.setText(getContext().getString(
                        R.string.troubleshoot_wifi_connected_no_config, connectedSsid));
                textWifiResult.setTextColor(COLOR_WARN);
            } else {
                textWifiResult.setText(R.string.troubleshoot_wifi_no_config);
                textWifiResult.setTextColor(COLOR_WARN);
            }
            return;
        }

        if (connectedSsid == null || connectedSsid.isEmpty()) {
            textWifiResult.setText(R.string.troubleshoot_wifi_no_wifi);
            textWifiResult.setTextColor(COLOR_FAIL);
            return;
        }

        if (wifiState == WifiNetworkManager.State.BOUND) {
            textWifiResult.setText(getContext().getString(
                    R.string.troubleshoot_wifi_ok, connectedSsid, expectedSsid));
            textWifiResult.setTextColor(COLOR_OK);
        } else if (wifiState == WifiNetworkManager.State.WRONG_NETWORK) {
            textWifiResult.setText(getContext().getString(
                    R.string.troubleshoot_wifi_wrong, connectedSsid, expectedSsid));
            textWifiResult.setTextColor(COLOR_FAIL);
        } else {
            textWifiResult.setText(getContext().getString(
                    R.string.troubleshoot_wifi_ok, connectedSsid, expectedSsid));
            textWifiResult.setTextColor(connectedSsid.equals(expectedSsid) ? COLOR_OK : COLOR_FAIL);
        }
    }

    // ======================== MQTT Check ========================

    private void checkMqtt() {
        if (!preferences.isMqttEnabled()) {
            textMqttResult.setText(R.string.troubleshoot_mqtt_disconnected);
            textMqttResult.setTextColor(COLOR_NEUTRAL);
            return;
        }

        String host = preferences.getMqttHost();
        int port = preferences.getMqttPort();
        MqttClientManager.State state = mqttClientManager.getState();

        switch (state) {
            case CONNECTED:
                textMqttResult.setText(getContext().getString(
                        R.string.troubleshoot_mqtt_connected, host, port));
                textMqttResult.setTextColor(COLOR_OK);
                break;
            case CONNECTING:
                textMqttResult.setText(getContext().getString(
                        R.string.troubleshoot_mqtt_connecting, host, port));
                textMqttResult.setTextColor(COLOR_PROGRESS);
                runTcpProbe(host, port);
                break;
            case FAILING:
                String error = mqttClientManager.getLastError();
                if (error == null) error = "unknown";
                textMqttResult.setText(getContext().getString(
                        R.string.troubleshoot_mqtt_failing, error, host, port));
                textMqttResult.setTextColor(COLOR_FAIL);
                runTcpProbe(host, port);
                break;
            default:
                textMqttResult.setText(R.string.troubleshoot_mqtt_disconnected);
                textMqttResult.setTextColor(COLOR_NEUTRAL);
                break;
        }
    }

    /** Background TCP probe to check raw port reachability. */
    private void runTcpProbe(String host, int port) {
        probeExecutor.submit(() -> {
            boolean reachable = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), TCP_PROBE_TIMEOUT_MS);
                reachable = true;
            } catch (IOException e) {
                // Not reachable
            }
            boolean ok = reachable;
            mainHandler.post(() -> {
                CharSequence existing = textMqttResult.getText();
                String extra = getContext().getString(ok
                        ? R.string.troubleshoot_mqtt_tcp_ok
                        : R.string.troubleshoot_mqtt_tcp_fail);
                textMqttResult.setText(existing + "\n" + extra);
            });
        });
    }

    // ======================== MQTT Topic Scan ========================

    private void startTopicScan() {
        if (!mqttClientManager.isConnected()) {
            textTopicsStatus.setText(R.string.troubleshoot_topics_need_mqtt);
            textTopicsStatus.setTextColor(COLOR_FAIL);
            return;
        }

        topicScanRunning = true;
        topicLastSeen.clear();
        textTopicsList.setText("");
        buttonTopicScan.setText(R.string.troubleshoot_topics_stop);
        updateTopicCount();

        String host = preferences.getMqttHost();
        int port = preferences.getMqttPort();

        // If WiFi proxy is active, connect through the proxy instead
        String connectHost = host;
        int connectPort = port;
        if (wifiNetworkManager.getState() == WifiNetworkManager.State.BOUND
                || wifiNetworkManager.getState() == WifiNetworkManager.State.WRONG_NETWORK) {
            int proxyPort = wifiNetworkManager.getProxyPort();
            if (proxyPort > 0) {
                connectHost = "127.0.0.1";
                connectPort = proxyPort;
            }
        }

        try {
            topicScanClient = MqttClient.builder()
                    .useMqttVersion3()
                    .serverHost(connectHost)
                    .serverPort(connectPort)
                    .identifier("pitstopper-troubleshoot-" + System.currentTimeMillis())
                    .buildAsync();

            topicScanClient.connectWith()
                    .keepAlive(30)
                    .send()
                    .whenComplete((ack, throwable) -> {
                        if (throwable != null) {
                            Log.e(TAG, "Topic scan connect failed: " + throwable.getMessage());
                            mainHandler.post(() -> {
                                textTopicsStatus.setText("✗ Connect failed: " + throwable.getMessage());
                                textTopicsStatus.setTextColor(COLOR_FAIL);
                                topicScanRunning = false;
                                buttonTopicScan.setText(R.string.troubleshoot_topics_start);
                            });
                            return;
                        }

                        topicScanClient.subscribeWith()
                                .topicFilter("#")
                                .qos(MqttQos.AT_MOST_ONCE)
                                .callback(publish -> {
                                    String topic = publish.getTopic().toString();
                                    topicLastSeen.put(topic, System.currentTimeMillis());
                                    mainHandler.post(this::updateTopicCount);
                                })
                                .send()
                                .whenComplete((subAck, subThrow) -> {
                                    if (subThrow != null) {
                                        Log.e(TAG, "Topic scan subscribe failed: "
                                                + subThrow.getMessage());
                                        mainHandler.post(() -> {
                                            textTopicsStatus.setText(
                                                    "✗ Subscribe failed: " + subThrow.getMessage());
                                            textTopicsStatus.setTextColor(COLOR_FAIL);
                                            topicScanRunning = false;
                                            buttonTopicScan.setText(
                                                    R.string.troubleshoot_topics_start);
                                        });
                                    }
                                });
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to create topic scan client: " + e.getMessage());
            textTopicsStatus.setText("✗ " + e.getMessage());
            textTopicsStatus.setTextColor(COLOR_FAIL);
            topicScanRunning = false;
            buttonTopicScan.setText(R.string.troubleshoot_topics_start);
        }
    }

    private void stopTopicScan() {
        topicScanRunning = false;
        buttonTopicScan.setText(R.string.troubleshoot_topics_start);

        if (topicScanClient != null) {
            try {
                topicScanClient.disconnect();
            } catch (Exception ignored) {}
            topicScanClient = null;
        }

        if (!topicLastSeen.isEmpty()) {
            textTopicsStatus.setText(getContext().getString(
                    R.string.troubleshoot_topics_done, topicLastSeen.size()));
            textTopicsStatus.setTextColor(COLOR_OK);
        }
    }

    private void updateTopicCount() {
        textTopicsStatus.setText(getContext().getString(
                R.string.troubleshoot_topics_scanning, topicLastSeen.size()));
        textTopicsStatus.setTextColor(COLOR_PROGRESS);
    }

    // ======================== Device Scan ========================

    private void startDeviceScan() {
        String prefix = SubnetScanner.getSubnetPrefix(wifiManager);
        if (prefix == null) {
            textDevicesStatus.setText(R.string.troubleshoot_devices_no_wifi);
            textDevicesStatus.setTextColor(COLOR_FAIL);
            return;
        }

        deviceScanRunning = true;
        devicesFound = 0;
        deviceListBuilder.setLength(0);
        textDevicesList.setText("");
        buttonDeviceScan.setText(R.string.troubleshoot_devices_scanning);
        buttonDeviceScan.setEnabled(true);

        String ownIp = SubnetScanner.getOwnIp(wifiManager);

        subnetScanner = new SubnetScanner();
        android.net.Network wifiNetwork = wifiNetworkManager.getBoundNetwork();
        subnetScanner.scan(prefix, wifiNetwork, new SubnetScanner.ScanCallback() {
            @Override
            public void onDeviceFound(SubnetScanner.DeviceInfo device) {
                devicesFound++;
                deviceLastUpdateTime = System.currentTimeMillis();
                StringBuilder line = new StringBuilder();
                line.append(device.ip);

                if (device.hostname != null && !device.hostname.isEmpty()) {
                    line.append("  (").append(device.hostname).append(")");
                }

                if (device.ip.equals(ownIp)) {
                    line.append("  ← this device");
                }

                if (device.mac != null && !device.mac.isEmpty()) {
                    line.append("\n  MAC: ").append(device.mac);
                }

                line.append("\n\n");
                deviceListBuilder.append(line);
                textDevicesList.setText(deviceListBuilder.toString());
            }

            @Override
            public void onProgress(int scanned, int total) {
                textDevicesStatus.setText(getContext().getString(
                        R.string.troubleshoot_devices_progress, scanned, total, devicesFound));
                textDevicesStatus.setTextColor(COLOR_PROGRESS);
            }

            @Override
            public void onComplete(int totalFound) {
                deviceScanRunning = false;
                deviceLastUpdateTime = System.currentTimeMillis();
                buttonDeviceScan.setText(R.string.troubleshoot_devices_start);
                if (totalFound < 0) {
                    textDevicesStatus.setText("SSH scan failed — is the gateway reachable?");
                    textDevicesStatus.setTextColor(COLOR_FAIL);
                } else {
                    textDevicesStatus.setText(getContext().getString(
                            R.string.troubleshoot_devices_done, totalFound));
                    textDevicesStatus.setTextColor(COLOR_OK);
                }
            }
        });
    }

    private void stopDeviceScan() {
        deviceScanRunning = false;
        buttonDeviceScan.setText(R.string.troubleshoot_devices_start);
        if (subnetScanner != null) {
            subnetScanner.cancel();
            subnetScanner = null;
        }
        if (devicesFound > 0) {
            textDevicesStatus.setText(getContext().getString(
                    R.string.troubleshoot_devices_done, devicesFound));
            textDevicesStatus.setTextColor(COLOR_OK);
        }
    }

    // ======================== Timestamp Ticker ========================

    private void startTimestampTicker() {
        if (!tickerRunning) {
            tickerRunning = true;
            mainHandler.post(timestampTicker);
        }
    }

    private void stopTimestampTicker() {
        tickerRunning = false;
        mainHandler.removeCallbacks(timestampTicker);
    }

    private void updateTimestamps() {
        long now = System.currentTimeMillis();

        // Refresh per-topic "Xs ago" display
        if (!topicLastSeen.isEmpty()) {
            renderTopicList(now);
            int count = topicLastSeen.size();
            if (topicScanRunning) {
                textTopicsStatus.setText(getContext().getString(
                        R.string.troubleshoot_topics_scanning, count));
                textTopicsStatus.setTextColor(COLOR_PROGRESS);
            } else {
                textTopicsStatus.setText(getContext().getString(
                        R.string.troubleshoot_topics_done, count));
                textTopicsStatus.setTextColor(COLOR_OK);
            }
        }

        if (deviceLastUpdateTime > 0 && !deviceScanRunning) {
            long secsAgo = (now - deviceLastUpdateTime) / 1000;
            textDevicesStatus.setText(getContext().getString(
                    R.string.troubleshoot_devices_done_ago, devicesFound, secsAgo));
            textDevicesStatus.setTextColor(COLOR_OK);
        }
    }

    /** Renders the topic list sorted by most-recently-seen first, with per-topic "Xs ago". */
    private void renderTopicList(long now) {
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(topicLastSeen.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : sorted) {
            long secsAgo = (now - entry.getValue()) / 1000;
            String ago;
            if (secsAgo < 60) {
                ago = secsAgo + "s ago";
            } else if (secsAgo < 3600) {
                ago = (secsAgo / 60) + "m ago";
            } else {
                ago = (secsAgo / 3600) + "h ago";
            }
            sb.append(String.format(Locale.US, "%-6s  %s\n", ago, entry.getKey()));
        }
        textTopicsList.setText(sb.toString());
    }
}
