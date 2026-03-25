package at.semmal.pitstopper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Locale;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.model.TelemetryData;
import at.semmal.pitstopper.timing.PitWindowPreferences;

/**
 * Center module displaying a racing-style telemetry dashboard.
 * <p>
 * Layout: RPM (top, largest) → Speed + Throttle bar + Brake indicator (middle)
 * → Engine health grid: Coolant, Oil Temp, Oil Pressure, Battery (bottom).
 * <p>
 * Values are color-coded: white (normal), yellow (warning), red (critical).
 * Thresholds and alarm enable/disable are loaded from preferences.
 */
public class TelemetryModule extends CenterModule {

    // Warning / critical thresholds (loaded from preferences)
    private int rpmWarn;
    private int rpmCrit;
    private boolean rpmAlarm;
    private int coolantWarn;
    private int coolantCrit;
    private boolean coolantAlarm;
    private int oilTempWarn;
    private int oilTempCrit;
    private boolean oilTempAlarm;
    private float oilPresWarn;
    private float oilPresCrit;
    private boolean oilPresAlarm;
    private float batteryWarn;
    private float batteryCrit;
    private boolean batteryAlarm;

    private final int colorNormal;
    private final int colorWarning;
    private final int colorCritical;
    private final int colorBrakeTouch;
    private final int colorBrakePressed;
    private final int colorDim;

    // Tier 1
    private final TextView textRpmValue;

    // Tier 2
    private final TextView textSpeedValue;
    private final View throttleBarFill;
    private final FrameLayout throttleBarContainer;
    private final TextView textThrottleValue;
    private final View brakeIndicator;

    // Tier 3
    private final TextView textCoolantValue;
    private final TextView textOilTempValue;
    private final TextView textOilPresValue;
    private final TextView textBatteryValue;

    private final TelemetryData data = new TelemetryData();

    public TelemetryModule(Context context, PitWindowPreferences preferences) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.module_telemetry, this, true);

        // Load thresholds from preferences
        rpmWarn = preferences.getRpmWarn();
        rpmCrit = preferences.getRpmCrit();
        rpmAlarm = preferences.isRpmAlarm();
        coolantWarn = preferences.getCoolantWarn();
        coolantCrit = preferences.getCoolantCrit();
        coolantAlarm = preferences.isCoolantAlarm();
        oilTempWarn = preferences.getOilTempWarn();
        oilTempCrit = preferences.getOilTempCrit();
        oilTempAlarm = preferences.isOilTempAlarm();
        oilPresWarn = preferences.getOilPresWarn();
        oilPresCrit = preferences.getOilPresCrit();
        oilPresAlarm = preferences.isOilPresAlarm();
        batteryWarn = preferences.getBatteryWarn();
        batteryCrit = preferences.getBatteryCrit();
        batteryAlarm = preferences.isBatteryAlarm();

        colorNormal = ContextCompat.getColor(context, R.color.text_primary);
        colorWarning = ContextCompat.getColor(context, R.color.telemetry_warning);
        colorCritical = ContextCompat.getColor(context, R.color.telemetry_critical);
        colorBrakeTouch = ContextCompat.getColor(context, R.color.brake_touch);
        colorBrakePressed = ContextCompat.getColor(context, R.color.brake_pressed);
        colorDim = 0xFF333333;

        textRpmValue = findViewById(R.id.textRpmValue);
        textSpeedValue = findViewById(R.id.textSpeedValue);
        throttleBarFill = findViewById(R.id.throttleBarFill);
        throttleBarContainer = findViewById(R.id.throttleBarContainer);
        textThrottleValue = findViewById(R.id.textThrottleValue);
        brakeIndicator = findViewById(R.id.brakeIndicator);
        textCoolantValue = findViewById(R.id.textCoolantValue);
        textOilTempValue = findViewById(R.id.textOilTempValue);
        textOilPresValue = findViewById(R.id.textOilPresValue);
        textBatteryValue = findViewById(R.id.textBatteryValue);
    }

    public TelemetryData getData() {
        return data;
    }

    /** Update RPM, speed, throttle from CAN 0x201. Call on main thread. */
    public void updateCan201(int rpm, float speedKmh, float throttlePct) {
        data.setCan201(rpm, speedKmh, throttlePct);

        textRpmValue.setText(String.valueOf(rpm));
        textRpmValue.setTextColor(rpmAlarm
                ? colorForHighValue(rpm, rpmWarn, rpmCrit) : colorNormal);
        textSpeedValue.setText(String.valueOf(Math.round(speedKmh)));

        // Throttle bar fill
        float pct = Math.max(0, Math.min(100, throttlePct));
        textThrottleValue.setText(String.format(Locale.US, "%.0f%%", pct));
        ViewGroup.LayoutParams lp = throttleBarFill.getLayoutParams();
        int containerWidth = throttleBarContainer.getWidth();
        if (containerWidth > 0) {
            lp.width = Math.round(containerWidth * pct / 100f);
            throttleBarFill.setLayoutParams(lp);
        }
    }

    /** Update brake pedal state from CAN 0x360. Call on main thread. */
    public void updateBrake(String brakePedal) {
        data.setBrakePedal(brakePedal);
        switch (brakePedal) {
            case "pressed":
                brakeIndicator.setBackgroundColor(colorBrakePressed);
                break;
            case "touch":
                brakeIndicator.setBackgroundColor(colorBrakeTouch);
                break;
            default:
                brakeIndicator.setBackgroundColor(colorDim);
                break;
        }
    }

    /** Update coolant + brake from CAN 0x420. Call on main thread. */
    public void updateCan420(int coolantC, String brakePedal) {
        data.setCan420(coolantC, brakePedal);
        textCoolantValue.setText(String.format(Locale.US, "%d°", coolantC));
        textCoolantValue.setTextColor(coolantAlarm
                ? colorForHighValue(coolantC, coolantWarn, coolantCrit) : colorNormal);
        updateBrake(brakePedal);
    }

    /** Update oil temp + oil pressure from fiesta/sensors. Call on main thread. */
    public void updateSensors(int oilTemp, float oilPres) {
        data.setSensors(oilTemp, oilPres);
        textOilTempValue.setText(String.format(Locale.US, "%d°", oilTemp));
        textOilTempValue.setTextColor(oilTempAlarm
                ? colorForHighValue(oilTemp, oilTempWarn, oilTempCrit) : colorNormal);
        textOilPresValue.setText(String.format(Locale.US, "%.1f", oilPres));
        textOilPresValue.setTextColor(oilPresAlarm
                ? colorForLowValue(oilPres, oilPresWarn, oilPresCrit) : colorNormal);
    }

    /** Update battery voltage from CAN 0x428. Call on main thread. */
    public void updateBattery(float batteryV) {
        data.setBatteryV(batteryV);
        textBatteryValue.setText(String.format(Locale.US, "%.1fV", batteryV));
        textBatteryValue.setTextColor(batteryAlarm
                ? colorForLowValue(batteryV, batteryWarn, batteryCrit) : colorNormal);
    }

    /** Color for values where HIGH is bad (temp). */
    private int colorForHighValue(float value, float warn, float crit) {
        if (value >= crit) return colorCritical;
        if (value >= warn) return colorWarning;
        return colorNormal;
    }

    /** Color for values where LOW is bad (pressure, voltage). */
    private int colorForLowValue(float value, float warn, float crit) {
        if (value <= crit) return colorCritical;
        if (value <= warn) return colorWarning;
        return colorNormal;
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
