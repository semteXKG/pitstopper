package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.timing.PitWindowPreferences;
import at.semmal.pitstopper.livetiming.SpeedHiveManager;
import at.semmal.pitstopper.model.SpeedHiveEvent;
import at.semmal.pitstopper.model.SpeedHiveSession;
import at.semmal.pitstopper.adapters.EventSpinnerAdapter;
import at.semmal.pitstopper.adapters.SessionSpinnerAdapter;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

public class SettingsSpeedHiveActivity extends AppCompatActivity {

    private Spinner spinnerSpeedHiveMode;
    private Spinner spinnerEventId;
    private Spinner spinnerSessionId;
    private Spinner spinnerDemoCar;
    private EditText editCarNumber;

    private TextView labelEventId;
    private FrameLayout frameEventSpinner;
    private ProgressBar progressEvents;

    private TextView labelSessionId;
    private FrameLayout frameSessionSpinner;
    private ProgressBar progressSessions;

    private TextView labelCarNumber;

    private PitWindowPreferences preferences;
    private SpeedHiveManager speedHiveManager;

    private List<SpeedHiveEvent> loadedEvents;
    private List<SpeedHiveSession> loadedSessions;

    private boolean isLoadingSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_speedhive);
        hideSystemUI();

        preferences = new PitWindowPreferences(this);

        spinnerSpeedHiveMode = findViewById(R.id.spinnerSpeedHiveMode);
        spinnerEventId = findViewById(R.id.spinnerEventId);
        spinnerSessionId = findViewById(R.id.spinnerSessionId);
        spinnerDemoCar = findViewById(R.id.spinnerDemoCar);
        editCarNumber = findViewById(R.id.editCarNumber);

        labelEventId = findViewById(R.id.labelEventId);
        frameEventSpinner = findViewById(R.id.frameEventSpinner);
        progressEvents = findViewById(R.id.progressEvents);

        labelSessionId = findViewById(R.id.labelSessionId);
        frameSessionSpinner = findViewById(R.id.frameSessionSpinner);
        progressSessions = findViewById(R.id.progressSessions);

        labelCarNumber = findViewById(R.id.labelCarNumber);

        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);

        setupModeSpinner();
        setupDemoCarSpinner();

        loadSettings();

        buttonSave.setOnClickListener(v -> saveSettings());
        buttonCancel.setOnClickListener(v -> finish());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speedHiveManager != null) {
            speedHiveManager.shutdown();
        }
    }

    private SpeedHiveManager getSpeedHiveManager() {
        if (speedHiveManager == null) {
            speedHiveManager = new SpeedHiveManager(this);
            speedHiveManager.setCellularNetwork(
                    ((PitStopperApplication) getApplication()).getCellularNetwork());
        }
        return speedHiveManager;
    }

    // ── Spinner setup ──────────────────────────────────────────────

    private void setupModeSpinner() {
        String[] modeOptions = {
            getString(R.string.speedhive_mode_off),
            getString(R.string.speedhive_mode_speedhive),
            getString(R.string.speedhive_mode_demo)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeedHiveMode.setAdapter(adapter);

        spinnerSpeedHiveMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateVisibilityForMode(position);
                if (!isLoadingSettings && position == 1) {
                    loadEvents(null);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDemoCarSpinner() {
        List<String> demoCars = new ArrayList<>();
        demoCars.add("#88 - JOHNSON");
        demoCars.add("#23 - RACER-X");
        demoCars.add("#77 - STEALTH");
        demoCars.add("#42 - MARTINEZ");
        demoCars.add("#15 - SPEEDSTER");
        demoCars.add("#99 - PHANTOM");
        demoCars.add("#7 - ACE");
        demoCars.add("#33 - VIPER");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, demoCars);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDemoCar.setAdapter(adapter);
    }

    private void setupEventSpinnerListener() {
        spinnerEventId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isLoadingSettings && loadedEvents != null
                        && position >= 0 && position < loadedEvents.size()) {
                    SpeedHiveEvent event = loadedEvents.get(position);
                    loadSessions(event.getId(), event.isLive(), null);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ── Visibility ─────────────────────────────────────────────────

    private void updateVisibilityForMode(int modePosition) {
        boolean isSpeedHive = (modePosition == 1);
        boolean isDemo = (modePosition == 2);

        int speedhiveVis = isSpeedHive ? View.VISIBLE : View.GONE;
        labelEventId.setVisibility(speedhiveVis);
        frameEventSpinner.setVisibility(speedhiveVis);
        labelSessionId.setVisibility(speedhiveVis);
        frameSessionSpinner.setVisibility(speedhiveVis);
        labelCarNumber.setVisibility(speedhiveVis);
        editCarNumber.setVisibility(speedhiveVis);

        spinnerDemoCar.setVisibility(isDemo ? View.VISIBLE : View.GONE);
    }

    // ── Load / save settings ───────────────────────────────────────

    private void loadSettings() {
        isLoadingSettings = true;

        String mode = preferences.getSpeedHiveMode();
        int modeIndex;
        switch (mode) {
            case PitWindowPreferences.SPEEDHIVE_MODE_SPEEDHIVE: modeIndex = 1; break;
            case PitWindowPreferences.SPEEDHIVE_MODE_DEMO:      modeIndex = 2; break;
            default:                                             modeIndex = 0; break;
        }
        spinnerSpeedHiveMode.setSelection(modeIndex);
        updateVisibilityForMode(modeIndex);

        // Car number
        String savedCar = preferences.getSpeedHiveCarNumber();
        if (!savedCar.isEmpty()) {
            editCarNumber.setText(savedCar);
        }

        // Demo car pre-selection
        if (modeIndex == 2) {
            selectDemoCar(savedCar);
        }

        // SpeedHive API: fetch events → pre-select saved event → fetch sessions → pre-select saved session
        if (modeIndex == 1) {
            loadEvents(preferences.getSpeedHiveEventId());
        }

        isLoadingSettings = false;
    }

    private void selectDemoCar(String carNumber) {
        if (carNumber == null || carNumber.isEmpty()) return;
        for (int i = 0; i < spinnerDemoCar.getCount(); i++) {
            String item = (String) spinnerDemoCar.getItemAtPosition(i);
            if (item.startsWith("#" + carNumber + " ")) {
                spinnerDemoCar.setSelection(i);
                return;
            }
        }
    }

    // ── API calls ──────────────────────────────────────────────────

    private void loadEvents(String preselectEventId) {
        progressEvents.setVisibility(View.VISIBLE);
        getSpeedHiveManager().fetchEvents(new SpeedHiveManager.EventsCallback() {
            @Override
            public void onSuccess(List<SpeedHiveEvent> events) {
                runOnUiThread(() -> {
                    progressEvents.setVisibility(View.GONE);
                    loadedEvents = events;
                    EventSpinnerAdapter adapter = new EventSpinnerAdapter(
                            SettingsSpeedHiveActivity.this, events);
                    spinnerEventId.setAdapter(adapter);
                    setupEventSpinnerListener();

                    int selectIndex = 0;
                    if (preselectEventId != null && !preselectEventId.isEmpty()) {
                        for (int i = 0; i < events.size(); i++) {
                            if (events.get(i).getId().equals(preselectEventId)) {
                                selectIndex = i;
                                break;
                            }
                        }
                    }
                    spinnerEventId.setSelection(selectIndex);

                    if (!events.isEmpty()) {
                        SpeedHiveEvent selected = events.get(selectIndex);
                        loadSessions(selected.getId(), selected.isLive(),
                                preferences.getSpeedHiveSessionId());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressEvents.setVisibility(View.GONE);
                    Toast.makeText(SettingsSpeedHiveActivity.this,
                            "Failed to load events: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadSessions(String eventId, boolean parentEventLive, String preselectSessionId) {
        progressSessions.setVisibility(View.VISIBLE);
        getSpeedHiveManager().fetchSessions(eventId, parentEventLive,
                new SpeedHiveManager.SessionsCallback() {
            @Override
            public void onSuccess(List<SpeedHiveSession> sessions) {
                runOnUiThread(() -> {
                    progressSessions.setVisibility(View.GONE);
                    loadedSessions = sessions;
                    SessionSpinnerAdapter adapter = new SessionSpinnerAdapter(
                            SettingsSpeedHiveActivity.this, sessions);
                    spinnerSessionId.setAdapter(adapter);

                    // Position 0 = AUTO; manual sessions start at 1
                    int selectIndex = 0;
                    if (preselectSessionId != null && !preselectSessionId.isEmpty()
                            && !PitWindowPreferences.SPEEDHIVE_SESSION_AUTO.equals(preselectSessionId)) {
                        for (int i = 0; i < sessions.size(); i++) {
                            if (sessions.get(i).getId().equals(preselectSessionId)) {
                                selectIndex = i + 1; // offset for AUTO
                                break;
                            }
                        }
                    }
                    spinnerSessionId.setSelection(selectIndex);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressSessions.setVisibility(View.GONE);
                    Toast.makeText(SettingsSpeedHiveActivity.this,
                            "Failed to load sessions: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Save ───────────────────────────────────────────────────────

    private void saveSettings() {
        int modeSelection = spinnerSpeedHiveMode.getSelectedItemPosition();
        String mode;
        switch (modeSelection) {
            case 1: mode = PitWindowPreferences.SPEEDHIVE_MODE_SPEEDHIVE; break;
            case 2: mode = PitWindowPreferences.SPEEDHIVE_MODE_DEMO; break;
            default: mode = PitWindowPreferences.SPEEDHIVE_MODE_OFF; break;
        }

        String eventId = "", eventName = "", sessionId = "", sessionName = "", carNumber = "", carName = "";

        if (modeSelection == 1) {
            carNumber = editCarNumber.getText().toString().trim();
            if (carNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a car number", Toast.LENGTH_SHORT).show();
                return;
            }
            try { Integer.parseInt(carNumber); } catch (NumberFormatException e) {
                Toast.makeText(this, "Car number must be numeric", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (modeSelection == 2) {
            if (spinnerDemoCar.getSelectedItemPosition() >= 0) {
                String selectedCar = (String) spinnerDemoCar.getSelectedItem();
                carNumber = selectedCar.split(" ")[0].substring(1);
            } else {
                Toast.makeText(this, "Please select a car", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (modeSelection == 1) {
            int eventPos = spinnerEventId.getSelectedItemPosition();
            if (loadedEvents != null && eventPos >= 0 && eventPos < loadedEvents.size()) {
                SpeedHiveEvent event = loadedEvents.get(eventPos);
                eventId = event.getId();
                eventName = event.getName();
            }
            int sessionPos = spinnerSessionId.getSelectedItemPosition();
            if (sessionPos == 0) {
                sessionId = PitWindowPreferences.SPEEDHIVE_SESSION_AUTO;
                sessionName = "AUTO";
            } else if (loadedSessions != null && sessionPos - 1 >= 0 && sessionPos - 1 < loadedSessions.size()) {
                SpeedHiveSession session = loadedSessions.get(sessionPos - 1);
                sessionId = session.getId();
                sessionName = session.getDisplayName();
            }
            carName = "";
        }

        preferences.saveAllSpeedHive(mode, eventId, eventName, sessionId, sessionName, carNumber, carName);
        Toast.makeText(this, "SpeedHive settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ── Immersive mode ─────────────────────────────────────────────

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
