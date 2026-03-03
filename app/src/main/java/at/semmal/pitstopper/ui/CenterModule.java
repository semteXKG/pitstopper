package at.semmal.pitstopper.ui;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * Abstract base class for interchangeable center-panel modules.
 * Each module occupies the space between the vertical pit-timer bar (left)
 * and the live timing panel (right) in the main activity.
 */
public abstract class CenterModule extends FrameLayout {

    public CenterModule(Context context) {
        super(context);
    }

    /** Called when this module becomes the active/visible module. */
    public abstract void onActivate();

    /** Called when this module is replaced by another module. */
    public abstract void onDeactivate();
}
