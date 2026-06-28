# Label Column Background — Design Spec

**Date:** 2025-06-28
**Status:** approved

## Goal

Add a subtle background behind the vertical label column (right ~12% of each telemetry slot) from top to bottom, so the label area has visual separation without being prominent.

## Color

New color resource: `telemetry_label_bg` = `#1C1C1C` — barely lighter than the black background (`#FF000000`), creating a subtle column distinction.

## Layout

In each of the 3 slot XML files, add a `<View>` between the Guideline and the label TextView:

- Constrained: `start_toStartOf="@id/slotGuideline"`, `end_toEndOf="parent"`, `top_toTopOf="parent"`, `bottom_toBottomOf="parent"`
- `android:background="@color/telemetry_label_bg"`
- Declared **before** the label TextView so it draws behind it

The label TextView (with `rotation="90"`) renders on top unchanged.

## Files changed

1. `app/src/main/res/values/colors.xml` — add `<color name="telemetry_label_bg">#1C1C1C</color>`
2. `app/src/main/res/layout/slot_numeric.xml` — add View behind slotLabel
3. `app/src/main/res/layout/slot_tyre.xml` — add View behind slotTyrePos
4. `app/src/main/res/layout/slot_thermal.xml` — add View behind slotThermalPos

## Not changed

- `slot_throttle_brake.xml`
- `TelemetryModule.java`
