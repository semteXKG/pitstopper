package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SettingsTelemetryActivity extends AppCompatActivity {

    private EditText editCoolantWarn, editCoolantCrit;
    private CheckBox checkCoolantAlarm;
    private EditText editOilTempWarn, editOilTempCrit;
    private CheckBox checkOilTempAlarm;
    private EditText editOilPresWarn, editOilPresCrit;
    private CheckBox checkOilPresAlarm;
    private EditText editBatteryWarn, editBatteryCrit;
    private CheckBox checkBatteryAlarm;

    private PitWindowPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_telemetry);
        hideSystemUI();

        preferences = new PitWindowPreferences(this);

        editCoolantWarn = findViewById(R.id.editCoolantWarn);
        editCoolantCrit = findViewById(R.id.editCoolantCrit);
        checkCoolantAlarm = findViewById(R.id.checkCoolantAlarm);
        editOilTempWarn = findViewById(R.id.editOilTempWarn);
        editOilTempCrit = findViewById(R.id.editOilTempCrit);
        checkOilTempAlarm = findViewById(R.id.checkOilTempAlarm);
        editOilPresWarn = findViewById(R.id.editOilPresWarn);
        editOilPresCrit = findViewById(R.id.editOilPresCrit);
        checkOilPresAlarm = findViewById(R.id.checkOilPresAlarm);
        editBatteryWarn = findViewById(R.id.editBatteryWarn);
        editBatteryCrit = findViewById(R.id.editBatteryCrit);
        checkBatteryAlarm = findViewById(R.id.checkBatteryAlarm);

        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);

        loadSettings();

        buttonSave.setOnClickListener(v -> saveSettings());
        buttonCancel.setOnClickListener(v -> finish());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void loadSettings() {
        editCoolantWarn.setText(String.valueOf(preferences.getCoolantWarn()));
        editCoolantCrit.setText(String.valueOf(preferences.getCoolantCrit()));
        checkCoolantAlarm.setChecked(preferences.isCoolantAlarm());
        editOilTempWarn.setText(String.valueOf(preferences.getOilTempWarn()));
        editOilTempCrit.setText(String.valueOf(preferences.getOilTempCrit()));
        checkOilTempAlarm.setChecked(preferences.isOilTempAlarm());
        editOilPresWarn.setText(String.valueOf(preferences.getOilPresWarn()));
        editOilPresCrit.setText(String.valueOf(preferences.getOilPresCrit()));
        checkOilPresAlarm.setChecked(preferences.isOilPresAlarm());
        editBatteryWarn.setText(String.valueOf(preferences.getBatteryWarn()));
        editBatteryCrit.setText(String.valueOf(preferences.getBatteryCrit()));
        checkBatteryAlarm.setChecked(preferences.isBatteryAlarm());
    }

    private void saveSettings() {
        try {
            int coolantWarn = Integer.parseInt(editCoolantWarn.getText().toString().trim());
            int coolantCrit = Integer.parseInt(editCoolantCrit.getText().toString().trim());
            int oilTempWarn = Integer.parseInt(editOilTempWarn.getText().toString().trim());
            int oilTempCrit = Integer.parseInt(editOilTempCrit.getText().toString().trim());
            float oilPresWarn = Float.parseFloat(editOilPresWarn.getText().toString().trim());
            float oilPresCrit = Float.parseFloat(editOilPresCrit.getText().toString().trim());
            float batteryWarn = Float.parseFloat(editBatteryWarn.getText().toString().trim());
            float batteryCrit = Float.parseFloat(editBatteryCrit.getText().toString().trim());

            preferences.saveTelemetryAlarms(
                    coolantWarn, coolantCrit, checkCoolantAlarm.isChecked(),
                    oilTempWarn, oilTempCrit, checkOilTempAlarm.isChecked(),
                    oilPresWarn, oilPresCrit, checkOilPresAlarm.isChecked(),
                    batteryWarn, batteryCrit, checkBatteryAlarm.isChecked());

            Toast.makeText(this, "Telemetry settings saved", Toast.LENGTH_SHORT).show();
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
