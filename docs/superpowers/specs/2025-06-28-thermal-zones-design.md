# Thermal Zones Sensor for PitStopper

**Date:** 2025-06-28  
**Status:** Approved — awaiting implementation

## Motivation

A new data source provides MLX90640 thermal camera readings for each tyre via MQTT. Unlike the existing TPMS bridge (which gives single-sensor temperature + pressure from valve sensors), the thermal cameras deliver segmented zone temperatures (outside / center / inside) at 1 Hz with no pressure data. Both sources coexist: TPMS for pressure, thermal cameras for zone temperatures.

## Protocol

**MQTT topics:** `fiesta/tire-temp/{FL|FR|RL|RR}` (uppercase positions)  
**Publisher:** `tire-temp-{position}` (ESP32 + MLX90640)  
**QoS:** 1 (at least once), **Retain:** No  
**Rate:** ~1 Hz

### Payload (detected)

```json
{
  "ts":       12500,
  "ta":       22.5,
  "outside":  45.2,
  "center":   52.1,
  "inside":   48.3,
  "detected": true,
  "pixels":   47
}
```

### Payload (not detected — zones omitted)

```json
{
  "ts":       13500,
  "ta":       22.6,
  "detected": false,
  "pixels":   0
}
```

| Field | Type | Unit | Notes |
|-------|------|------|-------|
| `ts` | uint32 | ms | FreeRTOS tick timestamp |
| `ta` | float | °C | MLX90640 die ambient temperature |
| `outside` | float | °C | Mean temp of outer third of detected region |
| `center` | float | °C | Mean temp of center third |
| `inside` | float | °C | Mean temp of inner third |
| `detected` | boolean | — | `true` when a tyre-sized region is found |
| `pixels` | uint16 | — | Pixel count of detected region |

Raw pixel data on `fiesta/tire-temp/{position}/raw` is **not** subscribed by this feature (reserved for offline analysis).

## Data Model Changes

### New `TelemetrySensor` enum values

```
THERMAL_FL, THERMAL_FR, THERMAL_RL, THERMAL_RR
```

Added to `TelemetrySensor.java` alongside existing `TYRE_FL`/`FR`/`RL`/`RR`.

### New fields in `TelemetryData`

Per position (`FL`/`FR`/`RL`/`RR`):

| Field | Type | Sentinel | Notes |
|-------|------|----------|-------|
| `thermalTa{Pos}` | `float` | `Float.NaN` | Ambient sensor temperature |
| `thermalOutside{Pos}` | `float` | `Float.NaN` | Outer zone mean temp |
| `thermalCenter{Pos}` | `float` | `Float.NaN` | Center zone mean temp |
| `thermalInside{Pos}` | `float` | `Float.NaN` | Inner zone mean temp |
| `thermalDetected{Pos}` | `boolean` | `false` | Tyre visible in frame |
| `thermalPixels{Pos}` | `int` | `0` | Detected pixel count |

### New setter

```java
public void setThermal(String pos, float ta, float outside, float center,
                       float inside, boolean detected, int pixels)
```

Has-helper: `hasThermal{Pos}()` returns `true` when at least one of the three zone temps is not `NaN` (i.e., data has been received). The max zone temp is computed on-the-fly: `Math.max(Math.max(outside, center), inside)`.

## MQTT Ingestion

### Subscription (MainActivity.java)

```
fiesta/tire-temp/FL → handleThermalMessage("FL", ...)
fiesta/tire-temp/FR → handleThermalMessage("FR", ...)
fiesta/tire-temp/RL → handleThermalMessage("RL", ...)
fiesta/tire-temp/RR → handleThermalMessage("RR", ...)
```

### Handler behavior

1. Parse JSON payload
2. Extract `ta`, `detected`, `pixels`
3. If `detected=true`: extract `outside`, `center`, `inside` (default to `NaN` if missing)
4. If `detected=false`: set all three zones to `NaN`
5. Call `telemetryModule.updateThermal(pos, ta, outside, center, inside, detected, pixels)` on UI thread

## UI Design

### New layout: `slot_thermal.xml`

```
┌─────────────────┐
│       FL        │  ← slotThermalPos (36sp)
│                 │
│      72.3°      │  ← slotThermalMax (auto-sized 16-120sp, bold, max zone temp)
│                 │
│ O:45° C:52° I:48°│  ← slotThermalZones (18sp)
└─────────────────┘
```

### Zone label ordering

The physical layout of the zones matches real-world tyre orientation:

| Position | Left label | Center | Right label |
|----------|------------|--------|-------------|
| FL (front-left) | **O** (outside) | **C** (center) | **I** (inside) |
| FR (front-right) | **I** (inside) | **C** (center) | **O** (outside) |
| RL (rear-left) | **O** (outside) | **C** (center) | **I** (inside) |
| RR (rear-right) | **I** (inside) | **C** (center) | **O** (outside) |

Left-side tyres (FL, RL): outside is on body-left → displayed as `O: C: I:`  
Right-side tyres (FR, RR): outside is on body-right → displayed as `I: C: O:`

### Color scheme

Colors apply to both the max temp and zone temp text, driven by the **max zone temperature**:

| Max Temp Range | Color | Hex | Meaning |
|---------------|-------|-----|---------|
| 0°C – 50°C | Blue | `#3B82F6` | Cold / warming up |
| 50°C – 75°C | Green | `#22C55E` | Optimal operating range |
| 75°C+ | Red | `#EF4444` | Hot — attention needed |
| Not detected | Dim gray | `#6B7280` | Sensor cannot see tyre |
| No data yet | White | `#FFFFFF` | Never received data (default) |

### View references in TelemetryModule

Per position, two `TextView` references:

```java
TextView thermalMaxViewFL, thermalMaxViewFR, thermalMaxViewRL, thermalMaxViewRR;
TextView thermalZonesViewFL, thermalZonesViewFR, thermalZonesViewRL, thermalZonesViewRR;
```

### Inflation

`inflateSlot()` handles `THERMAL_FL`/`FR`/`RL`/`RR` by inflating `slot_thermal.xml`, same pattern as existing `TYRE_FL` → `slot_tyre.xml`.

### Default layout

Thermal sensors are **not** in default layouts (`LAYOUT_1_2_4` or `LAYOUT_2_4`). Users must manually assign them via Settings > Telemetry.

## Settings

- `TelemetrySensor.java`: Add `THERMAL_FL`, `THERMAL_FR`, `THERMAL_RL`, `THERMAL_RR`
- `SettingsTelemetryActivity.java`: The existing sensor picker dialog automatically includes new enum values
- `TelemetryLayout.java`: No changes — thermal sensors fit into existing tier/slot grid
- No thermal-specific alert thresholds in settings for this round

## Alert Tracking

No `TelemetryAlertTracker` integration in this round. Only display. May be added later.

## Files Changed

| File | Change |
|------|--------|
| `TelemetrySensor.java` | Add 4 enum values |
| `TelemetryData.java` | Add 24 fields (6 per position) + setter + has-helpers |
| `MainActivity.java` | Add 4 MQTT subscriptions + handler method |
| `TelemetryModule.java` | Add 8 View refs + `updateThermal()` + inflation logic + color method |
| `slot_thermal.xml` | **New** layout file |
| `strings.xml` | Add thermal sensor display labels |

## TPMS Coexistence

- Existing `fiesta/tpms/{fl|fr|rl|rr}` subscriptions remain untouched
- Existing `TYRE_FL`/`FR`/`RL`/`RR` sensor types remain untouched
- Users can assign both a TPMS slot and a thermal slot in the same layout (e.g., pressure in one tier, temperature zones in another)
- Position naming: TPMS uses lowercase (`fl`), thermal uses uppercase (`FL`) — both map to same semantic position internally
