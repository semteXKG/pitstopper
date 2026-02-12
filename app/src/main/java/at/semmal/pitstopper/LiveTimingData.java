package at.semmal.pitstopper;

/**
 * Data model representing live timing information for a specific car.
 * Contains position, gaps, and identification information.
 */
public class LiveTimingData {
    
    private final int position;
    private final String gapAhead;
    private final String gapBehind;
    private final String carNumber;
    private final String driverName;
    private final int totalCompetitors;
    
    /**
     * Create live timing data for a car.
     * 
     * @param position Current race position (1 = first place)
     * @param gapAhead Gap to car ahead (e.g., "2.345", "2 Laps", "LEADER")
     * @param gapBehind Gap to car behind (e.g., "1.234", "LAST")
     * @param carNumber Car/race number (e.g., "88")
     * @param driverName Driver or team name
     * @param totalCompetitors Total number of cars in the race
     */
    public LiveTimingData(int position, String gapAhead, String gapBehind, 
                         String carNumber, String driverName, int totalCompetitors) {
        this.position = position;
        this.gapAhead = gapAhead != null ? gapAhead : "";
        this.gapBehind = gapBehind != null ? gapBehind : "";
        this.carNumber = carNumber != null ? carNumber : "";
        this.driverName = driverName != null ? driverName : "";
        this.totalCompetitors = totalCompetitors;
    }
    
    /**
     * @return Current race position (1-based)
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * @return Gap to car ahead ("2.345", "2 Laps", "LEADER", etc.)
     */
    public String getGapAhead() {
        return gapAhead;
    }
    
    /**
     * @return Gap to car behind ("1.234", "LAST", etc.)
     */
    public String getGapBehind() {
        return gapBehind;
    }
    
    /**
     * @return Car number/race number (e.g., "88")
     */
    public String getCarNumber() {
        return carNumber;
    }
    
    /**
     * @return Driver or team name
     */
    public String getDriverName() {
        return driverName;
    }
    
    /**
     * @return Total number of competitors in the race
     */
    public int getTotalCompetitors() {
        return totalCompetitors;
    }
    
    /**
     * Check if this car is leading the race.
     * @return true if position is 1
     */
    public boolean isLeader() {
        return position == 1;
    }
    
    /**
     * Check if this car is in last place.
     * @return true if position equals total competitors
     */
    public boolean isLast() {
        return position == totalCompetitors;
    }
    
    /**
     * Format position as "P1", "P2", etc.
     * @return Formatted position string
     */
    public String getFormattedPosition() {
        return "P" + position;
    }
    
    /**
     * Format position with total count as "P1/8", "P2/8", etc.
     * @return Formatted position string with total
     */
    public String getFormattedPositionWithTotal() {
        return "P" + position + "/" + totalCompetitors;
    }
    
    @Override
    public String toString() {
        return String.format("LiveTimingData{pos=%d/%d, car=%s, driver='%s', ahead='%s', behind='%s'}",
                position, totalCompetitors, carNumber, driverName, gapAhead, gapBehind);
    }
}