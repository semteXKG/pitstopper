# Thermal Raw Data Viewer — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a swipeable `ThermalViewerModule` to PitStopper that displays raw 32×24 IR sensor thermal images for sensor alignment.

**Architecture:** New `ThermalCanvasView` (custom `View`) renders color-mapped 32×24 bitmaps with GPU bilinear scaling. `ThermalViewerModule` (CenterModule) manages a 2×2 grid of these views, subscribes to `fiesta/tire-temp/{pos}/raw` on activate, unsubscribes on deactivate. Color map min/max configurable via settings.

**Tech Stack:** Java 11, Android SDK, HiveMQ MQTT client, JUnit 4.

## Global Constraints

- Language: Java 11 (no Kotlin source)
- Package: `at.semmal.pitstopper`
- MQTT topics: `fiesta/tire-temp/{FL|FR|RL|RR}/raw` — JSON payload `{"ts":<uint32>,"ta":<float>,"pixels":[[<float>×32]×24]}`
- Color map LUT: 256 entries, gradient blue→cyan→green→yellow→red (same as `tire-temp-viewer.html`)
- NaN pixels render as dark grey (#3C3C3C)
- Default temp range: 10°C–80°C
- Build: `./gradlew assembleDebug`
- Test: `./gradlew test`
- Test framework: JUnit 4 (`org.junit.Test`, `static org.junit.Assert.*`)

---

### Task 1: Add `unsubscribe` to `MqttClientManager`

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/mqtt/MqttClientManager.java`

**Interfaces:**
- Produces: `public void unsubscribe(String topic)` — unsubscribes from a single topic. Safe to call when not connected (no-op).

- [ ] **Step 1: Add the `unsubscribe` method**

Add after the existing `subscribe` method (after line 251, before `setState`):

```java
/**
 * Unsubscribe from a topic. Safe to call when not connected (no-op).
 */
public void unsubscribe(String topic) {
    if (client == null || currentState != State.CONNECTED) {
        return;
    }
    client.unsubscribeWith()
            .topicFilter(topic)
            .send()
            .whenComplete((voidResult, throwable) -> {
                if (throwable != null) {
                    Log.e(TAG, "Unsubscribe failed for " + topic + ": " + throwable.getMessage());
                }
            });
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/mqtt/MqttClientManager.java
git commit -m "feat: add unsubscribe method to MqttClientManager"
```

---

### Task 2: Add thermal viewer preferences to `PitWindowPreferences`

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/timing/PitWindowPreferences.java`

**Interfaces:**
- Produces: `getThermalViewerMin()` → `float`, `getThermalViewerMax()` → `float`, `saveThermalViewerRange(float min, float max)` → `void`

Note: `PitWindowPreferences` wraps Android `SharedPreferences` via a `Context`, so it can't be unit tested without Robolectric (not in this project). No existing tests cover it. Rely on build verification and manual testing, consistent with the existing codebase pattern.

- [ ] **Step 1: Add key constants and defaults**

In `PitWindowPreferences.java`, after the existing KEY constants (after line 85 area, near the telemetry keys):

```java
// Thermal viewer settings
private static final String KEY_THERMAL_VIEWER_MIN = "thermal_viewer_min";
private static final String KEY_THERMAL_VIEWER_MAX = "thermal_viewer_max";
private static final float DEFAULT_THERMAL_VIEWER_MIN = 10.0f;
private static final float DEFAULT_THERMAL_VIEWER_MAX = 80.0f;
```

- [ ] **Step 2: Add getter and setter methods**

Add near the other telemetry getters/setters (after `isBatteryAlarm()` around line 375):

```java
public float getThermalViewerMin() { return prefs.getFloat(KEY_THERMAL_VIEWER_MIN, DEFAULT_THERMAL_VIEWER_MIN); }
public float getThermalViewerMax() { return prefs.getFloat(KEY_THERMAL_VIEWER_MAX, DEFAULT_THERMAL_VIEWER_MAX); }

public void saveThermalViewerRange(float min, float max) {
    prefs.edit()
        .putFloat(KEY_THERMAL_VIEWER_MIN, min)
        .putFloat(KEY_THERMAL_VIEWER_MAX, max)
        .apply();
}
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/timing/PitWindowPreferences.java
git commit -m "feat: add thermal viewer min/max temp preferences"
```

---

### Task 3: Create `ThermalCanvasView` with color map

**Files:**
- Create: `app/src/main/java/at/semmal/pitstopper/ui/ThermalCanvasView.java`
- Test: `app/src/test/java/at/semmal/pitstopper/ui/ThermalColormapTest.java`

**Interfaces:**
- Produces: `ThermalCanvasView` class with `update(float[][] pixels, float ta)`, `setColorRange(float min, float max)`, static `buildColormap()` → `int[]`, static `tempToColor(float temp, float min, float max, int[] lut)` → `int`

- [ ] **Step 1: Write the failing test for the color map LUT**

```java
package at.semmal.pitstopper.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThermalColormapTest {

    @Test
    public void lutHas256Entries() {
        int[] lut = ThermalCanvasView.buildColormap();
        assertEquals(256, lut.length);
    }

    @Test
    public void lutStartIsDarkBlue() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = lut[0];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        assertEquals(0, r);
        assertEquals(0, g);
        assertTrue(b > 0); // some blue
    }

    @Test
    public void lutEndIsRed() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = lut[255];
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        assertEquals(255, r);
        assertEquals(0, g);
        assertEquals(0, b);
    }

    @Test
    public void tempToColorNaNReturnsGrey() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = ThermalCanvasView.tempToColor(Float.NaN, 10f, 80f, lut);
        assertEquals(0xFF3C3C3C, c);
    }

    @Test
    public void tempToColorBelowMinClampsToFirstEntry() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = ThermalCanvasView.tempToColor(5f, 10f, 80f, lut);
        assertEquals(lut[0], c);
    }

    @Test
    public void tempToColorAboveMaxClampsToLastEntry() {
        int[] lut = ThermalCanvasView.buildColormap();
        int c = ThermalCanvasView.tempToColor(100f, 10f, 80f, lut);
        assertEquals(lut[255], c);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "at.semmal.pitstopper.ui.ThermalColormapTest"`
Expected: FAIL — `ThermalCanvasView` class does not exist

- [ ] **Step 3: Create `ThermalCanvasView.java`**

```java
package at.semmal.pitstopper.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Custom View that renders a 24×32 thermal pixel array as a color-mapped,
 * GPU-scaled bitmap. Header overlay shows position label, ambient temp,
 * and a staleness dot.
 */
public class ThermalCanvasView extends View {

    private static final int SRC_COLS = 32;
    private static final int SRC_ROWS = 24;
    private static final int NAN_COLOR = 0xFF3C3C3C;

    private final Bitmap bitmap = Bitmap.createBitmap(SRC_COLS, SRC_ROWS, Bitmap.Config.ARGB_8888);
    private final int[] bitmapPixels = new int[SRC_COLS * SRC_ROWS];
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint;
    private final Paint dotPaint;
    private final String label;

    private float[][] pixels = new float[SRC_ROWS][SRC_COLS];
    private float ta = Float.NaN;
    private long lastUpdateMs = 0;
    private int[] colormap = buildColormap();
    private float minTemp = 10f;
    private float maxTemp = 80f;

    public ThermalCanvasView(Context context, String label) {
        super(context);
        this.label = label;
        // Initialize pixels to NaN
        for (int y = 0; y < SRC_ROWS; y++) {
            for (int x = 0; x < SRC_COLS; x++) {
                pixels[y][x] = Float.NaN;
            }
        }
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(28f);
        textPaint.setFakeBoldText(true);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    /** Update the thermal frame. Call on the UI thread. */
    public void update(float[][] pixels, float ta) {
        this.pixels = pixels;
        this.ta = ta;
        this.lastUpdateMs = System.currentTimeMillis();
        invalidate();
    }

    /** Set the color map range and rebuild the LUT. */
    public void setColorRange(float min, float max) {
        this.minTemp = min;
        this.maxTemp = max;
        invalidate();
    }

    /** Return the position label (FL/FR/RL/RR). Used by ThermalViewerModule for tap-to-expand. */
    public String getLabel() {
        return label;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawThermal(canvas);
        drawHeader(canvas);
    }

    private void drawThermal(Canvas canvas) {
        // Build the 32×24 bitmap from the pixel array
        for (int y = 0; y < SRC_ROWS; y++) {
            for (int x = 0; x < SRC_COLS; x++) {
                bitmapPixels[y * SRC_COLS + x] = tempToColor(pixels[y][x], minTemp, maxTemp, colormap);
            }
        }
        bitmap.setPixels(bitmapPixels, 0, SRC_COLS, 0, 0, SRC_COLS, SRC_ROWS);
        // Draw scaled — GPU does bilinear interpolation via FILTER_BITMAP_FLAG
        canvas.drawBitmap(bitmap, null, new android.graphics.RectF(0, 0, getWidth(), getHeight()), bitmapPaint);
    }

    private void drawHeader(Canvas canvas) {
        // Position label (top-left)
        canvas.drawText(label, 8, 28, textPaint);
        // Ambient temp (top-right)
        if (!Float.isNaN(ta)) {
            String taStr = String.format(java.util.Locale.US, "ta: %.1f°", ta);
            float textWidth = textPaint.measureText(taStr);
            canvas.drawText(taStr, getWidth() - textWidth - 8, 28, textPaint);
        }
        // Staleness dot (top-right, below ta)
        long age = System.currentTimeMillis() - lastUpdateMs;
        int dotColor;
        if (lastUpdateMs == 0) {
            dotColor = 0xFFF44336; // red — no data
        } else if (age < 2000) {
            dotColor = 0xFF4CAF50; // green
        } else if (age < 5000) {
            dotColor = 0xFFFF9800; // orange
        } else {
            dotColor = 0xFFF44336; // red
        }
        dotPaint.setColor(dotColor);
        canvas.drawCircle(getWidth() - 16, 44, 6, dotPaint);
    }

    // ── Static color map logic (unit-testable) ──────────────────────────────

    /**
     * Build a 256-entry ARGB color lookup table.
     * Gradient: dark blue → blue → green → yellow → red.
     */
    public static int[] buildColormap() {
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            int r, g, b;
            if (t < 0.25f) {
                r = 0;
                g = 0;
                b = Math.round(255 * (t / 0.25f + 0.25f));
            } else if (t < 0.5f) {
                r = 0;
                g = Math.round(255 * ((t - 0.25f) / 0.25f));
                b = 255;
            } else if (t < 0.75f) {
                r = Math.round(255 * ((t - 0.5f) / 0.25f));
                g = 255;
                b = Math.round(255 * (1 - (t - 0.5f) / 0.25f));
            } else {
                r = 255;
                g = Math.round(255 * (1 - (t - 0.75f) / 0.25f));
                b = 0;
            }
            lut[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
        return lut;
    }

    /**
     * Map a temperature value to an ARGB color via the LUT.
     * NaN returns NAN_COLOR (dark grey). Values are clamped to [min, max].
     */
    public static int tempToColor(float temp, float min, float max, int[] lut) {
        if (Float.isNaN(temp)) return NAN_COLOR;
        float clamped = Math.max(min, Math.min(max, temp));
        float range = max - min;
        if (range <= 0) return lut[0];
        int idx = Math.round(((clamped - min) / range) * 255);
        if (idx < 0) idx = 0;
        if (idx > 255) idx = 255;
        return lut[idx];
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "at.semmal.pitstopper.ui.ThermalColormapTest"`
Expected: PASS — all 6 tests pass

- [ ] **Step 5: Build to verify full compilation**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/ui/ThermalCanvasView.java \
        app/src/test/java/at/semmal/pitstopper/ui/ThermalColormapTest.java
git commit -m "feat: add ThermalCanvasView with color-mapped bilinear rendering"
```

---

### Task 4: Create `ThermalViewerModule` + layout

**Files:**
- Create: `app/src/main/res/layout/module_thermal_viewer.xml`
- Create: `app/src/main/java/at/semmal/pitstopper/ui/ThermalViewerModule.java`
- Test: `app/src/test/java/at/semmal/pitstopper/ui/ThermalViewerModuleTest.java`

**Interfaces:**
- Consumes: `MqttClientManager.subscribe(String, Consumer<byte[]>)`, `MqttClientManager.unsubscribe(String)`, `PitWindowPreferences.getThermalViewerMin()`, `PitWindowPreferences.getThermalViewerMax()`
- Produces: `ThermalViewerModule` class (extends `CenterModule`) with `onActivate()` / `onDeactivate()` and static `parseRawPayload(byte[])` → `RawThermalFrame` or `null`

- [ ] **Step 1: Write the failing test for JSON parsing**

```java
package at.semmal.pitstopper.ui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThermalViewerModuleTest {

    @Test
    public void parseValidPayload() throws Exception {
        // Build a 24×32 pixel array
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 24; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) {
                row.put(25.0 + x * 0.5);
            }
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ts", 12345);
        json.put("ta", 22.5);
        json.put("pixels", rows);

        ThermalViewerModule.RawThermalFrame frame =
                ThermalViewerModule.parseRawPayload(json.toString().getBytes());

        assertNotNull(frame);
        assertEquals(22.5f, frame.ta, 0.01f);
        assertEquals(24, frame.pixels.length);
        assertEquals(32, frame.pixels[0].length);
        assertEquals(25.0f, frame.pixels[0][0], 0.01f);
        assertEquals(40.5f, frame.pixels[0][31], 0.01f);
    }

    @Test
    public void parseWrongRowCountReturnsNull() throws Exception {
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 20; y++) { // wrong: 20 instead of 24
            JSONArray row = new JSONArray();
            for (int x = 0; x < 32; x++) row.put(25.0);
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ta", 22.5);
        json.put("pixels", rows);

        assertNull(ThermalViewerModule.parseRawPayload(json.toString().getBytes()));
    }

    @Test
    public void parseWrongColCountReturnsNull() throws Exception {
        JSONArray rows = new JSONArray();
        for (int y = 0; y < 24; y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < 30; x++) row.put(25.0); // wrong: 30 instead of 32
            rows.put(row);
        }
        JSONObject json = new JSONObject();
        json.put("ta", 22.5);
        json.put("pixels", rows);

        assertNull(ThermalViewerModule.parseRawPayload(json.toString().getBytes()));
    }

    @Test
    public void parseMalformedJsonReturnsNull() {
        assertNull(ThermalViewerModule.parseRawPayload("not json".getBytes()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "at.semmal.pitstopper.ui.ThermalViewerModuleTest"`
Expected: FAIL — `ThermalViewerModule` class does not exist

- [ ] **Step 3: Create the layout XML**

`app/src/main/res/layout/module_thermal_viewer.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <GridLayout
        android:id="@+id/thermalGrid"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:rowCount="2"
        android:columnCount="2" />

    <TextView
        android:id="@+id/thermalBackBtn"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:layout_marginTop="4dp"
        android:fontFamily="monospace"
        android:text="←"
        android:textColor="@color/text_primary"
        android:textSize="28sp"
        android:visibility="gone" />

</FrameLayout>
```

- [ ] **Step 4: Create `ThermalViewerModule.java`**

```java
package at.semmal.pitstopper.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.mqtt.MqttClientManager;
import at.semmal.pitstopper.timing.PitWindowPreferences;

/**
 * Center module displaying raw 32×24 thermal camera output from the four
 * MLX90640 IR sensors. Used for sensor alignment at the track.
 *
 * Subscribes to fiesta/tire-temp/{pos}/raw on activate, unsubscribes on
 * deactivate. Shows a 2×2 grid of ThermalCanvasViews with tap-to-expand.
 */
public class ThermalViewerModule extends CenterModule {

    private static final String TAG = "ThermalViewerModule";
    private static final String[] POSITIONS = {"FL", "FR", "RL", "RR"};
    private static final String RAW_TOPIC_PREFIX = "fiesta/tire-temp/";
    private static final String RAW_TOPIC_SUFFIX = "/raw";

    private final MqttClientManager mqttClientManager;
    private final PitWindowPreferences preferences;
    private final ThermalCanvasView[] views = new ThermalCanvasView[4];
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler stalenessHandler = new Handler(Looper.getMainLooper());

    private GridLayout gridLayout;
    private TextView backBtn;
    private String expandedPos = null; // null = grid mode

    private final Runnable stalenessRunnable = new Runnable() {
        @Override
        public void run() {
            for (ThermalCanvasView v : views) {
                if (v != null) v.invalidate();
            }
            stalenessHandler.postDelayed(this, 2000);
        }
    };

    public ThermalViewerModule(Context context, MqttClientManager mqttClientManager,
                               PitWindowPreferences preferences) {
        super(context);
        this.mqttClientManager = mqttClientManager;
        this.preferences = preferences;
        LayoutInflater.from(context).inflate(R.layout.module_thermal_viewer, this, true);

        gridLayout = findViewById(R.id.thermalGrid);
        backBtn = findViewById(R.id.thermalBackBtn);

        // Create 4 ThermalCanvasViews and add to GridLayout
        for (int i = 0; i < 4; i++) {
            ThermalCanvasView v = new ThermalCanvasView(context, POSITIONS[i]);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / 2, 1, 1f);
            params.columnSpec = GridLayout.spec(i % 2, 1, 1f);
            params.width = 0;
            params.height = 0;
            v.setLayoutParams(params);
            v.setOnClickListener(view -> onTileTapped(v));
            views[i] = v;
            gridLayout.addView(v);
        }

        backBtn.setOnClickListener(v -> collapseGrid());
    }

    @Override
    public void onActivate() {
        setVisibility(View.VISIBLE);
        float min = preferences.getThermalViewerMin();
        float max = preferences.getThermalViewerMax();
        for (ThermalCanvasView v : views) {
            v.setColorRange(min, max);
        }
        for (String pos : POSITIONS) {
            String topic = RAW_TOPIC_PREFIX + pos + RAW_TOPIC_SUFFIX;
            mqttClientManager.subscribe(topic, payload -> handleRawMessage(pos, payload));
        }
        stalenessHandler.postDelayed(stalenessRunnable, 2000);
    }

    @Override
    public void onDeactivate() {
        stalenessHandler.removeCallbacks(stalenessRunnable);
        for (String pos : POSITIONS) {
            String topic = RAW_TOPIC_PREFIX + pos + RAW_TOPIC_SUFFIX;
            mqttClientManager.unsubscribe(topic);
        }
        setVisibility(View.GONE);
    }

    private void handleRawMessage(String pos, byte[] payload) {
        RawThermalFrame frame = parseRawPayload(payload);
        if (frame == null) return;
        mainHandler.post(() -> {
            for (int i = 0; i < 4; i++) {
                if (POSITIONS[i].equals(pos) && views[i] != null) {
                    views[i].update(frame.pixels, frame.ta);
                }
            }
        });
    }

    private void onTileTapped(ThermalCanvasView tapped) {
        if (expandedPos == null) {
            expandTile(tapped);
        } else {
            collapseGrid();
        }
    }

    private void expandTile(ThermalCanvasView tapped) {
        expandedPos = tapped.getLabel();
        for (ThermalCanvasView v : views) {
            if (v == tapped) {
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(0, 2, 1f);
                params.columnSpec = GridLayout.spec(0, 2, 1f);
                params.width = 0;
                params.height = 0;
                v.setLayoutParams(params);
            } else {
                v.setVisibility(View.GONE);
            }
        }
        backBtn.setVisibility(View.VISIBLE);
    }

    private void collapseGrid() {
        expandedPos = null;
        for (int i = 0; i < 4; i++) {
            ThermalCanvasView v = views[i];
            v.setVisibility(View.VISIBLE);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / 2, 1, 1f);
            params.columnSpec = GridLayout.spec(i % 2, 1, 1f);
            params.width = 0;
            params.height = 0;
            v.setLayoutParams(params);
        }
        backBtn.setVisibility(View.GONE);
    }

    // ── JSON parsing (static, unit-testable) ────────────────────────────────

    /** Parsed raw thermal frame. */
    public static class RawThermalFrame {
        public final float ta;
        public final float[][] pixels; // [24][32]

        public RawThermalFrame(float ta, float[][] pixels) {
            this.ta = ta;
            this.pixels = pixels;
        }
    }

    /**
     * Parse a raw thermal payload. Returns null on malformed data.
     * Expected: {"ts":<uint32>,"ta":<float>,"pixels":[[<float>×32]×24]}
     */
    public static RawThermalFrame parseRawPayload(byte[] payload) {
        try {
            JSONObject json = new JSONObject(new String(payload));
            float ta = (float) json.optDouble("ta", Double.NaN);
            JSONArray pixelsArray = json.optJSONArray("pixels");
            if (pixelsArray == null || pixelsArray.length() != 24) return null;
            float[][] pixels = new float[24][32];
            for (int y = 0; y < 24; y++) {
                JSONArray row = pixelsArray.getJSONArray(y);
                if (row.length() != 32) return null;
                for (int x = 0; x < 32; x++) {
                    pixels[y][x] = (float) row.getDouble(x);
                }
            }
            return new RawThermalFrame(ta, pixels);
        } catch (JSONException e) {
            Log.w(TAG, "Malformed raw thermal payload: " + e.getMessage());
            return null;
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "at.semmal.pitstopper.ui.ThermalViewerModuleTest"`
Expected: PASS — all 4 tests pass

- [ ] **Step 6: Build to verify full compilation**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/layout/module_thermal_viewer.xml \
        app/src/main/java/at/semmal/pitstopper/ui/ThermalViewerModule.java \
        app/src/main/java/at/semmal/pitstopper/ui/ThermalCanvasView.java \
        app/src/test/java/at/semmal/pitstopper/ui/ThermalViewerModuleTest.java
git commit -m "feat: add ThermalViewerModule with 2×2 grid and tap-to-expand"
```

---

### Task 5: Wire `ThermalViewerModule` into `MainActivity`

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java`

**Interfaces:**
- Consumes: `ThermalViewerModule` constructor `(Context, MqttClientManager, PitWindowPreferences)`, `CenterModule.onActivate()/onDeactivate()`

- [ ] **Step 1: Add the field declaration**

In `MainActivity.java`, after the `troubleshootModule` field declaration (line 78):

```java
private ThermalViewerModule thermalViewerModule;
```

- [ ] **Step 2: Change `swipeModules` array size from 4 to 5**

Change line 81 from:

```java
private final CenterModule[] swipeModules = new CenterModule[4];
```

to:

```java
private final CenterModule[] swipeModules = new CenterModule[5];
```

- [ ] **Step 3: Add import**

Add to the imports section (after the existing `TelemetryModule` import, line 18):

```java
import at.semmal.pitstopper.ui.ThermalViewerModule;
```

- [ ] **Step 4: Construct the module**

After the `troubleshootModule` construction (line 166), add:

```java
thermalViewerModule = new ThermalViewerModule(this, mqttClientManager, preferences);
```

- [ ] **Step 5: Add to container and swipeModules**

After `centerModuleContainer.addView(troubleshootModule);` (line 172), add:

```java
centerModuleContainer.addView(thermalViewerModule);
```

After `swipeModules[3] = troubleshootModule;` (line 176), add:

```java
swipeModules[4] = thermalViewerModule;
```

- [ ] **Step 6: Call onDeactivate on the new module**

After `troubleshootModule.onDeactivate();` (line 183), add:

```java
thermalViewerModule.onDeactivate();
```

- [ ] **Step 7: Build and test**

Run: `./gradlew assembleDebug && ./gradlew test`
Expected: `BUILD SUCCESSFUL` for both

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java
git commit -m "feat: wire ThermalViewerModule into swipe cycle"
```

---

### Task 6: Add thermal viewer settings UI

**Files:**
- Modify: `app/src/main/res/layout/activity_settings_telemetry.xml`
- Modify: `app/src/main/java/at/semmal/pitstopper/activities/SettingsTelemetryActivity.java`

**Interfaces:**
- Consumes: `PitWindowPreferences.getThermalViewerMin()`, `getThermalViewerMax()`, `saveThermalViewerRange(float, float)`

- [ ] **Step 1: Add EditText fields to the settings layout**

In `activity_settings_telemetry.xml`, insert after `</RadioGroup>` (line 466) and before the `<!-- Layout preview -->` comment:

```xml
    <!-- Thermal viewer color range -->
    <TextView
        android:id="@+id/textThermalRangeLabel"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:fontFamily="monospace"
        android:text="Thermal Viewer Range (°C)"
        android:textColor="@color/telemetry_label"
        android:textSize="16sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/radioGroupLayout" />

    <EditText
        android:id="@+id/editThermalMin"
        android:layout_width="0dp"
        android:layout_height="48dp"
        android:layout_marginTop="4dp"
        android:background="#FF222222"
        android:fontFamily="monospace"
        android:hint="Min"
        android:inputType="numberDecimal|numberSigned"
        android:textColor="@color/text_primary"
        android:textColorHint="@color/telemetry_label"
        android:textSize="16sp"
        app:layout_constraintEnd_toStartOf="@id/editThermalMax"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/textThermalRangeLabel" />

    <EditText
        android:id="@+id/editThermalMax"
        android:layout_width="0dp"
        android:layout_height="48dp"
        android:layout_marginStart="8dp"
        android:layout_marginTop="4dp"
        android:background="#FF222222"
        android:fontFamily="monospace"
        android:hint="Max"
        android:inputType="numberDecimal|numberSigned"
        android:textColor="@color/text_primary"
        android:textColorHint="@color/telemetry_label"
        android:textSize="16sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@id/editThermalMin"
        app:layout_constraintTop_toTopOf="@id/editThermalMin" />
```

Then update the `layoutPreview` constraint: change `app:layout_constraintTop_toBottomOf="@id/radioGroupLayout"` to `app:layout_constraintTop_toBottomOf="@id/editThermalMin"`.

- [ ] **Step 2: Add field declarations in `SettingsTelemetryActivity.java`**

After the `checkBatteryAlarm` field (line 36), add:

```java
private EditText editThermalMin, editThermalMax;
```

- [ ] **Step 3: Add findViewById calls in `onCreate`**

After `checkBatteryAlarm = findViewById(R.id.checkBatteryAlarm);` (line 88), add:

```java
editThermalMin = findViewById(R.id.editThermalMin);
editThermalMax = findViewById(R.id.editThermalMax);
```

- [ ] **Step 4: Add load logic in `loadSettings()`**

After `checkBatteryAlarm.setChecked(preferences.isBatteryAlarm());` (line 128), add:

```java
editThermalMin.setText(String.valueOf(preferences.getThermalViewerMin()));
editThermalMax.setText(String.valueOf(preferences.getThermalViewerMax()));
```

- [ ] **Step 5: Add save logic in `saveSettings()`**

After the `preferences.saveLayoutConfig(...)` call (line 234), add before the Toast:

```java
float thermalMin = Float.parseFloat(editThermalMin.getText().toString().trim());
float thermalMax = Float.parseFloat(editThermalMax.getText().toString().trim());
if (thermalMin >= thermalMax) {
    Toast.makeText(this, "Thermal min must be less than max", Toast.LENGTH_SHORT).show();
    return;
}
preferences.saveThermalViewerRange(thermalMin, thermalMax);
```

- [ ] **Step 6: Build and test**

Run: `./gradlew assembleDebug && ./gradlew test`
Expected: `BUILD SUCCESSFUL` for both

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/layout/activity_settings_telemetry.xml \
        app/src/main/java/at/semmal/pitstopper/activities/SettingsTelemetryActivity.java
git commit -m "feat: add thermal viewer color range to telemetry settings"
```
