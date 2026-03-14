package at.semmal.pitstopper.model;

/**
 * Mutable state holder for live telemetry data aggregated from multiple
 * MQTT topics (fiesta/sensors, fiesta/can/*).
 * <p>
 * Integer.MIN_VALUE / Float.NaN sentinel values indicate "no data received yet".
 */
public class TelemetryData {

    // CAN 0x201 (50 Hz)
    private int rpm = Integer.MIN_VALUE;
    private float speedKmh = Float.NaN;
    private float throttlePct = Float.NaN;

    // CAN 0x360 / 0x420 (100 Hz / 10 Hz)
    private String brakePedal = null; // "off", "touch", "pressed"

    // CAN 0x420 (10 Hz)
    private int coolantC = Integer.MIN_VALUE;

    // fiesta/sensors (periodic ADC)
    private int oilTemp = Integer.MIN_VALUE;
    private float oilPres = Float.NaN;

    // CAN 0x428 (10 Hz)
    private float batteryV = Float.NaN;

    // --- CAN 0x201 ---

    public int getRpm()              { return rpm; }
    public float getSpeedKmh()       { return speedKmh; }
    public float getThrottlePct()    { return throttlePct; }

    public void setCan201(int rpm, float speedKmh, float throttlePct) {
        this.rpm = rpm;
        this.speedKmh = speedKmh;
        this.throttlePct = throttlePct;
    }

    // --- CAN 0x360 ---

    public String getBrakePedal()    { return brakePedal; }

    public void setBrakePedal(String brakePedal) {
        this.brakePedal = brakePedal;
    }

    // --- CAN 0x420 ---

    public int getCoolantC()         { return coolantC; }

    public void setCan420(int coolantC, String brakePedal) {
        this.coolantC = coolantC;
        this.brakePedal = brakePedal;
    }

    // --- fiesta/sensors ---

    public int getOilTemp()          { return oilTemp; }
    public float getOilPres()        { return oilPres; }

    public void setSensors(int oilTemp, float oilPres) {
        this.oilTemp = oilTemp;
        this.oilPres = oilPres;
    }

    // --- CAN 0x428 ---

    public float getBatteryV()       { return batteryV; }

    public void setBatteryV(float batteryV) {
        this.batteryV = batteryV;
    }

    // --- Helpers ---

    public boolean hasRpm()      { return rpm != Integer.MIN_VALUE; }
    public boolean hasSpeed()    { return !Float.isNaN(speedKmh); }
    public boolean hasThrottle() { return !Float.isNaN(throttlePct); }
    public boolean hasBrake()    { return brakePedal != null; }
    public boolean hasCoolant()  { return coolantC != Integer.MIN_VALUE; }
    public boolean hasOilTemp()  { return oilTemp != Integer.MIN_VALUE; }
    public boolean hasOilPres()  { return !Float.isNaN(oilPres); }
    public boolean hasBattery()  { return !Float.isNaN(batteryV); }
}
