package at.semmal.pitstopper;

import android.app.TimePickerDialog;
import android.os.Bundle;
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
    private Button buttonSave;
    private Button buttonCancel;
    
    // SpeedHive UI elements
    private Spinner spinnerSpeedHiveMode;
    private TextView labelEventId, labelSessionId, labelCarNumber;
    private Spinner spinnerEventId, spinnerSessionId, spinnerCarNumber;
    private FrameLayout frameEventSpinner, frameSessionSpinner, frameCarSpinner;
    private ProgressBar progressEvents, progressSessions, progressCars;
    private EditText editCarNumber; // For demo mode

    // SpeedHive data
    private SpeedHiveManager speedHiveManager;
    private List<SpeedHiveEvent> loadedEvents;
    private List<SpeedHiveSession> loadedSessions;
    private List<SpeedHiveCar> loadedCars;
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
        spinnerCarNumber = findViewById(R.id.spinnerCarNumber);
        frameCarSpinner = findViewById(R.id.frameCarSpinner);
        progressCars = findViewById(R.id.progressCars);
        editCarNumber = findViewById(R.id.editCarNumber);

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
        
        // Load SpeedHive settings AFTER spinners are set up
        loadSpeedHiveSettings();

        // Set up time picker button
        buttonSelectTime.setOnClickListener(v -> showTimePicker());

        // Set up button listeners
        buttonSave.setOnClickListener(v -> saveSettings());
        buttonCancel.setOnClickListener(v -> finish());
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
        
        // Load car number for demo mode
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
            frameCarSpinner.setVisibility(View.GONE);
            editCarNumber.setVisibility(View.GONE);
        } else if (modeSelection == 1) {
            // SpeedHive mode - show all fields with spinner for car
            labelEventId.setVisibility(View.VISIBLE);
            frameEventSpinner.setVisibility(View.VISIBLE);
            labelSessionId.setVisibility(View.VISIBLE);
            frameSessionSpinner.setVisibility(View.VISIBLE);
            labelCarNumber.setVisibility(View.VISIBLE);
            frameCarSpinner.setVisibility(View.VISIBLE);
            editCarNumber.setVisibility(View.GONE);
        } else if (modeSelection == 2) {
            // Demo mode - show only car number with Spinner (demo data)
            labelEventId.setVisibility(View.GONE);
            frameEventSpinner.setVisibility(View.GONE);
            labelSessionId.setVisibility(View.GONE);
            frameSessionSpinner.setVisibility(View.GONE);
            labelCarNumber.setVisibility(View.VISIBLE);
            frameCarSpinner.setVisibility(View.VISIBLE);
            editCarNumber.setVisibility(View.GONE);
            
            // Load demo cars
            loadDemoCars();
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

                    // Pre-select saved session
                    int selectedSessionIndex = -1;
                    for (int i = 0; i < sessions.size(); i++) {
                        if (sessions.get(i).getId().equals(savedSessionId)) {
                            spinnerSessionId.setSelection(i);
                            selectedSessionIndex = i;
                            break;
                        }
                    }
                    
                    // If we found and selected a saved session, load its cars
                    if (selectedSessionIndex >= 0 && loadedEvents != null) {
                        SpeedHiveEvent selectedEvent = getSelectedEvent();
                        if (selectedEvent != null) {
                            SpeedHiveSession selectedSession = sessions.get(selectedSessionIndex);
                            loadCars(selectedEvent.getId(), selectedSession.getId());
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
                if (loadedSessions != null && position < loadedSessions.size()) {
                    SpeedHiveSession session = loadedSessions.get(position);
                    SpeedHiveEvent selectedEvent = getSelectedEvent();
                    if (selectedEvent != null) {
                        loadCars(selectedEvent.getId(), session.getId());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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

    private void loadCars(String eventId, String sessionId) {
        progressCars.setVisibility(View.VISIBLE);
        spinnerCarNumber.setVisibility(View.INVISIBLE);

        getSpeedHiveManager().fetchCars(eventId, sessionId, new SpeedHiveManager.CarsCallback() {
            @Override
            public void onSuccess(List<SpeedHiveCar> cars) {
                runOnUiThread(() -> {
                    loadedCars = cars;
                    progressCars.setVisibility(View.GONE);
                    spinnerCarNumber.setVisibility(View.VISIBLE);

                    CarSpinnerAdapter adapter = new CarSpinnerAdapter(SettingsActivity.this, cars);
                    spinnerCarNumber.setAdapter(adapter);

                    // Pre-select saved car
                    for (int i = 0; i < cars.size(); i++) {
                        if (cars.get(i).getNumber().equals(savedCarNumber)) {
                            spinnerCarNumber.setSelection(i);
                            break;
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressCars.setVisibility(View.GONE);
                    spinnerCarNumber.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Failed to load cars: " + error);
                    Toast.makeText(SettingsActivity.this, "Failed to load cars: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadDemoCars() {
        // Use the same demo car data as the actual simulator
        List<SpeedHiveCar> demoCars = new ArrayList<>();
        
        // These are the actual cars from DemoSpeedHiveManager
        String[] carNumbers = {"88", "23", "77", "42", "15", "99", "7", "33"};
        String[] driverNames = {"JOHNSON", "RACER-X", "STEALTH", "MARTINEZ", "SPEEDSTER", "PHANTOM", "ACE", "VIPER"};
        
        for (int i = 0; i < carNumbers.length; i++) {
            demoCars.add(new SpeedHiveCar(carNumbers[i], driverNames[i]));
        }
        
        loadedCars = demoCars;
        CarSpinnerAdapter adapter = new CarSpinnerAdapter(this, demoCars);
        spinnerCarNumber.setAdapter(adapter);
        
        // Pre-select saved car number
        for (int i = 0; i < demoCars.size(); i++) {
            if (demoCars.get(i).getNumber().equals(savedCarNumber)) {
                spinnerCarNumber.setSelection(i);
                break;
            }
        }
    }

    private void saveSettings() {
        try {
            int pitWindowOpens = Integer.parseInt(editPitWindowOpens.getText().toString());
            int pitWindowDuration = Integer.parseInt(editPitWindowDuration.getText().toString());

            // Validate inputs
            if (pitWindowOpens < 0 || pitWindowOpens > 300) {
                Toast.makeText(this, "Pit window opens time must be between 0 and 300 minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pitWindowDuration < 1 || pitWindowDuration > 60) {
                Toast.makeText(this, "Pit window duration must be between 1 and 60 minutes", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to SharedPreferences
            preferences.saveAll(raceStartHour, raceStartMinute, pitWindowOpens, pitWindowDuration);
            
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
            // SpeedHive mode - get from spinners
            // Get selected event
            int eventPos = spinnerEventId.getSelectedItemPosition();
            if (loadedEvents != null && eventPos >= 0 && eventPos < loadedEvents.size()) {
                SpeedHiveEvent event = loadedEvents.get(eventPos);
                eventId = event.getId();
                eventName = event.getName();
            }

            // Get selected session
            int sessionPos = spinnerSessionId.getSelectedItemPosition();
            if (loadedSessions != null && sessionPos >= 0 && sessionPos < loadedSessions.size()) {
                SpeedHiveSession session = loadedSessions.get(sessionPos);
                sessionId = session.getId();
                sessionName = session.getDisplayName();
            }

            // Get selected car from spinner
            int carPos = spinnerCarNumber.getSelectedItemPosition();
            if (loadedCars != null && carPos >= 0 && carPos < loadedCars.size()) {
                SpeedHiveCar car = loadedCars.get(carPos);
                carNumber = car.getNumber();
                carName = car.getDriverName();
            }
        } else if (modeSelection == 2) {
            // Demo mode - get car number from spinner (demo data)
            int carPos = spinnerCarNumber.getSelectedItemPosition();
            if (loadedCars != null && carPos >= 0 && carPos < loadedCars.size()) {
                SpeedHiveCar car = loadedCars.get(carPos);
                carNumber = car.getNumber();
                carName = car.getDriverName();
            }
        }

        preferences.saveAllSpeedHive(mode, eventId, eventName, sessionId, sessionName, carNumber, carName);
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
