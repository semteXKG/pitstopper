package at.semmal.pitstopper;

/**
 * Data model for a SpeedHive session within an event.
 */
public class SpeedHiveSession {

    private final String id;
    private final String eventId;
    private final String runName;
    private final String groupName;
    private final int laps;
    private final String bestLapTime;
    private final String raceTime;
    private final String startOfDay;
    private final int flag;
    private final boolean parentEventLive;

    public SpeedHiveSession(String id, String eventId, String runName, String groupName,
                            int laps, String bestLapTime, String raceTime, String startOfDay,
                            int flag, boolean parentEventLive) {
        this.id = id;
        this.eventId = eventId;
        this.runName = runName;
        this.groupName = groupName;
        this.laps = laps;
        this.bestLapTime = bestLapTime;
        this.raceTime = raceTime;
        this.startOfDay = startOfDay;
        this.flag = flag;
        this.parentEventLive = parentEventLive;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getRunName() { return runName; }
    public String getGroupName() { return groupName; }
    public int getLaps() { return laps; }
    public String getBestLapTime() { return bestLapTime; }
    public String getRaceTime() { return raceTime; }
    public String getStartOfDay() { return startOfDay; }
    public int getFlag() { return flag; }

    /**
     * A session is considered active/live if:
     * 1. Parent event is live (status 102)
     * 2. Session flag is 0 (active) vs 3 (finished)
     * 3. Has timing data (race time, best lap, or laps > 0)
     */
    public boolean isActive() {
        return parentEventLive 
            && flag == 0 
            && ((raceTime != null && !raceTime.isEmpty()) 
                || (bestLapTime != null && !bestLapTime.isEmpty()) 
                || laps > 0);
    }

    public String getDisplayName() {
        if (groupName != null && !groupName.isEmpty()) {
            return runName + " — " + groupName;
        }
        return runName;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
