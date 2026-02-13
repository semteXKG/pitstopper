package at.semmal.pitstopper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MqttServerService extends Service {

    private static final String TAG = "MqttServerService";
    private static final String NOTIFICATION_CHANNEL_ID = "mqtt_server_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    public static final String ACTION_START_SERVER = "START_SERVER";
    public static final String ACTION_STOP_SERVER = "STOP_SERVER";
    public static final String EXTRA_PORT = "EXTRA_PORT";

    private MqttServerManager mqttServerManager;
    private PitWindowPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        preferences = new PitWindowPreferences(this);
        mqttServerManager = new MqttServerManager(this);
        mqttServerManager.setCallback(new MqttServerManager.ServerCallback() {
            @Override
            public void onStarted(int port, String ipAddress) {
                Log.i(TAG, "MQTT server started in service");
                updateNotification("MQTT Server Running", "Port: " + port + " | IP: " + ipAddress);
                preferences.setMqttServerEnabled(true);
            }

            @Override
            public void onStopped() {
                Log.i(TAG, "MQTT server stopped in service");
                preferences.setMqttServerEnabled(false);
                stopForeground(true);
                stopSelf();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "MQTT server error in service: " + error);
                preferences.setMqttServerEnabled(false);
                updateNotification("MQTT Server Error", error);
                // Don't stop service immediately, give user chance to see error
                android.os.Handler handler = new android.os.Handler(getMainLooper());
                handler.postDelayed(() -> {
                    stopForeground(true);
                    stopSelf();
                }, 3000);
            }
        });

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);
            
            if (ACTION_START_SERVER.equals(action)) {
                int port = intent.getIntExtra(EXTRA_PORT, 1883);
                startMqttServer(port);
            } else if (ACTION_STOP_SERVER.equals(action)) {
                stopMqttServer();
            }
        }
        
        // START_STICKY ensures service restarts if killed by system
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This service doesn't support binding
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (mqttServerManager != null) {
            mqttServerManager.shutdown();
        }
    }

    private void startMqttServer(int port) {
        Log.i(TAG, "Starting MQTT server on port " + port);
        
        // Start as foreground service immediately
        startForeground(NOTIFICATION_ID, 
            createNotification("MQTT Server Starting...", "Port: " + port));
        
        mqttServerManager.startServer(port);
    }

    private void stopMqttServer() {
        Log.i(TAG, "Stopping MQTT server");
        mqttServerManager.stopServer();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MQTT Server",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("MQTT server status notifications");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String title, String content) {
        // Intent to open the main activity when notification is tapped
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent to stop the server
        Intent stopIntent = new Intent(this, MqttServerService.class);
        stopIntent.setAction(ACTION_STOP_SERVER);
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // Use system icon for now
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)  // Cannot be dismissed by swipe
            .setAutoCancel(false)
            .build();
    }

    private void updateNotification(String title, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, createNotification(title, content));
            }
        }
    }

    /**
     * Helper methods for external classes to start/stop the service
     */
    public static void startMqttServer(Context context, int port) {
        Intent intent = new Intent(context, MqttServerService.class);
        intent.setAction(ACTION_START_SERVER);
        intent.putExtra(EXTRA_PORT, port);
        context.startForegroundService(intent);
    }

    public static void stopMqttServer(Context context) {
        Intent intent = new Intent(context, MqttServerService.class);
        intent.setAction(ACTION_STOP_SERVER);
        context.startService(intent);
    }
}