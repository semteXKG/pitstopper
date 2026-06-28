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

    // fiesta/tpms/{fl|fr|rl|rr}
    private Float   tyrePresFL = null, tyrePresFR = null, tyrePresRL = null, tyrePresRR = null;
    private int     tyreTempFL = Integer.MIN_VALUE, tyreTempFR = Integer.MIN_VALUE,
                    tyreTempRL = Integer.MIN_VALUE, tyreTempRR = Integer.MIN_VALUE;
    private boolean tyreAlarmFL, tyreAlarmFR, tyreAlarmRL, tyreAlarmRR;

    // fiesta/tire-temp/{FL|FR|RL|RR} — MLX90640 thermal camera zones
    private float   thermalTaFL = Float.NaN, thermalOutsideFL = Float.NaN,
                    thermalCenterFL = Float.NaN, thermalInsideFL = Float.NaN;
    private boolean thermalDetectedFL;
    private int     thermalPixelsFL;

    private float   thermalTaFR = Float.NaN, thermalOutsideFR = Float.NaN,
                    thermalCenterFR = Float.NaN, thermalInsideFR = Float.NaN;
    private boolean thermalDetectedFR;
    private int     thermalPixelsFR;

    private float   thermalTaRL = Float.NaN, thermalOutsideRL = Float.NaN,
                    thermalCenterRL = Float.NaN, thermalInsideRL = Float.NaN;
    private boolean thermalDetectedRL;
    private int     thermalPixelsRL;

    private float   thermalTaRR = Float.NaN, thermalOutsideRR = Float.NaN,
                    thermalCenterRR = Float.NaN, thermalInsideRR = Float.NaN;
    private boolean thermalDetectedRR;
    private int     thermalPixelsRR;

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

    // --- fiesta/tpms ---

    public void setTyre(String pos, Float presBar, int tempC, boolean alarm) {
        switch (pos) {
            case "fl": tyrePresFL = presBar; tyreTempFL = tempC; tyreAlarmFL = alarm; break;
            case "fr": tyrePresFR = presBar; tyreTempFR = tempC; tyreAlarmFR = alarm; break;
            case "rl": tyrePresRL = presBar; tyreTempRL = tempC; tyreAlarmRL = alarm; break;
            case "rr": tyrePresRR = presBar; tyreTempRR = tempC; tyreAlarmRR = alarm; break;
        }
    }

    public void setThermal(String pos, float ta, float outside, float center,
                           float inside, boolean detected, int pixels) {
        switch (pos) {
            case "FL":
                thermalTaFL = ta; thermalOutsideFL = outside;
                thermalCenterFL = center; thermalInsideFL = inside;
                thermalDetectedFL = detected; thermalPixelsFL = pixels;
                break;
            case "FR":
                thermalTaFR = ta; thermalOutsideFR = outside;
                thermalCenterFR = center; thermalInsideFR = inside;
                thermalDetectedFR = detected; thermalPixelsFR = pixels;
                break;
            case "RL":
                thermalTaRL = ta; thermalOutsideRL = outside;
                thermalCenterRL = center; thermalInsideRL = inside;
                thermalDetectedRL = detected; thermalPixelsRL = pixels;
                break;
            case "RR":
                thermalTaRR = ta; thermalOutsideRR = outside;
                thermalCenterRR = center; thermalInsideRR = inside;
                thermalDetectedRR = detected; thermalPixelsRR = pixels;
                break;
        }
    }

    public Float   getTyprePresFL() { return tyrePresFL; }
    public int     getTyreTempFL()  { return tyreTempFL; }
    public boolean getTyreAlarmFL() { return tyreAlarmFL; }
    public boolean hasTyreFL()      { return tyreTempFL != Integer.MIN_VALUE; }

    public Float   getTyprePresFR() { return tyrePresFR; }
    public int     getTyreTempFR()  { return tyreTempFR; }
    public boolean getTyreAlarmFR() { return tyreAlarmFR; }
    public boolean hasTyreFR()      { return tyreTempFR != Integer.MIN_VALUE; }

    public Float   getTyprePresRL() { return tyrePresRL; }
    public int     getTyreTempRL()  { return tyreTempRL; }
    public boolean getTyreAlarmRL() { return tyreAlarmRL; }
    public boolean hasTyreRL()      { return tyreTempRL != Integer.MIN_VALUE; }

    public Float   getTyprePresRR() { return tyrePresRR; }
    public int     getTyreTempRR()  { return tyreTempRR; }
    public boolean getTyreAlarmRR() { return tyreAlarmRR; }
    public boolean hasTyreRR()      { return tyreTempRR != Integer.MIN_VALUE; }

    public float   getThermalTaFL()        { return thermalTaFL; }
    public float   getThermalOutsideFL()   { return thermalOutsideFL; }
    public float   getThermalCenterFL()    { return thermalCenterFL; }
    public float   getThermalInsideFL()    { return thermalInsideFL; }
    public boolean getThermalDetectedFL()  { return thermalDetectedFL; }
    public int     getThermalPixelsFL()    { return thermalPixelsFL; }
    public boolean hasThermalFL()          { return !Float.isNaN(thermalOutsideFL)
                                                  || !Float.isNaN(thermalCenterFL)
                                                  || !Float.isNaN(thermalInsideFL); }
    public float   getThermalMaxFL()       { return Math.max(Math.max(thermalOutsideFL, thermalCenterFL), thermalInsideFL); }

    public float   getThermalTaFR()        { return thermalTaFR; }
    public float   getThermalOutsideFR()   { return thermalOutsideFR; }
    public float   getThermalCenterFR()    { return thermalCenterFR; }
    public float   getThermalInsideFR()    { return thermalInsideFR; }
    public boolean getThermalDetectedFR()  { return thermalDetectedFR; }
    public int     getThermalPixelsFR()    { return thermalPixelsFR; }
    public boolean hasThermalFR()          { return !Float.isNaN(thermalOutsideFR)
                                                  || !Float.isNaN(thermalCenterFR)
                                                  || !Float.isNaN(thermalInsideFR); }
    public float   getThermalMaxFR()       { return Math.max(Math.max(thermalOutsideFR, thermalCenterFR), thermalInsideFR); }

    public float   getThermalTaRL()        { return thermalTaRL; }
    public float   getThermalOutsideRL()   { return thermalOutsideRL; }
    public float   getThermalCenterRL()    { return thermalCenterRL; }
    public float   getThermalInsideRL()    { return thermalInsideRL; }
    public boolean getThermalDetectedRL()  { return thermalDetectedRL; }
    public int     getThermalPixelsRL()    { return thermalPixelsRL; }
    public boolean hasThermalRL()          { return !Float.isNaN(thermalOutsideRL)
                                                  || !Float.isNaN(thermalCenterRL)
                                                  || !Float.isNaN(thermalInsideRL); }
    public float   getThermalMaxRL()       { return Math.max(Math.max(thermalOutsideRL, thermalCenterRL), thermalInsideRL); }

    public float   getThermalTaRR()        { return thermalTaRR; }
    public float   getThermalOutsideRR()   { return thermalOutsideRR; }
    public float   getThermalCenterRR()    { return thermalCenterRR; }
    public float   getThermalInsideRR()    { return thermalInsideRR; }
    public boolean getThermalDetectedRR()  { return thermalDetectedRR; }
    public int     getThermalPixelsRR()    { return thermalPixelsRR; }
    public boolean hasThermalRR()          { return !Float.isNaN(thermalOutsideRR)
                                                  || !Float.isNaN(thermalCenterRR)
                                                  || !Float.isNaN(thermalInsideRR); }
    public float   getThermalMaxRR()       { return Math.max(Math.max(thermalOutsideRR, thermalCenterRR), thermalInsideRR); }

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
