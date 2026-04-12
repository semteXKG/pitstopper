package at.semmal.pitstopper.ui;

public enum TelemetryLayout {
    LAYOUT_1_2_4("1_2_4", 7),
    LAYOUT_2_4("2_4", 6);

    public final String key;
    public final int slotCount;

    TelemetryLayout(String key, int slotCount) {
        this.key = key;
        this.slotCount = slotCount;
    }

    public static TelemetryLayout fromKey(String key) {
        for (TelemetryLayout l : values()) {
            if (l.key.equals(key)) return l;
        }
        return LAYOUT_1_2_4;
    }
}
