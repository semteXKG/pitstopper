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
    private static final String SEG_TOPIC_SUFFIX = "/seg";

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
            String rawTopic = RAW_TOPIC_PREFIX + pos + RAW_TOPIC_SUFFIX;
            mqttClientManager.subscribe(rawTopic, payload -> handleRawMessage(pos, payload));
            String segTopic = RAW_TOPIC_PREFIX + pos + SEG_TOPIC_SUFFIX;
            mqttClientManager.subscribe(segTopic, payload -> handleSegMessage(pos, payload));
        }
        stalenessHandler.postDelayed(stalenessRunnable, 2000);
    }

    @Override
    public void onDeactivate() {
        stalenessHandler.removeCallbacks(stalenessRunnable);
        for (String pos : POSITIONS) {
            String rawTopic = RAW_TOPIC_PREFIX + pos + RAW_TOPIC_SUFFIX;
            mqttClientManager.unsubscribe(rawTopic);
            String segTopic = RAW_TOPIC_PREFIX + pos + SEG_TOPIC_SUFFIX;
            mqttClientManager.unsubscribe(segTopic);
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

    private void handleSegMessage(String pos, byte[] payload) {
        SegFrame frame = parseSegPayload(payload);
        if (frame == null) return;
        mainHandler.post(() -> {
            for (int i = 0; i < 4; i++) {
                if (POSITIONS[i].equals(pos) && views[i] != null) {
                    views[i].setLabels(frame.labels);
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

    /** Parsed segmentation frame: per-pixel zone labels (length 768, row-major). */
    public static class SegFrame {
        public final boolean detected;
        public final int pixels;
        public final int[] labels; // length 768, or all-zeros when !detected

        public SegFrame(boolean detected, int pixels, int[] labels) {
            this.detected = detected;
            this.pixels = pixels;
            this.labels = labels;
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
            try { Log.w(TAG, "Malformed raw thermal payload: " + e.getMessage()); } catch (RuntimeException ignored) {}
            return null;
        }
    }

    /**
     * Parse a /seg payload. Returns null on malformed data.
     * Expected: {"ts":<uint32>,"detected":<bool>,"pixels":<uint16>,"labels":[[<int>×32]×24]}
     */
    public static SegFrame parseSegPayload(byte[] payload) {
        try {
            JSONObject json = new JSONObject(new String(payload));
            boolean detected = json.optBoolean("detected", false);
            int pixels = json.optInt("pixels", 0);
            JSONArray labelsArray = json.optJSONArray("labels");
            if (labelsArray == null || labelsArray.length() != 24) return null;
            int[] labels = new int[24 * 32];
            for (int y = 0; y < 24; y++) {
                JSONArray row = labelsArray.getJSONArray(y);
                if (row.length() != 32) return null;
                for (int x = 0; x < 32; x++) {
                    labels[y * 32 + x] = row.getInt(x);
                }
            }
            return new SegFrame(detected, pixels, labels);
        } catch (JSONException e) {
            try { Log.w(TAG, "Malformed seg payload: " + e.getMessage()); } catch (RuntimeException ignored) {}
            return null;
        }
    }
}
