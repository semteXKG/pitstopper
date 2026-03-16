package at.semmal.pitstopper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import at.semmal.pitstopper.R;

/**
 * Center module displaying the pit-window timer:
 * - Event/session label (top)
 * - Current time in 120sp monospace (center)
 * - Countdown to next pit window or time remaining in alert (bottom)
 */
public class PitTimerModule extends CenterModule {

    private final TextView textCurrentTime;
    private final TextView textCountdown;
    private final TextView textEventSession;
    private final TextView textWifiWarning;

    public PitTimerModule(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.module_pit_timer, this, true);
        textCurrentTime = findViewById(R.id.textCurrentTime);
        textCountdown = findViewById(R.id.textCountdown);
        textEventSession = findViewById(R.id.textEventSession);
        textWifiWarning = findViewById(R.id.textWifiWarning);
    }

    public void updateTime(String time) {
        textCurrentTime.setText(time);
    }

    public void updateCountdown(String countdown) {
        textCountdown.setText(countdown);
    }

    public void setEventSession(String text, boolean visible) {
        textEventSession.setText(text);
        textEventSession.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setWifiWarning(String text) {
        if (text == null || text.isEmpty()) {
            textWifiWarning.setVisibility(View.GONE);
        } else {
            textWifiWarning.setText(text);
            textWifiWarning.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivate() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeactivate() {
        animate().cancel();
        setTranslationY(0);
        setVisibility(View.GONE);
    }
}
