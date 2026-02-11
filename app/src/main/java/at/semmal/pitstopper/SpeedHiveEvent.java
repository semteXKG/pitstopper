package at.semmal.pitstopper;

/**
 * Data model for a SpeedHive event.
 */
public class SpeedHiveEvent {

    public static final int STATUS_SCHEDULED = 100;
    public static final int STATUS_PRACTICE = 101;
    public static final int STATUS_ACTIVE = 102;
    public static final int STATUS_COMPLETED = 104;
    public static final int STATUS_CANCELLED = 107;

    private final String id;
    private final String name;
    private final String date;
    private final String country;
    private final String city;
    private final int status;
    private final String trackName;

    public SpeedHiveEvent(String id, String name, String date, String country, String city, int status, String trackName) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.country = country;
        this.city = city;
        this.status = status;
        this.trackName = trackName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public int getStatus() { return status; }
    public String getTrackName() { return trackName; }

    public boolean isLive() {
        return status == STATUS_PRACTICE || status == STATUS_ACTIVE;
    }

    public String getLocationDisplay() {
        if (city != null && !city.isEmpty() && country != null && !country.isEmpty()) {
            return city + ", " + country;
        } else if (country != null && !country.isEmpty()) {
            return country;
        }
        return "";
    }

    @Override
    public String toString() {
        return name;
    }
}
