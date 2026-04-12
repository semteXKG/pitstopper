package at.semmal.pitstopper.ui;

public enum TelemetrySensor {
    EMPTY, RPM, SPEED, THROTTLE_BRAKE, COOLANT, OIL_TEMP, OIL_PRES, BATTERY;

    public static TelemetrySensor fromString(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) {
            return EMPTY;
        }
    }
}
