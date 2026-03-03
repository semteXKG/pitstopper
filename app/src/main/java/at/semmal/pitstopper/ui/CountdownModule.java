package at.semmal.pitstopper.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import at.semmal.pitstopper.R;

/**
 * Center module shown when the physical PIT button is pressed.
 * Counts down from a configured duration to zero, then fires onFinished.
 */
public class CountdownModule extends CenterModule {

    private final TextView textCountdown;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tickRunnable;
    private int secondsRemaining;
    private Runnable onFinished;

    public CountdownModule(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.module_countdown, this, true);
        textCountdown = findViewById(R.id.textPitCountdown);
    }

    /**
     * Start (or restart) the countdown.
     *
     * @param totalSeconds duration in seconds
     * @param onFinished   called on the main thread when countdown reaches zero
     */
    public void startCountdown(int totalSeconds, Runnable onFinished) {
        cancelCountdown();
        this.secondsRemaining = totalSeconds;
        this.onFinished = onFinished;
        updateDisplay();
        scheduleNextTick();
    }

    public void cancelCountdown() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
            tickRunnable = null;
        }
        onFinished = null;
    }

    private void scheduleNextTick() {
        tickRunnable = () -> {
            secondsRemaining--;
            updateDisplay();
            if (secondsRemaining <= 0) {
                tickRunnable = null;
                if (onFinished != null) {
                    Runnable cb = onFinished;
                    onFinished = null;
                    cb.run();
                }
            } else {
                scheduleNextTick();
            }
        };
        handler.postDelayed(tickRunnable, 1000);
    }

    private void updateDisplay() {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        if (minutes > 0) {
            textCountdown.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
        } else {
            textCountdown.setText(String.format(Locale.getDefault(), "0:%02d", seconds));
        }
    }

    @Override
    public void onActivate() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeactivate() {
        cancelCountdown();
        animate().cancel();
        setTranslationY(0);
        setVisibility(View.GONE);
    }
}
