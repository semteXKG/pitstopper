# Alternating Telemetry Slot Labels — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox syntax for tracking.

**Goal:** Make adjacent telemetry slot labels meet at the shared cell boundary by alternating label sides per slot index.

**Architecture:** Add mirrored "left-label" XML layouts for numeric, tyre, and thermal slots. Update `TelemetryModule.populateTier`/`inflateSlot` to select the normal or mirrored layout based on the slot's column index.

**Tech Stack:** Android XML layouts, Java 11.

## Global Constraints

- No Kotlin source changes.
- Keep `slot_throttle_brake.xml` unchanged.
- Preserve all existing view IDs in mirrored layouts so `TelemetryModule` view references keep working.

---

### Task 1: Create mirrored left-label layouts

**Files:**
- Create: `app/src/main/res/layout/slot_numeric_left.xml`
- Create: `app/src/main/res/layout/slot_tyre_left.xml`
- Create: `app/src/main/res/layout/slot_thermal_left.xml`

**Interfaces:**
- Produces: three new layout resources usable by `TelemetryModule`.

- [ ] **Step 1: Copy `slot_numeric.xml` to `slot_numeric_left.xml`**

- [ ] **Step 2: Mirror the numeric layout**

  - Change guideline percent from `0.88` to `0.12`.
  - Change `slotValue` constraints from `start-to-start`/`end-to-startOf guideline` to `start-to-startOf guideline`/`end-to-end`.
  - Change label background View constraints from `start-to-startOf guideline`/`end-to-end` to `start-to-start`/`end-to-endOf guideline`.
  - Change `slotLabel` constraints from `start-to-startOf guideline`/`end-to-end` to `start-to-start`/`end-to-endOf guideline`.

- [ ] **Step 3: Mirror `slot_tyre.xml` into `slot_tyre_left.xml` with the same pattern**

  - Guideline at `0.12`.
  - `slotTyrePres` and `slotTyreTemp` to the right of the guideline.
  - Label background and `slotTyrePos` to the left of the guideline.

- [ ] **Step 4: Mirror `slot_thermal.xml` into `slot_thermal_left.xml` with the same pattern**

  - Guideline at `0.12`.
  - `slotThermalMax` and `slotThermalZones` to the right of the guideline.
  - Label background and `slotThermalPos` to the left of the guideline.

- [ ] **Step 5: Build to verify resources compile**

  Run: `./gradlew assembleDebug`
  Expected: `BUILD SUCCESSFUL`

---

### Task 2: Wire layout selection into TelemetryModule

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/ui/TelemetryModule.java`

**Interfaces:**
- Consumes: the three new `_left` layouts.
- Produces: `populateTier` and `inflateSlot` choose layout based on slot column index.

- [ ] **Step 1: Pass column index into `inflateSlot`**

  Change signature from `inflateSlot(LayoutInflater inflater, TelemetrySensor sensor)` to `inflateSlot(LayoutInflater inflater, TelemetrySensor sensor, int columnIndex)`.

- [ ] **Step 2: Update `populateTier` to compute column index**

  In the loop `for (int i = from; i < to; i++)`, compute `int columnIndex = i - from;` and pass it to `inflateSlot`.

- [ ] **Step 3: Select normal or mirrored layout in `inflateSlot`**

  For `RPM`, `SPEED`, `COOLANT`, `OIL_TEMP`, `OIL_PRES`, `BATTERY`: inflate `slot_numeric.xml` if `columnIndex` is even, else `slot_numeric_left.xml`.
  
  For `TYRE_*`: inflate `slot_tyre.xml` if even, else `slot_tyre_left.xml`.
  
  For `THERMAL_*`: inflate `slot_thermal.xml` if even, else `slot_thermal_left.xml`.
  
  `THROTTLE_BRAKE` and `EMPTY` remain unchanged.

- [ ] **Step 4: Build and test**

  Run: `./gradlew assembleDebug && ./gradlew test`
  Expected: `BUILD SUCCESSFUL` for both.
