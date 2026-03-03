package at.semmal.pitstopper.activities;

import android.graphics.Bitmap;
import android.graphics.Color;
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
    private static final String DEEP_LINK_BASE = "pitstopper://join?session=";

    private ImageView imageQrCode;
    private TextView textStatus;
    private TextView textSessionId;
    private View statusDot;
    private EditText editDeviceName;

    private PitWindowPreferences preferences;
    private ExternalSessionManager sessionManager;
    private MqttClientManager.StateListener stateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        imageQrCode  = findViewById(R.id.imageQrCode);
        textStatus   = findViewById(R.id.textStatus);
        textSessionId = findViewById(R.id.textSessionId);
        statusDot    = findViewById(R.id.statusDot);
        editDeviceName = findViewById(R.id.editDeviceName);

        preferences    = new PitWindowPreferences(this);
        sessionManager = ((PitStopperApplication) getApplication()).getExternalSessionManager();

        Button btnNew     = findViewById(R.id.buttonNewSession);
        Button btnRestore = findViewById(R.id.buttonRestoreSession);

        btnNew.setOnClickListener(v -> createNewSession());
        btnRestore.setOnClickListener(v -> restoreSession());

        // Load saved device label
        String saved = preferences.getDeviceLabel();
        if (saved != null) editDeviceName.setText(saved);

        // Show current session if one exists
        String sessionId = preferences.getSessionId();
        if (sessionId != null) {
            showSession(sessionId);
        }

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
        sessionManager.disconnect();
        sessionManager.connect(sessionId);
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
            sessionManager.connect(sessionId);
        }
        showSession(sessionId);
        Toast.makeText(this, "Session restored", Toast.LENGTH_SHORT).show();
    }

    private void showSession(String sessionId) {
        textSessionId.setText(sessionId.substring(0, 8) + "...");

        // Run QR generation off the main thread — 512x512 bitmap is expensive
        new Thread(() -> {
            try {
                String uri = DEEP_LINK_BASE + sessionId;
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
                break;
            case CONNECTING:
                statusDot.setBackgroundResource(R.drawable.circle_dot);
                textStatus.setText("Connecting…");
                break;
            case FAILING:
                statusDot.setBackgroundResource(R.drawable.circle_dot_red);
                textStatus.setText("Error: " + (error != null ? error : "unknown"));
                break;
            default:
                statusDot.setBackgroundResource(R.drawable.circle_dot);
                textStatus.setText("Not connected");
        }
    }
}
