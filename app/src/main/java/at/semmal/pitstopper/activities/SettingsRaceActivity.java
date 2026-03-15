package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Locale;

public class SettingsRaceActivity extends AppCompatActivity {

    private Button buttonSelectTime;
    private EditText editPitWindowOpens;
    private EditText editPitWindowDuration;
    private EditText editMinPitStop;

    private int raceStartHour = 9;
    private int raceStartMinute = 0;

    private PitWindowPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_race);
        hideSystemUI();

        preferences = new PitWindowPreferences(this);

        buttonSelectTime = findViewById(R.id.buttonSelectTime);
        editPitWindowOpens = findViewById(R.id.editPitWindowOpens);
        editPitWindowDuration = findViewById(R.id.editPitWindowDuration);
        editMinPitStop = findViewById(R.id.editMinPitStop);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);

        loadSettings();

        buttonSelectTime.setOnClickListener(v -> showTimePicker());
        buttonSave.setOnClickListener(v -> saveSettings());
        buttonCancel.setOnClickListener(v -> finish());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void loadSettings() {
        raceStartHour = preferences.getRaceStartHour();
        raceStartMinute = preferences.getRaceStartMinute();
        updateTimeButtonText();

        editPitWindowOpens.setText(String.valueOf(preferences.getPitWindowOpens()));
        editPitWindowDuration.setText(String.valueOf(preferences.getPitWindowDuration()));
        editMinPitStop.setText(String.valueOf(preferences.getMinPitStopSeconds()));
    }

    private void showTimePicker() {
        new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    raceStartHour = hourOfDay;
                    raceStartMinute = minute;
                    updateTimeButtonText();
                },
                raceStartHour, raceStartMinute, true).show();
    }

    private void updateTimeButtonText() {
        buttonSelectTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                raceStartHour, raceStartMinute));
    }

    private void saveSettings() {
        try {
            int pitWindowOpens = Integer.parseInt(editPitWindowOpens.getText().toString());
            int pitWindowDuration = Integer.parseInt(editPitWindowDuration.getText().toString());
            int minPitStop = Integer.parseInt(editMinPitStop.getText().toString());

            if (pitWindowOpens < 0 || pitWindowOpens > 300) {
                Toast.makeText(this, "Pit window opens time must be between 0 and 300 minutes",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (pitWindowDuration < 1 || pitWindowDuration > 60) {
                Toast.makeText(this, "Pit window duration must be between 1 and 60 minutes",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (minPitStop < 1 || minPitStop > 600) {
                Toast.makeText(this, "Min. pit stop must be between 1 and 600 seconds",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            preferences.saveAll(raceStartHour, raceStartMinute, pitWindowOpens, pitWindowDuration);
            preferences.saveMinPitStopSeconds(minPitStop);

            Toast.makeText(this, "Race settings saved", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
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
