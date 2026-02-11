package at.semmal.pitstopper;

/**
 * Data model for a SpeedHive car/competitor.
 */
public class SpeedHiveCar {

    private final String number;
    private final String driverName;

    public SpeedHiveCar(String number, String driverName) {
        this.number = number;
        this.driverName = driverName;
    }

    public String getNumber() { return number; }
    public String getDriverName() { return driverName; }

    public String getDisplayName() {
        if (driverName != null && !driverName.isEmpty() && !driverName.equals("Unknown")) {
            return "#" + number + " - " + driverName;
        }
        return "#" + number;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}