package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.adapters.EventSpinnerAdapter;
import at.semmal.pitstopper.adapters.SessionSpinnerAdapter;
import at.semmal.pitstopper.livetiming.SpeedHiveManager;
import at.semmal.pitstopper.model.SpeedHiveEvent;
import at.semmal.pitstopper.model.SpeedHiveSession;
import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private Button buttonSelectTime;
    private EditText editPitWindowOpens;
    private EditText editPitWindowDuration;
    private EditText editMinPitStop;
    private Button buttonSave;
    private Button buttonCancel;
    
    // SpeedHive UI elements
    private Spinner spinnerSpeedHiveMode;
    private TextView labelEventId, labelSessionId, labelCarNumber;
    private Spinner spinnerEventId, spinnerSessionId;
    private FrameLayout frameEventSpinner, frameSessionSpinner;
    private ProgressBar progressEvents, progressSessions;
    private EditText editCarNumber; // For SpeedHive live mode
    private Spinner spinnerDemoCar; // For demo mode car selection
    
    // MQTT broker UI elements
    private EditText editMqttHost;
    private EditText editMqttPort;
    private TextView textMqttStatus;
    private Button buttonMqttConnect;

    private EditText editExtMqttHost;
    private EditText editExtMqttPort;
    private TextView textExtMqttStatus;
    private Button buttonExtMqttConnect;

    private MqttClientManager mqttClientManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MqttClientManager.StateListener mqttStateListener;
    private MqttClientManager.StateListener extMqttStateListener;

    // SpeedHive data
    private SpeedHiveManager speedHiveManager;
    private List<SpeedHiveEvent> loadedEvents;
    private List<SpeedHiveSession> loadedSessions;
    private String savedEventId;
    private String savedSessionId;
    private String savedCarNumber;
    private boolean suppressEventSelection = false;

    private int raceStartHour = 9;
    private int raceStartMinute = 0;

    private PitWindowPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Enable fullscreen immersive mode
        hideSystemUI();

        // Initialize preferences
        preferences = new PitWindowPreferences(this);

        // Initialize views
        buttonSelectTime = findViewById(R.id.buttonSelectTime);
        editPitWindowOpens = findViewById(R.id.editPitWindowOpens);
        editPitWindowDuration = findViewById(R.id.editPitWindowDuration);
        editMinPitStop = findViewById(R.id.editMinPitStop);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        
        // Initialize SpeedHive views
        spinnerSpeedHiveMode = findViewById(R.id.spinnerSpeedHiveMode);
        labelEventId = findViewById(R.id.labelEventId);
        spinnerEventId = findViewById(R.id.spinnerEventId);
        frameEventSpinner = findViewById(R.id.frameEventSpinner);
        progressEvents = findViewById(R.id.progressEvents);
        labelSessionId = findViewById(R.id.labelSessionId);
        spinnerSessionId = findViewById(R.id.spinnerSessionId);
        frameSessionSpinner = findViewById(R.id.frameSessionSpinner);
        progressSessions = findViewById(R.id.progressSessions);
        labelCarNumber = findViewById(R.id.labelCarNumber);
        editCarNumber = findViewById(R.id.editCarNumber);
        spinnerDemoCar = findViewById(R.id.spinnerDemoCar);
        
        // Initialize MQTT broker views
        editMqttHost = findViewById(R.id.editMqttHost);
        editMqttPort = findViewById(R.id.editMqttPort);
        textMqttStatus = findViewById(R.id.textMqttStatus);
        buttonMqttConnect = findViewById(R.id.buttonMqttConnect);

        editExtMqttHost = findViewById(R.id.editExtMqttHost);
        editExtMqttPort = findViewById(R.id.editExtMqttPort);
        textExtMqttStatus = findViewById(R.id.textExtMqttStatus);
        buttonExtMqttConnect = findViewById(R.id.buttonExtMqttConnect);

        mqttClientManager = ((PitStopperApplication) getApplication()).getMqttClientManager();

        // Store saved IDs for pre-selection after data loads
        savedEventId = preferences.getSpeedHiveEventId();
        savedSessionId = preferences.getSpeedHiveSessionId();
        savedCarNumber = preferences.getSpeedHiveCarNumber();

        // Load current settings
        loadSettings();
        
        // Set up SpeedHive mode spinner
        setupSpeedHiveModeSpinner();
        
        // Set up event and session spinners
        setupEventSpinner();
        setupSessionSpinner();
        setupDemoCarSpinner();
        
        // Load SpeedHive settings AFTER spinners are set up
        loadSpeedHiveSettings();
        
        // Set up MQTT broker
        setupMqttClient();

        // Set up external (public) MQTT broker
        setupExtMqttClient();

        // Set up time picker button
        buttonSelectTime.setOnClickListener(v -> showTimePicker());

        // Set up button listeners
        buttonSave.setOnClickListener(v -> saveSettings());
        buttonCancel.setOnClickListener(v -> finish());

        Button buttonTeamSession = findViewById(R.id.buttonTeamSession);
        buttonTeamSession.setOnClickListener(v ->
                startActivity(new Intent(this, SessionActivity.class)));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speedHiveManager != null) {
            speedHiveManager.shutdown();
        }
        if (mqttStateListener != null) {
            mqttClientManager.removeStateListener(mqttStateListener);
        }
        if (extMqttStateListener != null) {
            ((PitStopperApplication) getApplication()).getExternalSessionManager()
                    .removeStateListener(extMqttStateListener);
        }
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                raceStartHour = hourOfDay;
                raceStartMinute = minute;
                updateTimeButtonText();
            },
            raceStartHour,
            raceStartMinute,
            true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void updateTimeButtonText() {
        String timeText = String.format(Locale.getDefault(), "%02d:%02d", raceStartHour, raceStartMinute);
        buttonSelectTime.setText(timeText);
    }

    private void loadSettings() {
        // Load from SharedPreferences
        raceStartHour = preferences.getRaceStartHour();
        raceStartMinute = preferences.getRaceStartMinute();
        updateTimeButtonText();

        editPitWindowOpens.setText(String.valueOf(preferences.getPitWindowOpens()));
        editPitWindowDuration.setText(String.valueOf(preferences.getPitWindowDuration()));
        editMinPitStop.setText(String.valueOf(preferences.getMinPitStopSeconds()));
        
        // Load car number for SpeedHive live mode - demo mode uses spinner
        editCarNumber.setText(preferences.getSpeedHiveCarNumber());
    }
    
    private void setupSpeedHiveModeSpinner() {
        // Create spinner adapter with mode options
        String[] modeOptions = {
            getString(R.string.speedhive_mode_off),
            getString(R.string.speedhive_mode_speedhive), 
            getString(R.string.speedhive_mode_demo)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, modeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeedHiveMode.setAdapter(adapter);
        
        // Set selection change listener
        spinnerSpeedHiveMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSpeedHiveFieldVisibility(position);
                if (position == 1) {
                    // SpeedHive mode selected — fetch events
                    loadEvents();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupEventSpinner() {
        spinnerEventId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressEventSelection) return;
                if (loadedEvents != null && position < loadedEvents.size()) {
                    SpeedHiveEvent event = loadedEvents.get(position);
                    loadSessions(event.getId(), event.isLive());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void loadSpeedHiveSettings() {
        String currentMode = preferences.getSpeedHiveMode();
        
        // Set spinner selection based on current mode
        int selection = 0; // Default to "off"
        if (PitWindowPreferences.SPEEDHIVE_MODE_SPEEDHIVE.equals(currentMode)) {
            selection = 1;
        } else if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(currentMode)) {
            selection = 2;
        }
        spinnerSpeedHiveMode.setSelection(selection);
        
        // Update field visibility
        updateSpeedHiveFieldVisibility(selection);
        
        // If SpeedHive mode is selected, load events to populate dropdowns
        if (selection == 1) {
            loadEvents();
        }
    }
    
    private void updateSpeedHiveFieldVisibility(int modeSelection) {
        // 0 = Off, 1 = SpeedHive, 2 = Demo
        
        if (modeSelection == 0) {
            // Off - hide all SpeedHive fields
            labelEventId.setVisibility(View.GONE);
            frameEventSpinner.setVisibility(View.GONE);
            labelSessionId.setVisibility(View.GONE);
            frameSessionSpinner.setVisibility(View.GONE);
            labelCarNumber.setVisibility(View.GONE);
            editCarNumber.setVisibility(View.GONE);
            spinnerDemoCar.setVisibility(View.GONE);
        } else if (modeSelection == 1) {
            // SpeedHive mode - show all fields with EditText for car
            labelEventId.setVisibility(View.VISIBLE);
            frameEventSpinner.setVisibility(View.VISIBLE);
            labelSessionId.setVisibility(View.VISIBLE);
            frameSessionSpinner.setVisibility(View.VISIBLE);
            labelCarNumber.setVisibility(View.VISIBLE);
            editCarNumber.setVisibility(View.VISIBLE);
            spinnerDemoCar.setVisibility(View.GONE);
        } else if (modeSelection == 2) {
            // Demo mode - hide event/session, show demo car selection
            labelEventId.setVisibility(View.GONE);
            frameEventSpinner.setVisibility(View.GONE);
            labelSessionId.setVisibility(View.GONE);
            frameSessionSpinner.setVisibility(View.GONE);
            labelCarNumber.setVisibility(View.VISIBLE);
            editCarNumber.setVisibility(View.GONE);
            spinnerDemoCar.setVisibility(View.VISIBLE);
        }
    }

    // --- SpeedHive API loading ---

    private SpeedHiveManager getSpeedHiveManager() {
        if (speedHiveManager == null) {
            speedHiveManager = new SpeedHiveManager(this);
        }
        return speedHiveManager;
    }

    private void loadEvents() {
        progressEvents.setVisibility(View.VISIBLE);
        spinnerEventId.setVisibility(View.INVISIBLE);

        getSpeedHiveManager().fetchEvents(new SpeedHiveManager.EventsCallback() {
            @Override
            public void onSuccess(List<SpeedHiveEvent> events) {
                runOnUiThread(() -> {
                    loadedEvents = events;
                    progressEvents.setVisibility(View.GONE);
                    spinnerEventId.setVisibility(View.VISIBLE);

                    EventSpinnerAdapter adapter = new EventSpinnerAdapter(SettingsActivity.this, events);
                    suppressEventSelection = true;
                    spinnerEventId.setAdapter(adapter);

                    // Pre-select saved event
                    int savedIndex = -1;
                    for (int i = 0; i < events.size(); i++) {
                        if (events.get(i).getId().equals(savedEventId)) {
                            savedIndex = i;
                            break;
                        }
                    }

                    if (savedIndex >= 0) {
                        spinnerEventId.setSelection(savedIndex);
                    }
                    suppressEventSelection = false;

                    // Set up event selection listener
                    setupEventSpinner();

                    // Load sessions for the selected event
                    int selectedPos = spinnerEventId.getSelectedItemPosition();
                    if (events.size() > 0 && selectedPos >= 0 && selectedPos < events.size()) {
                        SpeedHiveEvent event = events.get(selectedPos);
                        loadSessions(event.getId(), event.isLive());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressEvents.setVisibility(View.GONE);
                    spinnerEventId.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Failed to load events: " + error);
                    Toast.makeText(SettingsActivity.this, "Failed to load events: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadSessions(String eventId, boolean parentEventLive) {
        progressSessions.setVisibility(View.VISIBLE);
        spinnerSessionId.setVisibility(View.INVISIBLE);

        getSpeedHiveManager().fetchSessions(eventId, parentEventLive, new SpeedHiveManager.SessionsCallback() {
            @Override
            public void onSuccess(List<SpeedHiveSession> sessions) {
                runOnUiThread(() -> {
                    loadedSessions = sessions;
                    progressSessions.setVisibility(View.GONE);
                    spinnerSessionId.setVisibility(View.VISIBLE);

                    SessionSpinnerAdapter adapter = new SessionSpinnerAdapter(SettingsActivity.this, sessions);
                    spinnerSessionId.setAdapter(adapter);

                    // Pre-select saved session (AUTO is position 0, regular sessions start at 1)
                    if (PitWindowPreferences.SPEEDHIVE_SESSION_AUTO.equals(savedSessionId) || savedSessionId.isEmpty()) {
                        spinnerSessionId.setSelection(0); // AUTO
                    } else {
                        boolean found = false;
                        for (int i = 0; i < sessions.size(); i++) {
                            if (sessions.get(i).getId().equals(savedSessionId)) {
                                spinnerSessionId.setSelection(i + 1); // +1 because AUTO is at position 0
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            spinnerSessionId.setSelection(0); // Default to AUTO if saved session not found
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressSessions.setVisibility(View.GONE);
                    spinnerSessionId.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Failed to load sessions: " + error);
                    Toast.makeText(SettingsActivity.this, "Failed to load sessions: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupSessionSpinner() {
        spinnerSessionId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Session selection changed - no longer need to load cars
                // Car number is now manually entered via EditText
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupDemoCarSpinner() {
        // Create demo car list with car number and driver name
        List<String> demoCars = new ArrayList<>();
        demoCars.add("#88 - JOHNSON");
        demoCars.add("#23 - RACER-X");
        demoCars.add("#77 - STEALTH");
        demoCars.add("#42 - MARTINEZ");
        demoCars.add("#15 - SPEEDSTER");
        demoCars.add("#99 - PHANTOM");
        demoCars.add("#7 - ACE");
        demoCars.add("#33 - VIPER");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, demoCars);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDemoCar.setAdapter(adapter);

        // Pre-select based on saved car number
        String savedCar = preferences.getSpeedHiveCarNumber();
        if (!savedCar.isEmpty()) {
            for (int i = 0; i < demoCars.size(); i++) {
                if (demoCars.get(i).startsWith("#" + savedCar + " ")) {
                    spinnerDemoCar.setSelection(i);
                    break;
                }
            }
        }
    }

    private SpeedHiveEvent getSelectedEvent() {
        if (loadedEvents != null && spinnerEventId.getSelectedItemPosition() >= 0) {
            int pos = spinnerEventId.getSelectedItemPosition();
            if (pos < loadedEvents.size()) {
                return loadedEvents.get(pos);
            }
        }
        return null;
    }

    private void saveSettings() {
        try {
            int pitWindowOpens = Integer.parseInt(editPitWindowOpens.getText().toString());
            int pitWindowDuration = Integer.parseInt(editPitWindowDuration.getText().toString());
            int minPitStop = Integer.parseInt(editMinPitStop.getText().toString());

            // Validate inputs
            if (pitWindowOpens < 0 || pitWindowOpens > 300) {
                Toast.makeText(this, "Pit window opens time must be between 0 and 300 minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pitWindowDuration < 1 || pitWindowDuration > 60) {
                Toast.makeText(this, "Pit window duration must be between 1 and 60 minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            if (minPitStop < 1 || minPitStop > 600) {
                Toast.makeText(this, "Min. pit stop must be between 1 and 600 seconds", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to SharedPreferences
            preferences.saveAll(raceStartHour, raceStartMinute, pitWindowOpens, pitWindowDuration);
            preferences.saveMinPitStopSeconds(minPitStop);
            
            // Save SpeedHive settings
            saveSpeedHiveSettings();

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveSpeedHiveSettings() {
        int modeSelection = spinnerSpeedHiveMode.getSelectedItemPosition();
        String mode;
        
        switch (modeSelection) {
            case 1:
                mode = PitWindowPreferences.SPEEDHIVE_MODE_SPEEDHIVE;
                break;
            case 2:
                mode = PitWindowPreferences.SPEEDHIVE_MODE_DEMO;
                break;
            default:
                mode = PitWindowPreferences.SPEEDHIVE_MODE_OFF;
                break;
        }
        
        String eventId = "";
        String eventName = "";
        String sessionId = "";
        String sessionName = "";
        String carNumber = "";
        String carName = "";

        if (modeSelection == 1) {
            // SpeedHive mode - validate car number from EditText
            carNumber = editCarNumber.getText().toString().trim();
            if (carNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a car number", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate numeric input
            try {
                Integer.parseInt(carNumber);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Car number must be numeric", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (modeSelection == 2) {
            // Demo mode - get car number from spinner selection
            if (spinnerDemoCar.getSelectedItemPosition() >= 0) {
                String selectedCar = (String) spinnerDemoCar.getSelectedItem();
                // Extract car number from format "#88 - JOHNSON"
                carNumber = selectedCar.split(" ")[0].substring(1); // Remove "#" prefix
            } else {
                Toast.makeText(this, "Please select a car", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (modeSelection == 1) {
            // SpeedHive mode - get from spinners and EditText
            // Get selected event
            int eventPos = spinnerEventId.getSelectedItemPosition();
            if (loadedEvents != null && eventPos >= 0 && eventPos < loadedEvents.size()) {
                SpeedHiveEvent event = loadedEvents.get(eventPos);
                eventId = event.getId();
                eventName = event.getName();
            }

            // Get selected session
            int sessionPos = spinnerSessionId.getSelectedItemPosition();
            if (sessionPos == 0) {
                // AUTO selected
                sessionId = PitWindowPreferences.SPEEDHIVE_SESSION_AUTO;
                sessionName = "AUTO";
            } else if (loadedSessions != null && sessionPos - 1 >= 0 && sessionPos - 1 < loadedSessions.size()) {
                SpeedHiveSession session = loadedSessions.get(sessionPos - 1);
                sessionId = session.getId();
                sessionName = session.getDisplayName();
            }

            carName = ""; // Not needed since we're entering manually
        }
        // For demo mode, no additional processing needed - carNumber is already set

        preferences.saveAllSpeedHive(mode, eventId, eventName, sessionId, sessionName, carNumber, carName);
    }
    
    // --- MQTT Broker ---

    private void setupMqttClient() {
        editMqttHost.setText(preferences.getMqttHost());
        editMqttPort.setText(String.valueOf(preferences.getMqttPort()));

        TextWatcher savingWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                saveMqttClientSettings();
            }
        };
        editMqttHost.addTextChangedListener(savingWatcher);
        editMqttPort.addTextChangedListener(savingWatcher);

        buttonMqttConnect.setOnClickListener(v -> toggleMqttConnection());

        // Register state listener — always dispatch to main thread
        mqttStateListener = (state, error) -> mainHandler.post(() -> updateMqttStatusUi(state, error));
        mqttClientManager.addStateListener(mqttStateListener);

        // Reflect current state immediately
        updateMqttStatusUi(mqttClientManager.getState(), mqttClientManager.getLastError());
    }

    private void toggleMqttConnection() {
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

    private void updateMqttStatusUi(MqttClientManager.State state, String error) {
        boolean isEditable = (state == MqttClientManager.State.DISCONNECTED
                || state == MqttClientManager.State.FAILING);
        editMqttHost.setEnabled(isEditable);
        editMqttPort.setEnabled(isEditable);

        switch (state) {
            case CONNECTED:
                textMqttStatus.setText(R.string.mqtt_status_connected);
                textMqttStatus.setTextColor(Color.parseColor("#4CAF50")); // green
                buttonMqttConnect.setText(R.string.mqtt_disconnect);
                break;
            case CONNECTING:
                textMqttStatus.setText(R.string.mqtt_status_connecting);
                textMqttStatus.setTextColor(Color.parseColor("#FFC107")); // amber
                buttonMqttConnect.setText(R.string.mqtt_disconnect);
                break;
            case FAILING:
                String msg = error != null ? error : "";
                textMqttStatus.setText(getString(R.string.mqtt_status_failing, msg));
                textMqttStatus.setTextColor(Color.parseColor("#F44336")); // red
                buttonMqttConnect.setText(R.string.mqtt_connect);
                break;
            default: // DISCONNECTED
                textMqttStatus.setText(R.string.mqtt_status_disconnected);
                textMqttStatus.setTextColor(Color.parseColor("#9E9E9E")); // gray
                buttonMqttConnect.setText(R.string.mqtt_connect);
                break;
        }
    }

    private void saveMqttClientSettings() {
        String host = editMqttHost.getText().toString().trim();
        String portText = editMqttPort.getText().toString().trim();
        if (host.isEmpty() || portText.isEmpty()) return;

        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
                return;
            }
            preferences.saveMqttSettings(host, port, preferences.isMqttEnabled());
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.mqtt_invalid_port, Toast.LENGTH_SHORT).show();
        }
    }

    // --- External (Public) MQTT Broker ---

    private void setupExtMqttClient() {
        ExternalSessionManager extMqtt = ((PitStopperApplication) getApplication()).getExternalSessionManager();

        editExtMqttHost.setText(preferences.getExtMqttHost());
        editExtMqttPort.setText(String.valueOf(preferences.getExtMqttPort()));

        TextWatcher savingWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                saveExtMqttClientSettings();
            }
        };
        editExtMqttHost.addTextChangedListener(savingWatcher);
        editExtMqttPort.addTextChangedListener(savingWatcher);

        buttonExtMqttConnect.setOnClickListener(v -> toggleExtMqttConnection(extMqtt));

        extMqttStateListener = (state, error) -> mainHandler.post(() -> updateExtMqttStatusUi(state, error));
        extMqtt.addStateListener(extMqttStateListener);
        updateExtMqttStatusUi(extMqtt.getState(), null);
    }

    private void toggleExtMqttConnection(ExternalSessionManager extMqtt) {
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

    private void updateExtMqttStatusUi(MqttClientManager.State state, String error) {
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

    private void saveExtMqttClientSettings() {
        String host = editExtMqttHost.getText().toString().trim();
        String portText = editExtMqttPort.getText().toString().trim();
        if (host.isEmpty() || portText.isEmpty()) return;
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) return;
            preferences.saveExtMqttSettings(host, port, preferences.isExtMqttEnabled());
        } catch (NumberFormatException ignored) {}
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
