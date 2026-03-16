package at.semmal.pitstopper.activities;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

public class SessionActivity extends AppCompatActivity {

    private static final String TAG = "SessionActivity";
    private static final int QR_SIZE = 512;
    private static final String DEEP_LINK_SCHEME = "pitstopper://join?";

    private ImageView imageQrCode;
    private TextView textStatus;
    private TextView textTopic;
    private View statusDot;
    private EditText editDeviceName;
    private Button buttonExtMqttConnect;

    private PitWindowPreferences preferences;
    private ExternalSessionManager sessionManager;
    private MqttClientManager.StateListener stateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        imageQrCode  = findViewById(R.id.imageQrCode);
        textStatus   = findViewById(R.id.textStatus);
        textTopic    = findViewById(R.id.textTopic);
        statusDot    = findViewById(R.id.statusDot);
        editDeviceName = findViewById(R.id.editDeviceName);
        buttonExtMqttConnect = findViewById(R.id.buttonExtMqttConnect);

        preferences    = new PitWindowPreferences(this);
        sessionManager = ((PitStopperApplication) getApplication()).getExternalSessionManager();

        Button btnNew     = findViewById(R.id.buttonNewSession);
        Button btnRestore = findViewById(R.id.buttonRestoreSession);
        findViewById(R.id.buttonClose).setOnClickListener(v -> finish());

        btnNew.setOnClickListener(v -> createNewSession());
        btnRestore.setOnClickListener(v -> restoreSession());
        buttonExtMqttConnect.setOnClickListener(v -> toggleExtMqttConnection());

        // Load saved device label
        String saved = preferences.getDeviceLabel();
        if (saved != null) editDeviceName.setText(saved);

        // Show current session (auto-create if first run)
        String sessionId = preferences.getSessionId();
        if (sessionId == null) {
            sessionId = preferences.generateAndSaveNewSessionId();
            ((PitStopperApplication) getApplication()).connectExternalSession(sessionId,
                    preferences.getExtMqttHost(), preferences.getExtMqttPort());
        }
        showSession(sessionId);

        // Watch connection state
        stateListener = (state, error) -> runOnUiThread(() -> updateStatus(state, error));
        sessionManager.addStateListener(stateListener);
        updateStatus(sessionManager.getState(), null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Persist device label
        String label = editDeviceName.getText().toString().trim();
        if (!TextUtils.isEmpty(label)) {
            preferences.saveDeviceLabel(label);
        }
        sessionManager.removeStateListener(stateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionManager.addStateListener(stateListener);
        updateStatus(sessionManager.getState(), null);
    }

    private void createNewSession() {
        String sessionId = preferences.generateAndSaveNewSessionId();
        PitStopperApplication app = (PitStopperApplication) getApplication();
        app.disconnectExternalSession();
        app.connectExternalSession(sessionId,
                preferences.getExtMqttHost(), preferences.getExtMqttPort());
        showSession(sessionId);
        Toast.makeText(this, "New session created", Toast.LENGTH_SHORT).show();
    }

    private void restoreSession() {
        String sessionId = preferences.getSessionId();
        if (sessionId == null) {
            Toast.makeText(this, "No saved session — create one first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!sessionManager.isConnected()) {
            ((PitStopperApplication) getApplication()).connectExternalSession(sessionId,
                    preferences.getExtMqttHost(), preferences.getExtMqttPort());
        }
        showSession(sessionId);
        Toast.makeText(this, "Session restored", Toast.LENGTH_SHORT).show();
    }

    private void showSession(String sessionId) {
        textTopic.setText(sessionId + "/fiesta/chat");

        // Run QR generation off the main thread — 512x512 bitmap is expensive
        new Thread(() -> {
            try {
                String host = preferences.getExtMqttHost();
                int port = preferences.getExtMqttPort();
                String uri = DEEP_LINK_SCHEME + "session=" + sessionId
                        + "&host=" + host + "&port=" + port;
                BitMatrix matrix = new MultiFormatWriter().encode(uri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
                int[] pixels = new int[QR_SIZE * QR_SIZE];
                for (int y = 0; y < QR_SIZE; y++) {
                    for (int x = 0; x < QR_SIZE; x++) {
                        pixels[y * QR_SIZE + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }
                Bitmap bmp = Bitmap.createBitmap(pixels, QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565);
                runOnUiThread(() -> imageQrCode.setImageBitmap(bmp));
            } catch (WriterException e) {
                Log.e(TAG, "QR generation failed", e);
            }
        }).start();
    }

    private void updateStatus(MqttClientManager.State state, String error) {
        switch (state) {
            case CONNECTED:
                statusDot.setBackgroundResource(R.drawable.circle_dot_green);
                textStatus.setText("Connected");
                buttonExtMqttConnect.setText(R.string.mqtt_disconnect);
                buttonExtMqttConnect.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                break;
            case CONNECTING:
                statusDot.setBackgroundResource(R.drawable.circle_dot);
                textStatus.setText("Connecting…");
                buttonExtMqttConnect.setText(R.string.mqtt_disconnect);
                buttonExtMqttConnect.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                break;
            case FAILING:
                statusDot.setBackgroundResource(R.drawable.circle_dot_red);
                textStatus.setText("Error: " + (error != null ? error : "unknown"));
                buttonExtMqttConnect.setText(R.string.mqtt_connect);
                buttonExtMqttConnect.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#009688")));
                break;
            default:
                statusDot.setBackgroundResource(R.drawable.circle_dot);
                textStatus.setText("Not connected");
                buttonExtMqttConnect.setText(R.string.mqtt_connect);
                buttonExtMqttConnect.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#009688")));
                break;
        }
    }

    private void toggleExtMqttConnection() {
        PitStopperApplication app = (PitStopperApplication) getApplication();
        MqttClientManager.State state = sessionManager.getState();
        if (state == MqttClientManager.State.CONNECTED || state == MqttClientManager.State.CONNECTING) {
            preferences.saveExtMqttSettings(preferences.getExtMqttHost(), preferences.getExtMqttPort(), false);
            app.disconnectExternalSession();
        } else {
            String host = preferences.getExtMqttHost();
            int port = preferences.getExtMqttPort();
            if (host == null || host.isEmpty()) {
                Toast.makeText(this, "Set broker host in Settings first", Toast.LENGTH_SHORT).show();
                return;
            }
            String sessionId = preferences.getSessionId();
            if (sessionId == null) {
                Toast.makeText(this, "Create a team session first", Toast.LENGTH_SHORT).show();
                return;
            }
            preferences.saveExtMqttSettings(host, port, true);
            app.connectExternalSession(sessionId, host, port);
        }
    }
}
