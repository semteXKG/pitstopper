# Vertical Sensor Labels — Design Spec

**Date:** 2025-06-28
**Status:** approved

## Goal

Move sensor name labels from occupying a full horizontal row (bottom of numeric slots, top of tyre/thermal slots) to a narrow vertical strip on the right side, so the measured value gets the full slot height.

## Scope

| Slot layout | Change |
|---|---|
| `slot_numeric.xml` | Label moves from centered-bottom to vertical right side |
| `slot_tyre.xml` | Position label (FL/FR/RL/RR) moves from top to vertical right side |
| `slot_thermal.xml` | Position label (FL/FR/RL/RR) moves from top to vertical right side |
| `slot_throttle_brake.xml` | **Unchanged** |

No Java changes — `TelemetryModule.java` references (`slotLabel`, `slotTyrePos`, `slotThermalPos`) remain the same.

## Layout pattern (all 3 files)

Each slot uses a ConstraintLayout with:

1. A **vertical guideline** at `app:layout_constraintGuide_percent="0.88"` — creates a ~12% right column for the label.
2. The **value** is constrained `start-to-start` of parent and `end-to-start` of the guideline, filling the full height.
3. The **label** is constrained `start-to-start` of the guideline, `end-to-end` of parent, `top-to-top` and `bottom-to-bottom` of parent, with `android:rotation="90"` and `android:gravity="center"`.

```
┌──────────────────────┬──┐
│                      │  │
│        VALUE         │R │
│                      │P │
│                      │M │
│                      │  │
│    (secondary)       │  │
└──────────────────────┴──┘
```

### slot_numeric.xml

- `slotValue`: uses auto-size, fills left of guideline, full height
- `slotLabel`: right column, `rotation="90"`, text size reduced to 24sp (from 44sp)

### slot_tyre.xml

- `slotTyrePos`: right column, `rotation="90"`, text size 24sp (from 36sp)
- `slotTyrePres`: fills left of guideline, top-to-bottom (full height), auto-size
- `slotTyreTemp`: left of guideline, constrained to bottom

### slot_thermal.xml

- `slotThermalPos`: right column, `rotation="90"`, text size 24sp (from 36sp)
- `slotThermalMax`: fills left of guideline, full height, auto-size
- `slotThermalZones`: left of guideline, constrained to bottom

## Label text size rationale

Pre-rotation, the label text size controls the view height. After `rotation="90"`, this becomes the visual *width* (horizontal space the label steals from the value). 24sp keeps the right column narrow (~12% of slot width) while remaining readable.

## Files changed

- `app/src/main/res/layout/slot_numeric.xml`
- `app/src/main/res/layout/slot_tyre.xml`
- `app/src/main/res/layout/slot_thermal.xml`

## Not changed

- `app/src/main/res/layout/slot_throttle_brake.xml`
- `app/src/main/java/at/semmal/pitstopper/ui/TelemetryModule.java`
