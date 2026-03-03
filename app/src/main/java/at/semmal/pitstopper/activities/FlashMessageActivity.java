package at.semmal.pitstopper.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import at.semmal.pitstopper.R;

public class FlashMessageActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "flash_message";
    private static final long AUTO_CLOSE_DELAY_MS = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable closeRunnable = this::finish;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_message);
        textView = findViewById(R.id.textFlashMessage);
        showMessage(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showMessage(intent);
    }

    private void showMessage(android.content.Intent intent) {
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        textView.setText(message != null ? message : "");
        handler.removeCallbacks(closeRunnable);
        handler.postDelayed(closeRunnable, AUTO_CLOSE_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(closeRunnable);
    }
}
