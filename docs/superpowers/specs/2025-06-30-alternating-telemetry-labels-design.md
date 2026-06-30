# Alternating Telemetry Slot Labels — Design Spec

**Date:** 2025-06-30
**Status:** approved

## Goal

In tiers with multiple side-by-side slots, make adjacent slot labels meet at the shared cell boundary instead of both hugging the outer right edge.

## Layout rule

Within any tier, label position alternates by slot index:

- **Even-indexed slot** (0, 2, …): label on the **right** side (current behavior).
- **Odd-indexed slot** (1, 3, …): label on the **left** side.

Result for a 2-column row:

```
┌──────────────┬──┐┌──┬──────────────┐
│              │A ││B │              │
│    VALUE     │  ││  │    VALUE     │
│              │  ││  │              │
└──────────────┴──┘└──┴──────────────┘
```

For 4-column rows the same rule applies, producing two label pairs.

## Scope

| Slot layout | Change |
|---|---|
| `slot_numeric.xml` | Keep right-label as default; add `slot_numeric_left.xml` mirrored variant |
| `slot_tyre.xml` | Keep right-label as default; add `slot_tyre_left.xml` mirrored variant |
| `slot_thermal.xml` | Keep right-label as default; add `slot_thermal_left.xml` mirrored variant |
| `slot_throttle_brake.xml` | **Unchanged** — no side label |
| `TelemetryModule.java` | Pass slot column index into `inflateSlot`; pick normal or mirrored layout |

## Mirrored layout details

A left-label slot is a horizontal mirror of the existing right-label slot:

- Guideline moves from `0.88` to `0.12`.
- Value area is constrained to the **right** of the guideline.
- Label background and rotated label are constrained to the **left** of the guideline.
- Text rotation stays `90°`; text content and styling stay identical.

## Files changed

- `app/src/main/res/layout/slot_numeric_left.xml` (new)
- `app/src/main/res/layout/slot_tyre_left.xml` (new)
- `app/src/main/res/layout/slot_thermal_left.xml` (new)
- `app/src/main/java/at/semmal/pitstopper/ui/TelemetryModule.java`

## Not changed

- `app/src/main/res/layout/slot_throttle_brake.xml`
- `app/src/main/java/at/semmal/pitstopper/ui/TelemetryGridView.java`
