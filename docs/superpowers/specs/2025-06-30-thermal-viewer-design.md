# Thermal Raw Data Viewer — Design Spec

**Date:** 2025-06-30
**Status:** approved

## Goal

Add a swipeable `CenterModule` to PitStopper that displays the raw 32×24 thermal camera output from the four MLX90640 IR sensors, so the user can visually adjust sensor alignment at the track. Modeled on the standalone `tire-temp-viewer.html` but native to the Android app.

## Source data

MQTT topic: `fiesta/tire-temp/{FL|FR|RL|RR}/raw`

Payload (JSON):
```json
{
  "ts":     <uint32>,
  "ta":     <float>,
  "pixels": [[<float>, ... × 32] × 24]
}
```

Published at 1 Hz per sensor. The app currently subscribes to the *processed* `fiesta/tire-temp/{pos}` topic but not the `/raw` variant.

## Architecture

A new `ThermalViewerModule` (extends `CenterModule`) is added to the `swipeModules[]` array in `MainActivity`, after `TroubleshootModule`. It receives `MqttClientManager` and `PitWindowPreferences` (same construction pattern as `TroubleshootModule`).

On `onActivate()` the module subscribes to the four raw topics. On `onDeactivate()` it unsubscribes. This keeps the ~1.4 KB/frame raw data off the wire when the viewer is not visible.

```
ThermalViewerModule (CenterModule, FrameLayout)
  ├── GridLayout (2×2, grid mode)
  │     ├── ThermalCanvasView(FL)
  │     ├── ThermalCanvasView(FR)
  │     ├── ThermalCanvasView(RL)
  │     └── ThermalCanvasView(RR)
  └── TextView "←" (back button, GONE in grid mode, VISIBLE in expanded mode)
```

The module itself is a `FrameLayout`. The `GridLayout` holds the four tiles and fills the module. The back button is a `TextView` overlaid at the top-left, toggled visible only when a tile is expanded. In expanded mode, the non-expanded tiles are set to `GONE` and the expanded tile's layout parameters are changed to span the full grid.

### Tap-to-expand

In grid mode, tapping a tile hides the other three (set `GONE`) and expands the tapped tile to fill the module area. Tapping the expanded tile again returns to grid mode. The back button (top-left `TextView`) provides an alternative way to return to grid mode.

## Components

### `ThermalCanvasView` (custom `View`)

Holds the latest frame and renders it as a color-mapped, GPU-scaled bitmap.

**Fields:**
- `float[][] pixels` — 24×32 sensor data (initialized to all-NaN)
- `float ta` — ambient temperature from the MLX90640 die
- `long lastUpdateMs` — timestamp of last frame (for staleness)
- `int[] colormap` — 256-entry ARGB LUT (precomputed once)
- `float minTemp, maxTemp` — color map range

**Methods:**
- `update(float[][] pixels, float ta)` — store frame, update `lastUpdateMs`, call `invalidate()`
- `setColorRange(float min, float max)` — update range, rebuild LUT, `invalidate()`

**Rendering (`onDraw`):**
1. Reuse a preallocated 32×24 `Bitmap` (ARGB_8888), allocated once in the constructor
2. For each of the 768 sensor pixels: map the float temperature through the LUT to an ARGB color, write to the bitmap via `bitmap.setPixels()`
3. `canvas.drawBitmap(bitmap, null, destRect, paint)` where `paint.setFilterBitmap(true)` — Android GPU does bilinear upscaling for free
4. Draw header overlay on the same canvas: position label (top-left), `ta` value (top-right), staleness dot (small circle, top-right corner) using `Canvas.drawText()` and `Canvas.drawCircle()`

**Staleness dot colors:**
- Green: data < 2s old
- Orange: data 2–5s old
- Red: data > 5s old or no data yet

### `ThermalViewerModule` (CenterModule)

**Lifecycle:**
- `onActivate()`: read color range from preferences, apply to all four views, subscribe to raw topics, set VISIBLE
- `onDeactivate()`: unsubscribe, set GONE

**MQTT callback:**
1. Parse JSON: extract `ta` (double → float) and `pixels` (JSONArray of 24 rows × 32 cols)
2. Validate dimensions (24 rows, 32 cols each); skip frame if invalid
3. `runOnUiThread()` → dispatch to the matching `ThermalCanvasView.update()`

**Tap handling:**
- `expandedPos` field (null = grid mode)
- Tap in grid mode → set `expandedPos`, hide other tiles, resize tapped tile
- Tap in expanded mode → clear `expandedPos`, show all tiles

**Staleness timer:**
- `Handler` posts a runnable every 2s, calls `invalidate()` on all four views to refresh staleness dots

### Color map

256-entry LUT with the same gradient as the HTML viewer:
- 0.00–0.25: dark blue → blue
- 0.25–0.50: blue → green
- 0.50–0.75: green → yellow
- 0.75–1.00: yellow → red

`NaN` pixels (sensor dead pixels) rendered as dark grey (#3C3C3C).

### Settings

Two new preferences in `PitWindowPreferences`:
- `thermal_viewer_min` (float, default 10.0)
- `thermal_viewer_max` (float, default 80.0)

Two new input fields in `SettingsTelemetryActivity` (in the existing telemetry settings layout):
- "Thermal Viewer Min °C"
- "Thermal Viewer Max °C"

Saved/loaded alongside the existing telemetry alarm thresholds. `ThermalViewerModule` reads them on `onActivate()`.

## Error handling

- **Malformed JSON**: catch `JSONException`, log warning, skip frame
- **Wrong pixel dimensions**: validate `pixels.length == 24` and each row `length == 32`; skip if invalid
- **No data yet**: `ThermalCanvasView` initializes with all-NaN → renders uniform dark grey tile
- **Stale sensor**: staleness dot turns orange (2s) then red (5s)

## Swipe integration

- Module constructed in `MainActivity.onCreate()` alongside the other modules
- Added to `swipeModules[]` after `troubleshootModule` (index 4)
- `swipeModules` array grows from 4 to 5 elements
- Swipe cycle becomes: PitTimer → Telemetry → Chat → Troubleshoot → ThermalViewer

## Files changed

| File | Change |
|---|---|
| `ui/ThermalViewerModule.java` | New — CenterModule subclass |
| `ui/ThermalCanvasView.java` | New — custom View for thermal rendering |
| `ui/CenterModule.java` | Unchanged |
| `activities/MainActivity.java` | Construct module, add to container + swipeModules, wire MQTT |
| `timing/PitWindowPreferences.java` | Add min/max temp preferences + getters/setters |
| `activities/SettingsTelemetryActivity.java` | Add min/max temp input fields + save/load |
| `res/layout/activity_settings_telemetry.xml` | Add two EditText fields for min/max temp |
| `res/values/colors.xml` | Add staleness colors (`staleness_fresh` green, `staleness_stale` orange, `staleness_dead` red) — or reuse `alert_green`, `telemetry_warning`, `telemetry_critical` |

## Not changed

- `AndroidManifest.xml` — no new Activity needed
- `TelemetryModule.java` — existing thermal zone display is separate from this raw viewer
- `MqttClientManager.java` — uses existing subscribe/unsubscribe API
- `tire-temp-viewer.html` — standalone viewer remains as-is for browser use
