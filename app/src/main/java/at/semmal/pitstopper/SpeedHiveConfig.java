package at.semmal.pitstopper;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader for SpeedHive API credentials and settings.
 * Loads from speedhive.properties file in assets (gitignored for security).
 */
public class SpeedHiveConfig {
    
    private static final String TAG = "SpeedHiveConfig";
    private static final String PROPERTIES_FILE = "speedhive.properties";
    
    private final String baseUrl;
    private final String apiKey;
    private final String userAgent;
    
    /**
     * Load SpeedHive configuration from assets/speedhive.properties
     * 
     * @param context Application context for accessing assets
     * @throws IllegalStateException if properties file cannot be loaded or is missing required fields
     */
    public SpeedHiveConfig(Context context) {
        Properties props = new Properties();
        
        try {
            AssetManager assets = context.getAssets();
            InputStream inputStream = assets.open(PROPERTIES_FILE);
            props.load(inputStream);
            inputStream.close();
            
            Log.i(TAG, "SpeedHive configuration loaded successfully");
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to load " + PROPERTIES_FILE + " from assets", e);
            throw new IllegalStateException("SpeedHive configuration file not found. " +
                    "Copy speedhive.properties.example to speedhive.properties and configure with real values.", e);
        }
        
        // Extract required properties with validation
        baseUrl = getRequiredProperty(props, "speedhive.baseUrl");
        apiKey = getRequiredProperty(props, "speedhive.apiKey");
        userAgent = getRequiredProperty(props, "speedhive.userAgent");
        
        // Validate that we don't have placeholder values
        if ("YOUR_API_KEY_HERE".equals(apiKey)) {
            throw new IllegalStateException("SpeedHive API key not configured. " +
                    "Please update speedhive.properties with the real API key.");
        }
        
        Log.i(TAG, "SpeedHive config - Base URL: " + baseUrl + ", User-Agent: " + userAgent);
    }
    
    /**
     * Get a required property from the Properties object, throwing if missing.
     */
    private String getRequiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Required SpeedHive configuration property missing: " + key);
        }
        return value.trim();
    }
    
    /**
     * Get the SpeedHive API base URL.
     * @return Base URL (e.g., "https://lt-api.speedhive.com/api")
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Get the SpeedHive API key.
     * @return API key for authentication
     */
    public String getApiKey() {
        return apiKey;
    }
    
    /**
     * Get the User-Agent string for HTTP requests.
     * @return User-Agent (e.g., "speedhive-android/1.68")
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Build the full URL for an API endpoint.
     * @param endpoint Relative endpoint (e.g., "/events/123/sessions/456/data")
     * @return Full URL
     */
    public String buildUrl(String endpoint) {
        if (endpoint.startsWith("/")) {
            return baseUrl + endpoint;
        } else {
            return baseUrl + "/" + endpoint;
        }
    }
}