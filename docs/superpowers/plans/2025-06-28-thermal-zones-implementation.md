# Thermal Zones Sensor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Thermal Zones" telemetry sensor type that displays MLX90640 tyre zone temperatures (outside/center/inside) from `fiesta/tire-temp/{FL|FR|RL|RR}` MQTT topics alongside existing TPMS pressure data.

**Architecture:** New `TelemetrySensor` enum values (`THERMAL_FL/FR/RL/RR`), new `TelemetryData` fields (6 per position), new `slot_thermal.xml` layout, MQTT subscription/handler in `MainActivity`, update + inflation code in `TelemetryModule`. No changes to existing TPMS flow.

**Tech Stack:** Java 11, Android SDK, HiveMQ MQTT Client, ConstraintLayout XML

## Global Constraints

- TPMS (`fiesta/tpms/*`) subscriptions and `TYRE_FL/FR/RL/RR` sensor types remain untouched
- Thermal cameras publish on topics with uppercase positions: `fiesta/tire-temp/FL` etc.
- Zone ordering: left-side (FL/RL) → `O: C: I:`, right-side (FR/RR) → `I: C: O:`
- Color scheme: 0–50°C blue, 50–75°C green, 75°C+ red; not-detected dim gray; no-data white
- Max zone temp displayed large (auto-sized), three zone temps displayed small (18sp)
- No `TelemetryAlertTracker` integration — display only
- Follow existing code patterns exactly (sentinel values, color methods, inflation)

---

### Task 1: Add THERMAL sensor enum values and settings labels

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/ui/TelemetrySensor.java`
- Modify: `app/src/main/java/at/semmal/pitstopper/activities/SettingsTelemetryActivity.java`

**Interfaces:**
- Produces: `TelemetrySensor.THERMAL_FL`, `THERMAL_FR`, `THERMAL_RL`, `THERMAL_RR` — usable in slot assignment arrays and `TelemetrySensor.values()` picker

- [ ] **Step 1: Edit `TelemetrySensor.java` to add new enum values**

```java
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
```

- [ ] **Step 2: Edit `SettingsTelemetryActivity.java` — append to `SENSOR_TILE_LABELS`**

```java
    private static final String[] SENSOR_TILE_LABELS = {
        "—", "RPM", "Speed", "THR+BRK", "Coolant", "Oil T", "Oil P", "Battery",
        "T:FL", "T:FR", "T:RL", "T:RR",
        "TH:FL", "TH:FR", "TH:RL", "TH:RR"
    };
```

- [ ] **Step 3: Edit `SettingsTelemetryActivity.java` — append to `SENSOR_DIALOG_LABELS`**

```java
    private static final String[] SENSOR_DIALOG_LABELS = {
        "— Empty —", "RPM", "Speed (km/h)", "Throttle + Brake",
        "Coolant (°C)", "Oil Temp (°C)", "Oil Pressure (bar)", "Battery (V)",
        "Tyre FL (bar + °C)", "Tyre FR (bar + °C)", "Tyre RL (bar + °C)", "Tyre RR (bar + °C)",
        "Thermal FL (zone °C)", "Thermal FR (zone °C)", "Thermal RL (zone °C)", "Thermal RR (zone °C)"
    };
```

- [ ] **Step 4: Build and verify compilation**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/ui/TelemetrySensor.java \
        app/src/main/java/at/semmal/pitstopper/activities/SettingsTelemetryActivity.java
git commit -m "feat: add THERMAL sensor enum values and settings labels"
```

---

### Task 2: Add thermal data fields to TelemetryData

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/model/TelemetryData.java`

**Interfaces:**
- Produces:
  - `setThermal(String pos, float ta, float outside, float center, float inside, boolean detected, int pixels)`
  - `getThermalTa{Pos}()`, `getThermalOutside{Pos}()`, `getThermalCenter{Pos}()`, `getThermalInside{Pos}()`, `getThermalDetected{Pos}()`, `getThermalPixels{Pos}()`
  - `hasThermal{Pos}()` — returns `true` when any zone temp is not NaN
  - `getThermalMax{Pos}()` — returns `Math.max(Math.max(outside, center), inside)`

- [ ] **Step 1: Edit `TelemetryData.java` — add field declarations after the TPMS block (after line 33)**

```java
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
```

- [ ] **Step 2: Add `setThermal()` setter after the `setTyre()` method**

```java
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
```

- [ ] **Step 3: Add getters and has-helpers after `hasTyreRR()` (after line 111)**

```java
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
```

- [ ] **Step 4: Build and verify compilation**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run tests and commit**

```bash
./gradlew test
git add app/src/main/java/at/semmal/pitstopper/model/TelemetryData.java
git commit -m "feat: add thermal zone fields to TelemetryData"
```

---

### Task 3: Create slot_thermal.xml layout

**Files:**
- Create: `app/src/main/res/layout/slot_thermal.xml`

**Interfaces:**
- Produces: Layout with IDs `slotThermalPos` (TextView), `slotThermalMax` (TextView auto-sized), `slotThermalZones` (TextView) — inflated by `TelemetryModule`

- [ ] **Step 1: Create `slot_thermal.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Position label at top (FL / FR / RL / RR) -->
    <TextView
        android:id="@+id/slotThermalPos"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:textColor="@color/telemetry_label"
        android:textSize="36sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Zone temperatures small at bottom -->
    <TextView
        android:id="@+id/slotThermalZones"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:text="--"
        android:textColor="@color/telemetry_label"
        android:textSize="18sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- Max zone temperature fills the middle (auto-size) -->
    <TextView
        android:id="@+id/slotThermalMax"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:fontFamily="monospace"
        android:gravity="center"
        android:maxLines="1"
        android:text="--"
        android:textColor="@color/text_primary"
        android:textStyle="bold"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        app:autoSizeMaxTextSize="120sp"
        app:autoSizeMinTextSize="16sp"
        app:autoSizeStepGranularity="2sp"
        app:autoSizeTextType="uniform"
        app:layout_constraintBottom_toTopOf="@id/slotThermalZones"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/slotThermalPos" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Build and verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/slot_thermal.xml
git commit -m "feat: add slot_thermal.xml layout for thermal zone sensor"
```

---

### Task 4: Add thermal colors and update Strings

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: Color resources `thermal_cold` (blue), `thermal_warm` (green), `thermal_hot` (red), `thermal_no_detect` (dim gray) — referenced by `TelemetryModule` via `R.color.*`
- Produces: String resource `thermal_zones_format` — format string for zone display line

- [ ] **Step 1: Add thermal color resources to `colors.xml`** (before closing `</resources>`)

```xml
    <!-- Thermal zone temperature colors -->
    <color name="thermal_cold">#FF3B82F6</color>
    <color name="thermal_warm">#FF22C55E</color>
    <color name="thermal_hot">#FFEF4444</color>
    <color name="thermal_no_detect">#FF6B7280</color>
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/values/colors.xml
git commit -m "feat: add thermal zone color resources"
```

---

### Task 5: Add updateThermal(), View refs, inflation, and color method to TelemetryModule

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/ui/TelemetryModule.java`

**Interfaces:**
- Consumes: `TelemetrySensor.THERMAL_FL/FR/RL/RR`, `TelemetryData.hasThermal*()`, `TelemetryData.getThermal*()`, `R.layout.slot_thermal`, `R.color.thermal_cold/warm/hot/no_detect`
- Produces: `updateThermal(String pos, float ta, float outside, float center, float inside, boolean detected, int pixels)` — public method called from `MainActivity` on UI thread

- [ ] **Step 1: Add thermal color fields to constructor**

After line 99 (`colorDim = 0xFF333333;`), add:

```java
        thermalCold = ContextCompat.getColor(context, R.color.thermal_cold);
        thermalWarm = ContextCompat.getColor(context, R.color.thermal_warm);
        thermalHot = ContextCompat.getColor(context, R.color.thermal_hot);
        thermalNoDetect = ContextCompat.getColor(context, R.color.thermal_no_detect);
```

- [ ] **Step 2: Add thermal color field declarations**

After line 46 (`private final int colorDim;`), add:

```java
    private final int thermalCold;
    private final int thermalWarm;
    private final int thermalHot;
    private final int thermalNoDetect;
```

- [ ] **Step 3: Add thermal View reference declarations**

After line 58 (`private TextView tyrePresViewRR, tyreTempViewRR;`), add:

```java
    private TextView thermalMaxViewFL, thermalZonesViewFL;
    private TextView thermalMaxViewFR, thermalZonesViewFR;
    private TextView thermalMaxViewRL, thermalZonesViewRL;
    private TextView thermalMaxViewRR, thermalZonesViewRR;
```

- [ ] **Step 4: Add `updateThermal()` method**

Insert after the `updateTpms()` method closing brace (after line 209):

```java
    /** Update thermal zone temperatures from fiesta/tire-temp/{pos}. Call on main thread. */
    public void updateThermal(String pos, float ta, float outside, float center,
                              float inside, boolean detected, int pixels) {
        data.setThermal(pos, ta, outside, center, inside, detected, pixels);
        TextView maxView, zonesView;
        switch (pos) {
            case "FL": maxView = thermalMaxViewFL; zonesView = thermalZonesViewFL; break;
            case "FR": maxView = thermalMaxViewFR; zonesView = thermalZonesViewFR; break;
            case "RL": maxView = thermalMaxViewRL; zonesView = thermalZonesViewRL; break;
            case "RR": maxView = thermalMaxViewRR; zonesView = thermalZonesViewRR; break;
            default:   return;
        }
        int color = thermalColor(detected, outside, center, inside);
        if (maxView != null) {
            if (detected) {
                float maxZone = Math.max(Math.max(outside, center), inside);
                maxView.setText(String.format(Locale.US, "%.1f°", maxZone));
            } else {
                maxView.setText("?");
            }
            maxView.setTextColor(color);
        }
        if (zonesView != null) {
            if (detected && hasAnyZone(outside, center, inside)) {
                boolean isLeft = "FL".equals(pos) || "RL".equals(pos);
                if (isLeft) {
                    zonesView.setText(String.format(Locale.US, "O:%.0f° C:%.0f° I:%.0f°",
                            outside, center, inside));
                } else {
                    zonesView.setText(String.format(Locale.US, "I:%.0f° C:%.0f° O:%.0f°",
                            inside, center, outside));
                }
            } else {
                zonesView.setText("--");
            }
            zonesView.setTextColor(color);
        }
    }

    private boolean hasAnyZone(float outside, float center, float inside) {
        return !Float.isNaN(outside) || !Float.isNaN(center) || !Float.isNaN(inside);
    }
```

- [ ] **Step 5: Add `thermalColor()` method**

Insert at the end of the color helpers section (after line 397, before the closing `}`):

```java
    /** Color for thermal zone temperatures based on max zone value. */
    private int thermalColor(boolean detected, float outside, float center, float inside) {
        if (!detected) return thermalNoDetect;
        float maxZone = Math.max(Math.max(outside, center), inside);
        if (Float.isNaN(maxZone)) return thermalNoDetect;
        if (maxZone >= 75f) return thermalHot;
        if (maxZone >= 50f) return thermalWarm;
        return thermalCold;
    }
```

- [ ] **Step 6: Add `THERMAL_FL/FR/RL/RR` cases to `inflateSlot()`**

Insert after the `TYRE_RR` case (after line 337, before `case EMPTY:`):

```java
            case THERMAL_FL:
                return inflateThermalSlot(inflater, "FL",
                        data.hasThermalFL(), data.getThermalDetectedFL(),
                        data.getThermalOutsideFL(), data.getThermalCenterFL(), data.getThermalInsideFL());
            case THERMAL_FR:
                return inflateThermalSlot(inflater, "FR",
                        data.hasThermalFR(), data.getThermalDetectedFR(),
                        data.getThermalOutsideFR(), data.getThermalCenterFR(), data.getThermalInsideFR());
            case THERMAL_RL:
                return inflateThermalSlot(inflater, "RL",
                        data.hasThermalRL(), data.getThermalDetectedRL(),
                        data.getThermalOutsideRL(), data.getThermalCenterRL(), data.getThermalInsideRL());
            case THERMAL_RR:
                return inflateThermalSlot(inflater, "RR",
                        data.hasThermalRR(), data.getThermalDetectedRR(),
                        data.getThermalOutsideRR(), data.getThermalCenterRR(), data.getThermalInsideRR());
```

- [ ] **Step 7: Add `inflateThermalSlot()` helper method**

Insert after the `inflateTyreSlot()` method (after line 364):

```java
    private View inflateThermalSlot(LayoutInflater inflater, String posKey,
            boolean hasData, boolean detected, float outside, float center, float inside) {
        View v = inflater.inflate(R.layout.slot_thermal, null);
        ((TextView) v.findViewById(R.id.slotThermalPos)).setText(posKey);
        TextView maxView = v.findViewById(R.id.slotThermalMax);
        TextView zonesView = v.findViewById(R.id.slotThermalZones);
        switch (posKey) {
            case "FL": thermalMaxViewFL = maxView; thermalZonesViewFL = zonesView; break;
            case "FR": thermalMaxViewFR = maxView; thermalZonesViewFR = zonesView; break;
            case "RL": thermalMaxViewRL = maxView; thermalZonesViewRL = zonesView; break;
            case "RR": thermalMaxViewRR = maxView; thermalZonesViewRR = zonesView; break;
        }
        if (hasData) {
            int color = thermalColor(detected, outside, center, inside);
            if (detected) {
                float maxZone = Math.max(Math.max(outside, center), inside);
                maxView.setText(String.format(Locale.US, "%.1f°", maxZone));
            } else {
                maxView.setText("?");
            }
            maxView.setTextColor(color);
            if (detected && hasAnyZone(outside, center, inside)) {
                boolean isLeft = "FL".equals(posKey) || "RL".equals(posKey);
                if (isLeft) {
                    zonesView.setText(String.format(Locale.US, "O:%.0f° C:%.0f° I:%.0f°",
                            outside, center, inside));
                } else {
                    zonesView.setText(String.format(Locale.US, "I:%.0f° C:%.0f° O:%.0f°",
                            inside, center, outside));
                }
            } else {
                zonesView.setText("--");
            }
            zonesView.setTextColor(color);
        }
        return v;
    }
```

- [ ] **Step 8: Add thermal view refs to `clearSensorRefs()`**

After line 380 (`tyrePresViewRR = null; tyreTempViewRR = null;`), add:

```java
        thermalMaxViewFL = null; thermalZonesViewFL = null;
        thermalMaxViewFR = null; thermalZonesViewFR = null;
        thermalMaxViewRL = null; thermalZonesViewRL = null;
        thermalMaxViewRR = null; thermalZonesViewRR = null;
```

- [ ] **Step 9: Build and verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/ui/TelemetryModule.java
git commit -m "feat: add thermal zone UI updates and inflation to TelemetryModule"
```

---

### Task 6: Add MQTT subscriptions and handler in MainActivity

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java`

**Interfaces:**
- Consumes: `telemetryModule.updateThermal(pos, ta, outside, center, inside, detected, pixels)`
- Produces: Four MQTT subscriptions on `fiesta/tire-temp/{FL|FR|RL|RR}` with `handleThermalMessage` callback

- [ ] **Step 1: Add subscriptions in `doSubscribeTelemetry()`**

After line 514 (the last `fiesta/tpms` subscription), add:

```java
        mqttClientManager.subscribe("fiesta/tire-temp/FL", bytes -> handleThermalMessage("FL", bytes));
        mqttClientManager.subscribe("fiesta/tire-temp/FR", bytes -> handleThermalMessage("FR", bytes));
        mqttClientManager.subscribe("fiesta/tire-temp/RL", bytes -> handleThermalMessage("RL", bytes));
        mqttClientManager.subscribe("fiesta/tire-temp/RR", bytes -> handleThermalMessage("RR", bytes));
```

- [ ] **Step 2: Add `handleThermalMessage()` handler method**

Insert after the `handleTpmsMessage()` method closing brace (after line 665):

```java
    private void handleThermalMessage(String pos, byte[] payload) {
        try {
            JSONObject json = new JSONObject(new String(payload));
            float ta = (float) json.optDouble("ta", Double.NaN);
            boolean detected = json.optBoolean("detected", false);
            int pixels = json.optInt("pixels", 0);
            final float outside, center, inside;
            if (detected) {
                outside = (float) json.optDouble("outside", Double.NaN);
                center  = (float) json.optDouble("center", Double.NaN);
                inside  = (float) json.optDouble("inside", Double.NaN);
            } else {
                outside = Float.NaN;
                center  = Float.NaN;
                inside  = Float.NaN;
            }
            runOnUiThread(() ->
                    telemetryModule.updateThermal(pos, ta, outside, center, inside, detected, pixels));
        } catch (JSONException e) {
            Log.w(TAG, "Malformed thermal payload for " + pos);
        }
    }
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all existing tests to confirm no regressions**

```bash
./gradlew test
```

Expected: 42 tests passed (all `PitWindowAlertManagerTest` tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java
git commit -m "feat: add MQTT subscription and handler for thermal tire temperature"
```

---

### Task 7: Final verification

- [ ] **Step 1: Full build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full test suite**

```bash
./gradlew test
```

Expected: All tests pass

- [ ] **Step 3: Verify git log shows all commits**

```bash
git log --oneline -7
```

Expected: 6 feature commits + any prior history
