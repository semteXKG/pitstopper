package at.semmal.pitstopper.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.core.content.ContextCompat;

import at.semmal.pitstopper.R;

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

    private static final int ZONE_TINT_ALPHA = 0x22;
    private static final int LABELS_LEN = SRC_ROWS * SRC_COLS;

    private float[][] pixels = new float[SRC_ROWS][SRC_COLS];
    private float ta = Float.NaN;
    private long lastUpdateMs = 0;
    private int[] colormap = buildColormap();
    private float minTemp = 10f;
    private float maxTemp = 80f;
    private int[] labels = null;
    private final Paint zoneFillPaint;
    private final Paint zoneOutlinePaint;
    private final Paint zoneBoundaryPaint;
    private final int[] zoneColors;

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

        int cold = resolveColor(context, R.color.thermal_cold);
        int warm = resolveColor(context, R.color.thermal_warm);
        int hot  = resolveColor(context, R.color.thermal_hot);
        zoneFillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        zoneFillPaint.setStyle(Paint.Style.FILL);
        zoneOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        zoneOutlinePaint.setStyle(Paint.Style.STROKE);
        zoneOutlinePaint.setColor(0xFFFFFFFF);
        zoneBoundaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        zoneBoundaryPaint.setStyle(Paint.Style.STROKE);
        if (!isInEditMode()) {
            float dp = getResources().getDisplayMetrics().density;
            zoneOutlinePaint.setStrokeWidth(2f * dp);
            zoneBoundaryPaint.setStrokeWidth(1.5f * dp);
        }
        zoneColors = new int[] { 0, cold, warm, hot };
    }

    private static int resolveColor(Context context, int resId) {
        try {
            return androidx.core.content.ContextCompat.getColor(context, resId);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    /** Update the thermal frame. Call on the UI thread. */
    public void update(float[][] pixels, float ta) {
        this.pixels = pixels;
        this.ta = ta;
        this.lastUpdateMs = System.currentTimeMillis();
        invalidate();
    }

    /** Set the color map range and update the view. */
    public void setColorRange(float min, float max) {
        this.minTemp = min;
        this.maxTemp = max;
        invalidate();
    }

    /** Store per-pixel zone labels (length 24*32, row-major) and invalidate. */
    public void setLabels(int[] labels) {
        if (labels != null && labels.length != LABELS_LEN) return;
        this.labels = labels;
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
        drawZones(canvas);
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

    private void drawZones(Canvas canvas) {
        if (labels == null) return;
        boolean any = false;
        for (int i = 0; i < LABELS_LEN; i++) {
            if (labels[i] != 0) { any = true; break; }
        }
        if (!any) return;

        float sx = (float) getWidth()  / SRC_COLS;
        float sy = (float) getHeight() / SRC_ROWS;

        for (int y = 0; y < SRC_ROWS; y++) {
            for (int x = 0; x < SRC_COLS; x++) {
                int z = labels[y * SRC_COLS + x];
                if (z <= 0 || z > 3) continue;
                int base = zoneColors[z];
                zoneFillPaint.setColor((ZONE_TINT_ALPHA << 24) | (base & 0x00FFFFFF));
                float left = x * sx;
                float top  = y * sy;
                canvas.drawRect(left, top, left + sx, top + sy, zoneFillPaint);
            }
        }

        for (int y = 0; y < SRC_ROWS; y++) {
            for (int x = 0; x < SRC_COLS; x++) {
                int a = labels[y * SRC_COLS + x];
                int ax = x + 1, ay = y + 1;
                if (ax < SRC_COLS) {
                    int b = labels[y * SRC_COLS + ax];
                    drawEdge(canvas, x, y, ax, y, a, b, sx, sy);
                }
                if (ay < SRC_ROWS) {
                    int b = labels[ay * SRC_COLS + x];
                    drawEdge(canvas, x, y, x, ay, a, b, sx, sy);
                }
            }
        }
    }

    private void drawEdge(Canvas canvas, int x0, int y0, int x1, int y1,
                          int a, int b, float sx, float sy) {
        boolean aOn = a > 0;
        boolean bOn = b > 0;
        Paint p;
        if (aOn != bOn) {
            p = zoneOutlinePaint;
        } else if (aOn && a != b) {
            int lo = Math.min(a, b);
            p = zoneBoundaryPaint;
            int base = zoneColors[lo];
            p.setColor((0xFF << 24) | (base & 0x00FFFFFF));
            if (!isInEditMode()) {
                float dp = getResources().getDisplayMetrics().density;
                p.setStrokeWidth(1.5f * dp);
            }
        } else {
            return;
        }
        if (y0 == y1) {
            float fx = x1 * sx;
            canvas.drawLine(fx, y0 * sy, fx, (y0 + 1) * sy, p);
        } else {
            float fy = y1 * sy;
            canvas.drawLine(x0 * sx, fy, (x0 + 1) * sx, fy, p);
        }
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
                b = Math.min(255, Math.round(255 * (t / 0.25f + 0.25f)));
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
