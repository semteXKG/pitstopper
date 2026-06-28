package at.semmal.pitstopper.ui;

public enum TelemetrySensor {
    EMPTY, RPM, SPEED, THROTTLE_BRAKE, COOLANT, OIL_TEMP, OIL_PRES, BATTERY,
    TYRE_FL, TYRE_FR, TYRE_RL, TYRE_RR,
    THERMAL_FL, THERMAL_FR, THERMAL_RL, THERMAL_RR;

    public static TelemetrySensor fromString(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) {
            return EMPTY;
        }
    }
}
