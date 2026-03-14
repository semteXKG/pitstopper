package at.semmal.pitstopper.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import at.semmal.pitstopper.R;

/**
 * Custom drawn grid overlay for the telemetry dashboard.
 * Draws thin lines separating RPM / Speed+Controls / Health gauge cells.
 *
 * Grid layout:
 * ┌───────────────────────────────────┐
 * │              RPM                  │
 * ├────────────────┬──────────────────┤
 * │     SPEED      │   THR / BRK     │
 * ├─────────┬──────┴───┬──────┬──────┤
 * │  COOL   │  OIL T   │ OIL P│ BATT │
 * └─────────┴──────────┴──────┴──────┘
 */
public class TelemetryGridView extends View {

    private static final float GUIDE_MIDDLE = 0.42f;
    private static final float GUIDE_BOTTOM = 0.72f;
    private static final float MID_VERTICAL = 0.45f;

    private final Paint gridPaint;

    public TelemetryGridView(Context context) {
        this(context, null);
    }

    public TelemetryGridView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TelemetryGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.telemetry_grid));
        gridPaint.setStrokeWidth(dpToPx(1.5f));
        gridPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int pad = getPaddingLeft();
        float left = pad;
        float right = w - getPaddingRight();
        float top = getPaddingTop();
        float bottom = h - getPaddingBottom();

        float yMiddle = top + (bottom - top) * GUIDE_MIDDLE;
        float yBottom = top + (bottom - top) * GUIDE_BOTTOM;

        // Outer border
        canvas.drawRect(left, top, right, bottom, gridPaint);

        // Horizontal dividers
        canvas.drawLine(left, yMiddle, right, yMiddle, gridPaint);
        canvas.drawLine(left, yBottom, right, yBottom, gridPaint);

        // Middle zone: vertical split between speed and throttle/brake
        float xMidSplit = left + (right - left) * MID_VERTICAL;
        canvas.drawLine(xMidSplit, yMiddle, xMidSplit, yBottom, gridPaint);

        // Bottom zone: 4 equal columns
        float colW = (right - left) / 4f;
        for (int i = 1; i < 4; i++) {
            float x = left + colW * i;
            canvas.drawLine(x, yBottom, x, bottom, gridPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
