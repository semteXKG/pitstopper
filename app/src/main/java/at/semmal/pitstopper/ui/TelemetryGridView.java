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
 * Adapts its grid lines to the active TelemetryLayout.
 */
public class TelemetryGridView extends View {

    private final Paint gridPaint;
    private TelemetryLayout layout = TelemetryLayout.LAYOUT_1_2_4;

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

    public void setTelemetryLayout(TelemetryLayout layout) {
        this.layout = layout;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float left  = getPaddingLeft();
        float right = w - getPaddingRight();
        float top   = getPaddingTop();
        float bottom = h - getPaddingBottom();
        float totalH = bottom - top;
        float totalW = right - left;

        if (layout == TelemetryLayout.LAYOUT_2_4) {
            // ─────────── 2 / 4 ───────────
            // One horizontal divider at 50%
            float yMid = top + totalH * 0.5f;
            canvas.drawLine(left, yMid, right, yMid, gridPaint);

            // Top tier: 2 equal columns
            float xHalf = left + totalW * 0.5f;
            canvas.drawLine(xHalf, top, xHalf, yMid, gridPaint);

            // Bottom tier: 4 equal columns
            float colW = totalW / 4f;
            for (int i = 1; i < 4; i++) {
                float x = left + colW * i;
                canvas.drawLine(x, yMid, x, bottom, gridPaint);
            }
        } else if (layout == TelemetryLayout.LAYOUT_2_2_2) {
            // ─────────── 2 / 2 / 2 ───────────
            // Two horizontal dividers at 1/3 and 2/3 to match tier weights
            float yFirst = top + totalH / 3f;
            float ySecond = top + totalH * 2f / 3f;
            canvas.drawLine(left, yFirst, right, yFirst, gridPaint);
            canvas.drawLine(left, ySecond, right, ySecond, gridPaint);

            // Each tier: vertical split at 50%
            float xMid = left + totalW * 0.5f;
            canvas.drawLine(xMid, top, xMid, yFirst, gridPaint);
            canvas.drawLine(xMid, yFirst, xMid, ySecond, gridPaint);
            canvas.drawLine(xMid, ySecond, xMid, bottom, gridPaint);
        } else {
            // ─────────── 1 / 2 / 4 ───────────
            float yMiddle = top + totalH * 0.42f;
            float yBottom = top + totalH * 0.72f;

            canvas.drawLine(left, yMiddle, right, yMiddle, gridPaint);
            canvas.drawLine(left, yBottom, right, yBottom, gridPaint);

            // Middle tier: vertical split at 50%
            float xMidSplit = left + totalW * 0.5f;
            canvas.drawLine(xMidSplit, yMiddle, xMidSplit, yBottom, gridPaint);

            // Bottom tier: 4 equal columns
            float colW = totalW / 4f;
            for (int i = 1; i < 4; i++) {
                float x = left + colW * i;
                canvas.drawLine(x, yBottom, x, bottom, gridPaint);
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
