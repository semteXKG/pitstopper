package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private PitWindowPreferences preferences;
    private TextView summaryRace, summarySpeedHive, summaryTelemetry, summaryMqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_hub);
        hideSystemUI();

        preferences = new PitWindowPreferences(this);

        summaryRace = findViewById(R.id.summaryRace);
        summarySpeedHive = findViewById(R.id.summarySpeedHive);
        summaryTelemetry = findViewById(R.id.summaryTelemetry);
        summaryMqtt = findViewById(R.id.summaryMqtt);

        findViewById(R.id.rowRace).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsRaceActivity.class)));
        findViewById(R.id.rowSpeedHive).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsSpeedHiveActivity.class)));
        findViewById(R.id.rowTelemetry).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsTelemetryActivity.class)));
        findViewById(R.id.rowMqtt).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsMqttActivity.class)));

        refreshSummaries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSummaries();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void refreshSummaries() {
        // Race summary
        String raceTime = String.format(Locale.getDefault(), "%02d:%02d",
                preferences.getRaceStartHour(), preferences.getRaceStartMinute());
        summaryRace.setText(String.format(Locale.getDefault(),
                "%s • %dmin window • %dmin duration",
                raceTime, preferences.getPitWindowOpens(), preferences.getPitWindowDuration()));

        // SpeedHive summary
        String mode = preferences.getSpeedHiveMode();
        if (PitWindowPreferences.SPEEDHIVE_MODE_SPEEDHIVE.equals(mode)) {
            String event = preferences.getSpeedHiveEventName();
            String car = preferences.getSpeedHiveCarNumber();
            summarySpeedHive.setText(String.format("API • %s • Car #%s",
                    event.isEmpty() ? "No event" : event,
                    car.isEmpty() ? "?" : car));
        } else if (PitWindowPreferences.SPEEDHIVE_MODE_DEMO.equals(mode)) {
            summarySpeedHive.setText(String.format("Demo • Car #%s",
                    preferences.getSpeedHiveCarNumber()));
        } else {
            summarySpeedHive.setText("Off");
        }

        // Telemetry summary
        int alarms = 0;
        if (preferences.isCoolantAlarm()) alarms++;
        if (preferences.isOilTempAlarm()) alarms++;
        if (preferences.isOilPresAlarm()) alarms++;
        if (preferences.isBatteryAlarm()) alarms++;
        summaryTelemetry.setText(String.format(Locale.getDefault(),
                "%d/4 alarms active", alarms));

        // MQTT summary
        MqttClientManager mqttClient = ((PitStopperApplication) getApplication()).getMqttClientManager();
        ExternalSessionManager extMqtt = ((PitStopperApplication) getApplication()).getExternalSessionManager();
        String localState = mqttClient.getState().name().toLowerCase(Locale.ROOT);
        String extState = extMqtt.getState().name().toLowerCase(Locale.ROOT);
        summaryMqtt.setText(String.format("Local: %s • Public: %s", localState, extState));
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
