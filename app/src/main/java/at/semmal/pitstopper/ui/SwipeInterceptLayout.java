package at.semmal.pitstopper.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * FrameLayout that detects vertical flings for module switching.
 * <p>
 * When a RecyclerView is active (ChatModule), it intercepts the gesture at
 * the scroll boundary so the RecyclerView doesn't consume it, and detects
 * the fling via {@code dispatchTouchEvent} which sees every event regardless
 * of which child handles them.
 * <p>
 * When no RecyclerView is active (PitTimerModule), flings are detected
 * the same way — children don't consume touch, so everything just works.
 */
public class SwipeInterceptLayout extends FrameLayout {

    public interface OnModuleSwipeListener {
        void onSwipe(boolean swipeUp);
    }

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private final int touchSlop;
    private final GestureDetector gestureDetector;

    private float startY;
    private boolean intercepting;

    @Nullable private RecyclerView activeRecyclerView;
    @Nullable private OnModuleSwipeListener listener;

    public SwipeInterceptLayout(@NonNull Context context) {
        this(context, null);
    }

    public SwipeInterceptLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeInterceptLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float deltaY = e2.getY() - e1.getY();
                if (Math.abs(deltaY) < SWIPE_THRESHOLD || Math.abs(velocityY) < SWIPE_VELOCITY_THRESHOLD) {
                    return false;
                }

                boolean swipeUp = deltaY < 0;

                if (activeRecyclerView != null) {
                    boolean childCanScroll = swipeUp
                            ? activeRecyclerView.canScrollVertically(1)
                            : activeRecyclerView.canScrollVertically(-1);
                    if (childCanScroll) return false;
                }

                if (listener != null) {
                    listener.onSwipe(swipeUp);
                    return true;
                }
                return false;
            }
        });
    }

    public void setOnModuleSwipeListener(@Nullable OnModuleSwipeListener l) {
        this.listener = l;
    }

    public void setActiveRecyclerView(@Nullable RecyclerView rv) {
        this.activeRecyclerView = rv;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Let the GestureDetector observe every event before normal dispatch,
        // so it always sees the full DOWN→MOVE→UP sequence for fling detection.
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (activeRecyclerView == null) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                intercepting = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (intercepting) return true;

                float dy = ev.getY() - startY;
                if (Math.abs(dy) < touchSlop) break;

                boolean draggingDown = dy > 0;
                boolean childCanScroll = draggingDown
                        ? activeRecyclerView.canScrollVertically(-1)
                        : activeRecyclerView.canScrollVertically(1);

                if (!childCanScroll) {
                    intercepting = true;
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                intercepting = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Consume intercepted events so the touch sequence continues to flow
        // through dispatchTouchEvent → GestureDetector.
        return true;
    }
}
