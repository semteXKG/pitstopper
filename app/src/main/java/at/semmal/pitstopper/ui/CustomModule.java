package at.semmal.pitstopper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import at.semmal.pitstopper.R;

/**
 * Placeholder center module — implement your custom UI here.
 * Inflate module_custom.xml and add your views/logic.
 */
public class CustomModule extends CenterModule {

    public CustomModule(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.module_custom, this, true);
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
