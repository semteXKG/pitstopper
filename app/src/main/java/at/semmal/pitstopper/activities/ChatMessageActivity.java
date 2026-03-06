package at.semmal.pitstopper.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import at.semmal.pitstopper.R;

public class ChatMessageActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "chat_message";
    public static final String EXTRA_FROM    = "chat_from";

    private static final long AUTO_CLOSE_DELAY_MS = 10_000;
    private static final long FLASH_INTERVAL_MS   = 2_000;

    private static final int COLOR_BLUE  = Color.parseColor("#0055CC");
    private static final int COLOR_BLACK = Color.BLACK;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable closeRunnable = this::finish;

    private View rootLayout;
    private TextView textMessage;
    private TextView textFrom;
    private ValueAnimator flashAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);
        rootLayout  = findViewById(R.id.chatRootLayout);
        textMessage = findViewById(R.id.textChatMessage);
        textFrom    = findViewById(R.id.textChatFrom);
        showMessage(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showMessage(intent);
    }

    private void showMessage(Intent intent) {
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String from    = intent.getStringExtra(EXTRA_FROM);

        textMessage.setText(message != null ? message : "");
        textFrom.setText(from != null ? from : "");

        stopFlash();
        startFlash();

        handler.removeCallbacks(closeRunnable);
        handler.postDelayed(closeRunnable, AUTO_CLOSE_DELAY_MS);
    }

    private void startFlash() {
        // Animates a float 0→1 repeatedly; we snap color at the 0.5 midpoint
        // giving exactly equal on/off durations synced to the display vsync.
        flashAnimator = ValueAnimator.ofFloat(0f, 1f);
        flashAnimator.setDuration(FLASH_INTERVAL_MS);
        flashAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flashAnimator.setRepeatMode(ValueAnimator.RESTART);
        flashAnimator.setInterpolator(null); // linear
        flashAnimator.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            rootLayout.setBackgroundColor(f < 0.5f ? COLOR_BLUE : COLOR_BLACK);
        });
        flashAnimator.start();
    }

    private void stopFlash() {
        if (flashAnimator != null) {
            flashAnimator.cancel();
            flashAnimator = null;
        }
        rootLayout.setBackgroundColor(COLOR_BLACK);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFlash();
        handler.removeCallbacks(closeRunnable);
    }
}
