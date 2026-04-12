package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.mqtt.TelemetryAlertTracker;
import at.semmal.pitstopper.timing.PitWindowPreferences;
import at.semmal.pitstopper.ui.TelemetryLayout;
import at.semmal.pitstopper.ui.TelemetrySensor;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SettingsTelemetryActivity extends AppCompatActivity {

    private EditText editRpmWarn, editRpmCrit;
    private CheckBox checkRpmAlarm;
    private EditText editCoolantWarn, editCoolantCrit;
    private CheckBox checkCoolantAlarm;
    private EditText editOilTempWarn, editOilTempCrit;
    private CheckBox checkOilTempAlarm;
    private EditText editOilPresWarn, editOilPresCrit;
    private CheckBox checkOilPresAlarm;
    private EditText editBatteryWarn, editBatteryCrit;
    private CheckBox checkBatteryAlarm;

    private RadioButton radioLayout124, radioLayout24;
    private LinearLayout layoutPreview;

    // Current in-memory slot assignments (edited live, saved on Save)
    private TelemetrySensor[] currentSlots;

    private PitWindowPreferences preferences;

    // Tier structure per layout: slot counts top → bottom
    private static final int[] TIERS_1_2_4 = {1, 2, 4};
    private static final int[] TIERS_2_4   = {2, 4};

    // Short label shown inside each tile
    private static final String[] SENSOR_TILE_LABELS = {
        "—", "RPM", "Speed", "THR+BRK", "Coolant", "Oil T", "Oil P", "Battery"
    };

    // Full name shown in the picker dialog
    private static final String[] SENSOR_DIALOG_LABELS = {
        "— Empty —", "RPM", "Speed (km/h)", "Throttle + Brake",
        "Coolant (°C)", "Oil Temp (°C)", "Oil Pressure (bar)", "Battery (V)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_telemetry);
        hideSystemUI();

        preferences = new PitWindowPreferences(this);

        editRpmWarn = findViewById(R.id.editRpmWarn);
        editRpmCrit = findViewById(R.id.editRpmCrit);
        checkRpmAlarm = findViewById(R.id.checkRpmAlarm);
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

        radioLayout124 = findViewById(R.id.radioLayout124);
        radioLayout24 = findViewById(R.id.radioLayout24);
        layoutPreview = findViewById(R.id.layoutPreview);

        radioLayout124.setOnClickListener(v -> switchLayout());
        radioLayout24.setOnClickListener(v -> switchLayout());

        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);
        buttonSave.setOnClickListener(v -> saveSettings());
        buttonCancel.setOnClickListener(v -> finish());

        loadSettings();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void loadSettings() {
        editRpmWarn.setText(String.valueOf(preferences.getRpmWarn()));
        editRpmCrit.setText(String.valueOf(preferences.getRpmCrit()));
        checkRpmAlarm.setChecked(preferences.isRpmAlarm());
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

        TelemetryLayout layout = preferences.getTelemetryLayout();
        if (layout == TelemetryLayout.LAYOUT_2_4) {
            radioLayout24.setChecked(true);
        } else {
            radioLayout124.setChecked(true);
        }

        currentSlots = preferences.getSlotSensors(layout);
        buildPreview();
    }

    /** Called when the user taps a layout radio button. */
    private void switchLayout() {
        TelemetryLayout layout = getSelectedLayout();
        currentSlots = preferences.getSlotSensors(layout);
        buildPreview();
    }

    private void buildPreview() {
        layoutPreview.removeAllViews();

        int[] tiers = getSelectedLayout() == TelemetryLayout.LAYOUT_2_4 ? TIERS_2_4 : TIERS_1_2_4;
        int slot = 0;

        for (int tierIdx = 0; tierIdx < tiers.length; tierIdx++) {
            int slotCount = tiers[tierIdx];
            LinearLayout tierRow = new LinearLayout(this);
            tierRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams tierParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            if (tierIdx > 0) tierParams.topMargin = 4;
            tierRow.setLayoutParams(tierParams);

            for (int s = 0; s < slotCount; s++) {
                final int slotIndex = slot++;
                TextView cell = new TextView(this);
                cell.setText(SENSOR_TILE_LABELS[currentSlots[slotIndex].ordinal()]);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(0xFFFFFFFF);
                cell.setTextSize(15f);
                cell.setBackgroundColor(0xFF2A2A2A);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                if (s > 0) cellParams.leftMargin = 4;
                cell.setLayoutParams(cellParams);

                cell.setOnClickListener(v -> showSensorPicker(slotIndex, cell));

                tierRow.addView(cell);
            }
            layoutPreview.addView(tierRow);
        }
    }

    private void showSensorPicker(int slotIndex, TextView cell) {
        new AlertDialog.Builder(this)
                .setTitle("Slot " + (slotIndex + 1))
                .setItems(SENSOR_DIALOG_LABELS, (dialog, which) -> {
                    currentSlots[slotIndex] = TelemetrySensor.values()[which];
                    cell.setText(SENSOR_TILE_LABELS[which]);
                })
                .show();
    }

    private TelemetryLayout getSelectedLayout() {
        return radioLayout24.isChecked() ? TelemetryLayout.LAYOUT_2_4 : TelemetryLayout.LAYOUT_1_2_4;
    }

    private void saveSettings() {
        try {
            int rpmWarn = Integer.parseInt(editRpmWarn.getText().toString().trim());
            int rpmCrit = Integer.parseInt(editRpmCrit.getText().toString().trim());
            int coolantWarn = Integer.parseInt(editCoolantWarn.getText().toString().trim());
            int coolantCrit = Integer.parseInt(editCoolantCrit.getText().toString().trim());
            int oilTempWarn = Integer.parseInt(editOilTempWarn.getText().toString().trim());
            int oilTempCrit = Integer.parseInt(editOilTempCrit.getText().toString().trim());
            float oilPresWarn = Float.parseFloat(editOilPresWarn.getText().toString().trim());
            float oilPresCrit = Float.parseFloat(editOilPresCrit.getText().toString().trim());
            float batteryWarn = Float.parseFloat(editBatteryWarn.getText().toString().trim());
            float batteryCrit = Float.parseFloat(editBatteryCrit.getText().toString().trim());

            preferences.saveTelemetryAlarms(
                    rpmWarn, rpmCrit, checkRpmAlarm.isChecked(),
                    coolantWarn, coolantCrit, checkCoolantAlarm.isChecked(),
                    oilTempWarn, oilTempCrit, checkOilTempAlarm.isChecked(),
                    oilPresWarn, oilPresCrit, checkOilPresAlarm.isChecked(),
                    batteryWarn, batteryCrit, checkBatteryAlarm.isChecked());

            TelemetryAlertTracker tracker =
                    ((PitStopperApplication) getApplication()).getTelemetryAlertTracker();
            tracker.reloadThresholds(preferences);

            preferences.saveLayoutConfig(getSelectedLayout(), currentSlots);

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
