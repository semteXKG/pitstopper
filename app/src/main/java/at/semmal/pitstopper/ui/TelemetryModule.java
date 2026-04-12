package at.semmal.pitstopper.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Locale;

import at.semmal.pitstopper.R;
import at.semmal.pitstopper.model.TelemetryData;
import at.semmal.pitstopper.timing.PitWindowPreferences;

/**
 * Center module displaying a racing-style telemetry dashboard.
 * <p>
 * Layout is configurable: tiers (1/2/4 or 2/4) and sensor assignment per slot
 * are loaded from preferences each time the module activates.
 * <p>
 * Values are color-coded: white (normal), yellow (warning), red (critical).
 */
public class TelemetryModule extends CenterModule {

    // Warning / critical thresholds
    private int rpmWarn, rpmCrit;
    private boolean rpmAlarm;
    private int coolantWarn, coolantCrit;
    private boolean coolantAlarm;
    private int oilTempWarn, oilTempCrit;
    private boolean oilTempAlarm;
    private float oilPresWarn, oilPresCrit;
    private boolean oilPresAlarm;
    private float batteryWarn, batteryCrit;
    private boolean batteryAlarm;

    private final int colorNormal;
    private final int colorWarning;
    private final int colorCritical;
    private final int colorBrakeTouch;
    private final int colorBrakePressed;
    private final int colorDim;

    // Tier containers and grid (from XML)
    private TelemetryGridView telemetryGrid;
    private LinearLayout tier1Container;
    private LinearLayout tier2Container;
    private LinearLayout tier3Container;

    // Per-sensor view refs — null when sensor is not in the current layout
    private TextView tyrePresViewFL, tyreTempViewFL;
    private TextView tyrePresViewFR, tyreTempViewFR;
    private TextView tyrePresViewRL, tyreTempViewRL;
    private TextView tyrePresViewRR, tyreTempViewRR;
    private TextView rpmValueView;
    private TextView speedValueView;
    private View throttleBarFill;
    private FrameLayout throttleBarContainer;
    private TextView throttleValueView;
    private View brakeIndicatorView;
    private TextView coolantValueView;
    private TextView oilTempValueView;
    private TextView oilPresValueView;
    private TextView batteryValueView;

    private final TelemetryData data = new TelemetryData();
    private final PitWindowPreferences preferences;

    public TelemetryModule(Context context, PitWindowPreferences preferences) {
        super(context);
        this.preferences = preferences;
        LayoutInflater.from(context).inflate(R.layout.module_telemetry, this, true);

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

        telemetryGrid  = findViewById(R.id.telemetryGrid);
        tier1Container = findViewById(R.id.tier1Container);
        tier2Container = findViewById(R.id.tier2Container);
        tier3Container = findViewById(R.id.tier3Container);
    }

    public TelemetryData getData() {
        return data;
    }

    /** Update RPM, speed, throttle from CAN 0x201. Call on main thread. */
    public void updateCan201(int rpm, float speedKmh, float throttlePct) {
        data.setCan201(rpm, speedKmh, throttlePct);

        if (rpmValueView != null) {
            rpmValueView.setText(String.valueOf(rpm));
            rpmValueView.setTextColor(rpmAlarm
                    ? colorForHighValue(rpm, rpmWarn, rpmCrit) : colorNormal);
        }
        if (speedValueView != null) {
            speedValueView.setText(String.valueOf(Math.round(speedKmh)));
        }
        if (throttleValueView != null && throttleBarFill != null) {
            float pct = Math.max(0, Math.min(100, throttlePct));
            throttleValueView.setText(String.format(Locale.US, "%.0f%%", pct));
            ViewGroup.LayoutParams lp = throttleBarFill.getLayoutParams();
            int containerWidth = throttleBarContainer.getWidth();
            if (containerWidth > 0) {
                lp.width = Math.round(containerWidth * pct / 100f);
                throttleBarFill.setLayoutParams(lp);
            }
        }
    }

    /** Update brake pedal state from CAN 0x360. Call on main thread. */
    public void updateBrake(String brakePedal) {
        data.setBrakePedal(brakePedal);
        if (brakeIndicatorView != null) {
            switch (brakePedal) {
                case "pressed":
                    brakeIndicatorView.setBackgroundColor(colorBrakePressed);
                    break;
                case "touch":
                    brakeIndicatorView.setBackgroundColor(colorBrakeTouch);
                    break;
                default:
                    brakeIndicatorView.setBackgroundColor(colorDim);
                    break;
            }
        }
    }

    /** Update coolant + brake from CAN 0x420. Call on main thread. */
    public void updateCan420(int coolantC, String brakePedal) {
        data.setCan420(coolantC, brakePedal);
        if (coolantValueView != null) {
            coolantValueView.setText(String.format(Locale.US, "%d°", coolantC));
            coolantValueView.setTextColor(coolantAlarm
                    ? colorForHighValue(coolantC, coolantWarn, coolantCrit) : colorNormal);
        }
        updateBrake(brakePedal);
    }

    /** Update oil temp + oil pressure from fiesta/sensors. Call on main thread. */
    public void updateSensors(int oilTemp, float oilPres) {
        data.setSensors(oilTemp, oilPres);
        if (oilTempValueView != null) {
            oilTempValueView.setText(String.format(Locale.US, "%d°", oilTemp));
            oilTempValueView.setTextColor(oilTempAlarm
                    ? colorForHighValue(oilTemp, oilTempWarn, oilTempCrit) : colorNormal);
        }
        if (oilPresValueView != null) {
            oilPresValueView.setText(String.format(Locale.US, "%.1f", oilPres));
            oilPresValueView.setTextColor(oilPresAlarm
                    ? colorForLowValue(oilPres, oilPresWarn, oilPresCrit) : colorNormal);
        }
    }

    /** Update battery voltage from CAN 0x428. Call on main thread. */
    public void updateBattery(float batteryV) {
        data.setBatteryV(batteryV);
        if (batteryValueView != null) {
            batteryValueView.setText(String.format(Locale.US, "%.1fV", batteryV));
            batteryValueView.setTextColor(batteryAlarm
                    ? colorForLowValue(batteryV, batteryWarn, batteryCrit) : colorNormal);
        }
    }

    /** Update tyre pressure + temperature from fiesta/tpms/{pos}. Call on main thread. */
    public void updateTpms(String pos, Float presBar, int tempC, boolean alarm) {
        data.setTyre(pos, presBar, tempC, alarm);
        TextView presView, tempView;
        switch (pos) {
            case "fl": presView = tyrePresViewFL; tempView = tyreTempViewFL; break;
            case "fr": presView = tyrePresViewFR; tempView = tyreTempViewFR; break;
            case "rl": presView = tyrePresViewRL; tempView = tyreTempViewRL; break;
            case "rr": presView = tyrePresViewRR; tempView = tyreTempViewRR; break;
            default:   return;
        }
        if (presView != null) {
            presView.setText(presBar != null
                    ? String.format(Locale.US, "%.2f", presBar) : "--");
            presView.setTextColor(alarm ? colorCritical : colorNormal);
        }
        if (tempView != null) {
            tempView.setText(String.format(Locale.US, "%d°", tempC));
            tempView.setTextColor(alarm ? colorCritical : colorNormal);
        }
    }

    @Override
    public void onActivate() {
        setVisibility(View.VISIBLE);
        buildLayout();
    }

    @Override
    public void onDeactivate() {
        animate().cancel();
        setTranslationY(0);
        setVisibility(View.GONE);
    }

    // ── Layout construction ────────────────────────────────────────────────────

    public void buildLayout() {
        tier1Container.removeAllViews();
        tier2Container.removeAllViews();
        tier3Container.removeAllViews();
        clearSensorRefs();

        TelemetryLayout layout = preferences.getTelemetryLayout();
        TelemetrySensor[] sensors = preferences.getSlotSensors(layout);

        telemetryGrid.setTelemetryLayout(layout);

        if (layout == TelemetryLayout.LAYOUT_1_2_4) {
            setTierWeights(42, 30, 28);
            tier2Container.setVisibility(View.VISIBLE);
            populateTier(tier1Container, sensors, 0, 1);
            populateTier(tier2Container, sensors, 1, 3);
            populateTier(tier3Container, sensors, 3, 7);
        } else { // LAYOUT_2_4
            setTierWeights(50, 0, 50);
            tier2Container.setVisibility(View.GONE);
            populateTier(tier1Container, sensors, 0, 2);
            populateTier(tier3Container, sensors, 2, 6);
        }
    }

    private void setTierWeights(int w1, int w2, int w3) {
        setWeight(tier1Container, w1);
        setWeight(tier2Container, w2);
        setWeight(tier3Container, w3);
    }

    private void setWeight(View v, int weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) v.getLayoutParams();
        params.weight = weight;
        v.setLayoutParams(params);
    }

    private void populateTier(LinearLayout container, TelemetrySensor[] sensors, int from, int to) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = from; i < to; i++) {
            View slot = inflateSlot(inflater, sensors[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            slot.setLayoutParams(params);
            container.addView(slot);
        }
    }

    private View inflateSlot(LayoutInflater inflater, TelemetrySensor sensor) {
        switch (sensor) {
            case RPM: {
                View v = inflater.inflate(R.layout.slot_numeric, null);
                ((TextView) v.findViewById(R.id.slotLabel)).setText("RPM");
                rpmValueView = v.findViewById(R.id.slotValue);
                if (data.hasRpm()) rpmValueView.setText(String.valueOf(data.getRpm()));
                return v;
            }
            case SPEED: {
                View v = inflater.inflate(R.layout.slot_numeric, null);
                ((TextView) v.findViewById(R.id.slotLabel)).setText("km/h");
                speedValueView = v.findViewById(R.id.slotValue);
                if (data.hasSpeed()) speedValueView.setText(String.valueOf(Math.round(data.getSpeedKmh())));
                return v;
            }
            case COOLANT: {
                View v = inflater.inflate(R.layout.slot_numeric, null);
                ((TextView) v.findViewById(R.id.slotLabel)).setText("COOL");
                coolantValueView = v.findViewById(R.id.slotValue);
                if (data.hasCoolant()) coolantValueView.setText(String.format(Locale.US, "%d°", data.getCoolantC()));
                return v;
            }
            case OIL_TEMP: {
                View v = inflater.inflate(R.layout.slot_numeric, null);
                ((TextView) v.findViewById(R.id.slotLabel)).setText("OIL T");
                oilTempValueView = v.findViewById(R.id.slotValue);
                if (data.hasOilTemp()) oilTempValueView.setText(String.format(Locale.US, "%d°", data.getOilTemp()));
                return v;
            }
            case OIL_PRES: {
                View v = inflater.inflate(R.layout.slot_numeric, null);
                ((TextView) v.findViewById(R.id.slotLabel)).setText("OIL P");
                oilPresValueView = v.findViewById(R.id.slotValue);
                if (data.hasOilPres()) oilPresValueView.setText(String.format(Locale.US, "%.1f", data.getOilPres()));
                return v;
            }
            case BATTERY: {
                View v = inflater.inflate(R.layout.slot_numeric, null);
                ((TextView) v.findViewById(R.id.slotLabel)).setText("BATT");
                batteryValueView = v.findViewById(R.id.slotValue);
                if (data.hasBattery()) batteryValueView.setText(String.format(Locale.US, "%.1fV", data.getBatteryV()));
                return v;
            }
            case THROTTLE_BRAKE: {
                View v = inflater.inflate(R.layout.slot_throttle_brake, null);
                throttleBarFill = v.findViewById(R.id.throttleBarFill);
                throttleBarContainer = v.findViewById(R.id.throttleBarContainer);
                throttleValueView = v.findViewById(R.id.slotThrottleValue);
                brakeIndicatorView = v.findViewById(R.id.brakeIndicator);
                return v;
            }
            case TYRE_FL:
                return inflateTyreSlot(inflater, "FL", "fl",
                        data.hasTyreFL(), data.getTyprePresFL(), data.getTyreTempFL(), data.getTyreAlarmFL());
            case TYRE_FR:
                return inflateTyreSlot(inflater, "FR", "fr",
                        data.hasTyreFR(), data.getTyprePresFR(), data.getTyreTempFR(), data.getTyreAlarmFR());
            case TYRE_RL:
                return inflateTyreSlot(inflater, "RL", "rl",
                        data.hasTyreRL(), data.getTyprePresRL(), data.getTyreTempRL(), data.getTyreAlarmRL());
            case TYRE_RR:
                return inflateTyreSlot(inflater, "RR", "rr",
                        data.hasTyreRR(), data.getTyprePresRR(), data.getTyreTempRR(), data.getTyreAlarmRR());
            case EMPTY:
            default:
                return new View(getContext());
        }
    }

    private View inflateTyreSlot(LayoutInflater inflater, String label, String posKey,
            boolean hasData, Float presBar, int tempC, boolean alarm) {
        View v = inflater.inflate(R.layout.slot_tyre, null);
        ((TextView) v.findViewById(R.id.slotTyrePos)).setText(label);
        TextView presView = v.findViewById(R.id.slotTyrePres);
        TextView tempView = v.findViewById(R.id.slotTyreTemp);
        switch (posKey) {
            case "fl": tyrePresViewFL = presView; tyreTempViewFL = tempView; break;
            case "fr": tyrePresViewFR = presView; tyreTempViewFR = tempView; break;
            case "rl": tyrePresViewRL = presView; tyreTempViewRL = tempView; break;
            case "rr": tyrePresViewRR = presView; tyreTempViewRR = tempView; break;
        }
        if (hasData) {
            presView.setText(presBar != null
                    ? String.format(Locale.US, "%.2f", presBar) : "--");
            presView.setTextColor(alarm ? colorCritical : colorNormal);
            tempView.setText(String.format(Locale.US, "%d°", tempC));
            tempView.setTextColor(alarm ? colorCritical : colorNormal);
        }
        return v;
    }

    private void clearSensorRefs() {
        rpmValueView = null;
        speedValueView = null;
        throttleBarFill = null;
        throttleBarContainer = null;
        throttleValueView = null;
        brakeIndicatorView = null;
        coolantValueView = null;
        oilTempValueView = null;
        oilPresValueView = null;
        batteryValueView = null;
        tyrePresViewFL = null; tyreTempViewFL = null;
        tyrePresViewFR = null; tyreTempViewFR = null;
        tyrePresViewRL = null; tyreTempViewRL = null;
        tyrePresViewRR = null; tyreTempViewRR = null;
    }

    // ── Color helpers ──────────────────────────────────────────────────────────

    /** Color for values where HIGH is bad (temp, RPM). */
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
}
