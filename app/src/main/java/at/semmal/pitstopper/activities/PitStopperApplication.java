package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.app.Application;
import android.util.Log;

public class PitStopperApplication extends Application {

    private static final String TAG = "PitStopperApplication";

    private MqttClientManager mqttClientManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mqttClientManager = new MqttClientManager();

        PitWindowPreferences preferences = new PitWindowPreferences(this);
        if (preferences.isMqttEnabled()) {
            Log.i(TAG, "Auto-connecting to MQTT broker on startup");
            mqttClientManager.connect(preferences.getMqttHost(), preferences.getMqttPort());
        }
    }

    public MqttClientManager getMqttClientManager() {
        return mqttClientManager;
    }
}
