package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.timing.PitWindowPreferences;
import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
import at.semmal.pitstopper.mqtt.WifiNetworkManager;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SettingsMqttActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PitWindowPreferences preferences;
    private MqttClientManager mqttClientManager;
    private ExternalSessionManager extMqtt;
    private WifiNetworkManager wifiNetworkManager;

    // WiFi binding views
    private EditText editWifiSsid;
    private TextView textWifiStatus;
    private Button buttonSwitchWifi;

    // Local broker views
    private EditText editMqttHost, editMqttPort;
    private TextView textMqttStatus;
    private Button buttonMqttConnect;

    // Public broker views
    private EditText editExtMqttHost, editExtMqttPort;
    private TextView textExtMqttStatus;
    private Button buttonExtMqttConnect;

    private MqttClientManager.StateListener mqttStateListener;
    private MqttClientManager.StateListener extMqttStateListener;
    private WifiNetworkManager.StateListener wifiStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_mqtt);
        hideSystemUI();

        preferences = new PitWindowPreferences(this);
        mqttClientManager = ((PitStopperApplication) getApplication()).getMqttClientManager();
        extMqtt = ((PitStopperApplication) getApplication()).getExternalSessionManager();
        wifiNetworkManager = ((PitStopperApplication) getApplication()).getWifiNetworkManager();

        // WiFi binding views
        editWifiSsid = findViewById(R.id.editWifiSsid);
        textWifiStatus = findViewById(R.id.textWifiStatus);
        buttonSwitchWifi = findViewById(R.id.buttonSwitchWifi);

        // Local broker views
        editMqttHost = findViewById(R.id.editMqttHost);
        editMqttPort = findViewById(R.id.editMqttPort);
        textMqttStatus = findViewById(R.id.textMqttStatus);
        buttonMqttConnect = findViewById(R.id.buttonMqttConnect);

        // Public broker views
        editExtMqttHost = findViewById(R.id.editExtMqttHost);
        editExtMqttPort = findViewById(R.id.editExtMqttPort);
        textExtMqttStatus = findViewById(R.id.textExtMqttStatus);
        buttonExtMqttConnect = findViewById(R.id.buttonExtMqttConnect);

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        setupWifiBinding();
        setupLocalMqtt();
        setupPublicMqtt();
    }

    // ===================== WiFi Binding =====================

    private android.os.Handler wifiDebounceHandler = new android.os.Handler(Looper.getMainLooper());
    private Runnable wifiDebounceRunnable;

    private void setupWifiBinding() {
        String saved = preferences.getPreferredWifiSsid();
        if (saved != null) editWifiSsid.setText(saved);

        editWifiSsid.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (wifiDebounceRunnable != null) {
                    wifiDebounceHandler.removeCallbacks(wifiDebounceRunnable);
                }
                wifiDebounceRunnable = () -> {
                    String ssid = s.toString().trim();
                    if (ssid.isEmpty()) {
                        preferences.savePreferredWifiSsid(null);
                        wifiNetworkManager.release();
                    } else {
                        preferences.savePreferredWifiSsid(ssid);
                        wifiNetworkManager.bind(ssid,
                                preferences.getMqttHost(), preferences.getMqttPort());
                    }
                };
                wifiDebounceHandler.postDelayed(wifiDebounceRunnable, 800);
            }
        });

        buttonSwitchWifi.setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        android.provider.Settings.Panel.ACTION_WIFI)));

        wifiStateListener = (state, ssid) ->
                mainHandler.post(() -> updateWifiStatusUi(state, ssid));
        wifiNetworkManager.addStateListener(wifiStateListener);
        updateWifiStatusUi(wifiNetworkManager.getState(), wifiNetworkManager.getCurrentSsid());
    }

    private void updateWifiStatusUi(WifiNetworkManager.State state, String ssid) {
        buttonSwitchWifi.setVisibility(View.GONE);
        switch (state) {
            case BOUND:
                textWifiStatus.setText(getString(R.string.wifi_status_bound, ssid));
                textWifiStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;
            case REQUESTING:
                textWifiStatus.setText(R.string.wifi_status_requesting);
                textWifiStatus.setTextColor(Color.parseColor("#FFC107"));
                break;
            case UNAVAILABLE:
                textWifiStatus.setText(R.string.wifi_status_unavailable);
                textWifiStatus.setTextColor(Color.parseColor("#F44336"));
                break;
            case WRONG_NETWORK:
                String actual = wifiNetworkManager.getConnectedSsid();
                if (actual == null) actual = "?";
                String target = ssid != null ? ssid : "?";
                textWifiStatus.setText(getString(R.string.wifi_status_wrong_network, actual, target));
                textWifiStatus.setTextColor(Color.parseColor("#F44336"));
                textWifiStatus.setTextSize(20);
                buttonSwitchWifi.setVisibility(View.VISIBLE);
                break;
            default:
                textWifiStatus.setText(R.string.wifi_status_inactive);
                textWifiStatus.setTextColor(Color.parseColor("#9E9E9E"));
                break;
        }
        if (state != WifiNetworkManager.State.WRONG_NETWORK) {
            textWifiStatus.setTextSize(16);
        }
    }

    // ===================== Local Broker =====================

    private void setupLocalMqtt() {
        editMqttHost.setText(preferences.getMqttHost());
        editMqttPort.setText(String.valueOf(preferences.getMqttPort()));

        TextWatcher savingWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                saveLocalMqttSettings();
            }
        };
        editMqttHost.addTextChangedListener(savingWatcher);
        editMqttPort.addTextChangedListener(savingWatcher);

        buttonMqttConnect.setOnClickListener(v -> toggleLocalMqttConnection());

        mqttStateListener = (state, error) ->
                mainHandler.post(() -> updateLocalMqttStatusUi(state, error));
        mqttClientManager.addStateListener(mqttStateListener);
        updateLocalMqttStatusUi(mqttClientManager.getState(), mqttClientManager.getLastError());
    }

    private void toggleLocalMqttConnection() {
        MqttClientManager.State state = mqttClientManager.getState();
        if (state == MqttClientManager.State.CONNECTED || state == MqttClientManager.State.CONNECTING) {
            preferences.saveMqttSettings(preferences.getMqttHost(), preferences.getMqttPort(), false);
            mqttClientManager.disconnect();
        } else {
            String host = editMqttHost.getText().toString().trim();
            String portText = editMqttPort.getText().toString().trim();
            if (host.isEmpty() || portText.isEmpty()) {
                Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
                    return;
                }
                preferences.saveMqttSettings(host, port, true);
                mqttClientManager.connect(host, port);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateLocalMqttStatusUi(MqttClientManager.State state, String error) {
        boolean isEditable = (state == MqttClientManager.State.DISCONNECTED
                || state == MqttClientManager.State.FAILING);
        editMqttHost.setEnabled(isEditable);
        editMqttPort.setEnabled(isEditable);

        switch (state) {
            case CONNECTED:
                textMqttStatus.setText(R.string.mqtt_status_connected);
                textMqttStatus.setTextColor(Color.parseColor("#4CAF50"));
                buttonMqttConnect.setText(R.string.mqtt_disconnect);
                break;
            case CONNECTING:
                textMqttStatus.setText(R.string.mqtt_status_connecting);
                textMqttStatus.setTextColor(Color.parseColor("#FFC107"));
                buttonMqttConnect.setText(R.string.mqtt_disconnect);
                break;
            case FAILING:
                String msg = error != null ? error : "";
                textMqttStatus.setText(getString(R.string.mqtt_status_failing, msg));
                textMqttStatus.setTextColor(Color.parseColor("#F44336"));
                buttonMqttConnect.setText(R.string.mqtt_connect);
                break;
            default:
                textMqttStatus.setText(R.string.mqtt_status_disconnected);
                textMqttStatus.setTextColor(Color.parseColor("#9E9E9E"));
                buttonMqttConnect.setText(R.string.mqtt_connect);
                break;
        }
    }

    private void saveLocalMqttSettings() {
        String host = editMqttHost.getText().toString().trim();
        String portText = editMqttPort.getText().toString().trim();
        if (host.isEmpty() || portText.isEmpty()) return;

        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) return;
            preferences.saveMqttSettings(host, port, preferences.isMqttEnabled());
        } catch (NumberFormatException ignored) {}
    }

    // ===================== Public Broker =====================

    private void setupPublicMqtt() {
        editExtMqttHost.setText(preferences.getExtMqttHost());
        editExtMqttPort.setText(String.valueOf(preferences.getExtMqttPort()));

        TextWatcher savingWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                savePublicMqttSettings();
            }
        };
        editExtMqttHost.addTextChangedListener(savingWatcher);
        editExtMqttPort.addTextChangedListener(savingWatcher);

        buttonExtMqttConnect.setOnClickListener(v -> togglePublicMqttConnection());

        extMqttStateListener = (state, error) ->
                mainHandler.post(() -> updatePublicMqttStatusUi(state, error));
        extMqtt.addStateListener(extMqttStateListener);
        updatePublicMqttStatusUi(extMqtt.getState(), null);
    }

    private void togglePublicMqttConnection() {
        MqttClientManager.State state = extMqtt.getState();
        if (state == MqttClientManager.State.CONNECTED || state == MqttClientManager.State.CONNECTING) {
            preferences.saveExtMqttSettings(preferences.getExtMqttHost(), preferences.getExtMqttPort(), false);
            extMqtt.disconnect();
        } else {
            String host = editExtMqttHost.getText().toString().trim();
            String portText = editExtMqttPort.getText().toString().trim();
            if (host.isEmpty() || portText.isEmpty()) {
                Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
                    return;
                }
                preferences.saveExtMqttSettings(host, port, true);
                String sessionId = preferences.getSessionId();
                if (sessionId != null) {
                    extMqtt.connect(sessionId, host, port);
                } else {
                    Toast.makeText(this, "Create a team session first", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updatePublicMqttStatusUi(MqttClientManager.State state, String error) {
        boolean isEditable = (state == MqttClientManager.State.DISCONNECTED
                || state == MqttClientManager.State.FAILING);
        editExtMqttHost.setEnabled(isEditable);
        editExtMqttPort.setEnabled(isEditable);

        switch (state) {
            case CONNECTED:
                textExtMqttStatus.setText(R.string.mqtt_status_connected);
                textExtMqttStatus.setTextColor(Color.parseColor("#4CAF50"));
                buttonExtMqttConnect.setText(R.string.mqtt_disconnect);
                break;
            case CONNECTING:
                textExtMqttStatus.setText(R.string.mqtt_status_connecting);
                textExtMqttStatus.setTextColor(Color.parseColor("#FFC107"));
                buttonExtMqttConnect.setText(R.string.mqtt_disconnect);
                break;
            case FAILING:
                String msg = error != null ? error : "";
                textExtMqttStatus.setText(getString(R.string.mqtt_status_failing, msg));
                textExtMqttStatus.setTextColor(Color.parseColor("#F44336"));
                buttonExtMqttConnect.setText(R.string.mqtt_connect);
                break;
            default:
                textExtMqttStatus.setText(R.string.mqtt_status_disconnected);
                textExtMqttStatus.setTextColor(Color.parseColor("#9E9E9E"));
                buttonExtMqttConnect.setText(R.string.mqtt_connect);
                break;
        }
    }

    private void savePublicMqttSettings() {
        String host = editExtMqttHost.getText().toString().trim();
        String portText = editExtMqttPort.getText().toString().trim();
        if (host.isEmpty() || portText.isEmpty()) return;
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) return;
            preferences.saveExtMqttSettings(host, port, preferences.isExtMqttEnabled());
        } catch (NumberFormatException ignored) {}
    }

    // ===================== Lifecycle =====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttStateListener != null) {
            mqttClientManager.removeStateListener(mqttStateListener);
        }
        if (extMqttStateListener != null) {
            extMqtt.removeStateListener(extMqttStateListener);
        }
        if (wifiStateListener != null) {
            wifiNetworkManager.removeStateListener(wifiStateListener);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
