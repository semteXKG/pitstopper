package at.semmal.pitstopper.activities;

import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.mqtt.ExternalSessionManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

import android.app.Application;
import android.util.Log;

public class PitStopperApplication extends Application {

    private static final String TAG = "PitStopperApplication";

    private MqttClientManager mqttClientManager;
    private ExternalSessionManager externalSessionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mqttClientManager = new MqttClientManager();
        externalSessionManager = new ExternalSessionManager();

        PitWindowPreferences preferences = new PitWindowPreferences(this);
        if (preferences.isMqttEnabled()) {
            Log.i(TAG, "Auto-connecting to MQTT broker on startup");
            mqttClientManager.connect(preferences.getMqttHost(), preferences.getMqttPort());
        }

        String sessionId = preferences.getSessionId();
        if (sessionId != null) {
            Log.i(TAG, "Restoring external session: " + sessionId.substring(0, 8) + "...");
            externalSessionManager.connect(sessionId);
        }
    }

    public MqttClientManager getMqttClientManager() {
        return mqttClientManager;
    }

    public ExternalSessionManager getExternalSessionManager() {
        return externalSessionManager;
    }
}
