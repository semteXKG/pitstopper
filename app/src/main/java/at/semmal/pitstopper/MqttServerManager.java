package at.semmal.pitstopper;

import android.content.Context;
import android.util.Log;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Properties;

public class MqttServerManager {

    private static final String TAG = "MqttServerManager";

    public interface ServerCallback {
        void onStarted(int port, String ipAddress);
        void onStopped();
        void onError(String error);
    }

    private final Context context;
    private Server mqttServer;
    private boolean isServerRunning = false;
    private int currentPort = 1883;
    private ServerCallback callback;

    public MqttServerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setCallback(ServerCallback callback) {
        this.callback = callback;
    }

    public boolean isRunning() {
        return isServerRunning;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    /**
     * Start the MQTT server on the specified port
     */
    public void startServer(int port) {
        if (isServerRunning) {
            Log.w(TAG, "MQTT server already running on port " + currentPort);
            return;
        }

        currentPort = port;
        
        try {
            // Create configuration with proper Android app directory paths
            Properties configProps = new Properties();
            
            // Basic server settings
            configProps.setProperty("port", String.valueOf(port));
            configProps.setProperty("host", "0.0.0.0");  // Bind to all interfaces
            configProps.setProperty("allow_anonymous", "true");
            
            // Set persistence store to app's internal files directory
            String persistenceDir = context.getFilesDir().getAbsolutePath() + "/mqtt";
            configProps.setProperty("persistent_store", persistenceDir);
            
            // Disable SSL and WebSocket ports
            configProps.setProperty("ssl_port", "0");
            configProps.setProperty("websocket_port", "0");
            
            IConfig config = new MemoryConfig(configProps);
            
            // Create and start server
            mqttServer = new Server();
            mqttServer.startServer(config);
            
            isServerRunning = true;
            String ipAddress = getLocalIpAddress();
            Log.i(TAG, "MQTT server started on " + ipAddress + ":" + port);
            
            if (callback != null) {
                callback.onStarted(port, ipAddress);
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start MQTT server", e);
            isServerRunning = false;
            if (callback != null) {
                callback.onError("Failed to start server: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting MQTT server", e);
            isServerRunning = false;
            if (callback != null) {
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }
    }

    /**
     * Stop the MQTT server
     */
    public void stopServer() {
        if (!isServerRunning || mqttServer == null) {
            Log.w(TAG, "MQTT server not running");
            isServerRunning = false;
            if (callback != null) {
                callback.onStopped();
            }
            return;
        }

        try {
            mqttServer.stopServer();
            mqttServer = null;
            isServerRunning = false;
            Log.i(TAG, "MQTT server stopped");
            
            if (callback != null) {
                callback.onStopped();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping MQTT server", e);
            mqttServer = null;
            isServerRunning = false;
            if (callback != null) {
                callback.onStopped();  // Still notify stopped even if there was an error
            }
        }
    }

    /**
     * Get the local IP address for clients to connect to
     */
    private String getLocalIpAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!intf.isUp() || intf.isLoopback()) continue;
                
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    if (addr.isSiteLocalAddress()) {
                        String ip = addr.getHostAddress();
                        // Prefer IPv4
                        if (ip != null && ip.indexOf(':') < 0) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get local IP address", e);
        }
        
        return "localhost";  // Fallback
    }

    /**
     * Get current server status information
     */
    public String getServerInfo() {
        if (isServerRunning) {
            String ipAddress = getLocalIpAddress();
            return ipAddress + ":" + currentPort;
        } else {
            return "Server not running";
        }
    }

    /**
     * Validate if a port number is acceptable for MQTT server
     */
    public static boolean isValidPort(int port) {
        // Avoid well-known ports below 1024, use range 1024-65535
        return port >= 1024 && port <= 65535;
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        stopServer();
        callback = null;
    }
}