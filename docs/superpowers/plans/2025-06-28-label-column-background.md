# Label Column Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a subtle `#1C1C1C` background behind the vertical label column in all three telemetry slot types.

**Architecture:** A new color resource `telemetry_label_bg` is added to `colors.xml`, then a `<View>` with that color is placed behind the label TextView in each slot layout. The View fills the right column (guideline→parent end, top→bottom).

**Tech Stack:** Android XML layouts (ConstraintLayout, View)

## Global Constraints

- No Java code changes
- slot_throttle_brake.xml is untouched
- Background color: `#1C1C1C`
- Color resource name: `telemetry_label_bg`
- View constrained to full height of the right column (guideline→end, top→bottom)
- Build must succeed: `./gradlew assembleDebug`
- Tests must pass: `./gradlew test` (42/42)

---

### Task 1: colors.xml + slot_numeric.xml — add background

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/layout/slot_numeric.xml`

**Interfaces:**
- Produces: `@color/telemetry_label_bg` (new color resource)
- Consumes by Task 2, Task 3: `R.color.telemetry_label_bg`

- [ ] **Step 1: Add color resource**

In `app/src/main/res/values/colors.xml`, add after the `<color name="thermal_no_detect">` line:

```xml
    <color name="telemetry_label_bg">#1C1C1C</color>
```

Full colors.xml after edit:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>

    <!-- App specific colors -->
    <color name="background_primary">#FF000000</color>
    <color name="text_primary">#FFFFFFFF</color>
    <color name="alert_green">#FF00AA00</color>

    <!-- Telemetry warning/critical colors -->
    <color name="telemetry_warning">#FFFFCC00</color>
    <color name="telemetry_critical">#FFFF2222</color>
    <color name="telemetry_label">#FF888888</color>
    <color name="text_secondary">#FF888888</color>
    <color name="brake_touch">#FFFFCC00</color>
    <color name="brake_pressed">#FFFF2222</color>
    <color name="throttle_bar">#FF00AA00</color>
    <color name="telemetry_grid">#FF444444</color>
    
    <!-- SpeedHive gap trend colors -->
    <color name="gap_positive">#FF00CC00</color>
    <color name="gap_negative">#FFCC0000</color>

    <!-- Thermal zone temperature colors -->
    <color name="thermal_cold">#FF3B82F6</color>
    <color name="thermal_warm">#FF22C55E</color>
    <color name="thermal_hot">#FFEF4444</color>
    <color name="thermal_no_detect">#FF6B7280</color>
    <color name="telemetry_label_bg">#1C1C1C</color>
</resources>
```

- [ ] **Step 2: Add background View to slot_numeric.xml**

In `app/src/main/res/layout/slot_numeric.xml`, insert a `<View>` between the Guideline and slotLabel:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/slotGuideline"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_percent="0.88" />

    <TextView
        android:id="@+id/slotValue"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:fontFamily="monospace"
        android:gravity="center"
        android:maxLines="1"
        android:text="--"
        android:textColor="@color/text_primary"
        android:textStyle="bold"
        app:autoSizeMaxTextSize="200sp"
        app:autoSizeMinTextSize="20sp"
        app:autoSizeStepGranularity="2sp"
        app:autoSizeTextType="uniform"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toStartOf="@id/slotGuideline"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@color/telemetry_label_bg"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/slotGuideline"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/slotLabel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:rotation="90"
        android:textColor="@color/telemetry_label"
        android:textSize="24sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/slotGuideline"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Verify build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/layout/slot_numeric.xml
git commit -m "feat: add label column background color and apply to numeric slots"
```

---

### Task 2: slot_tyre.xml — add background

**Files:**
- Modify: `app/src/main/res/layout/slot_tyre.xml`

**Interfaces:**
- Consumes: `@color/telemetry_label_bg` from Task 1

- [ ] **Step 1: Add background View to slot_tyre.xml**

Replace `app/src/main/res/layout/slot_tyre.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/slotGuideline"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_percent="0.88" />

    <!-- Temperature at bottom -->
    <TextView
        android:id="@+id/slotTyreTemp"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:text="--°"
        android:textColor="@color/telemetry_label"
        android:textSize="40sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toStartOf="@id/slotGuideline"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- Pressure fills the remaining space (auto-size) -->
    <TextView
        android:id="@+id/slotTyrePres"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:fontFamily="monospace"
        android:gravity="center"
        android:maxLines="1"
        android:text="--"
        android:textColor="@color/text_primary"
        android:textStyle="bold"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        app:autoSizeMaxTextSize="120sp"
        app:autoSizeMinTextSize="16sp"
        app:autoSizeStepGranularity="2sp"
        app:autoSizeTextType="uniform"
        app:layout_constraintBottom_toTopOf="@id/slotTyreTemp"
        app:layout_constraintEnd_toStartOf="@id/slotGuideline"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@color/telemetry_label_bg"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/slotGuideline"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Tyre position label on right side, rotated vertical -->
    <TextView
        android:id="@+id/slotTyrePos"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:rotation="90"
        android:textColor="@color/telemetry_label"
        android:textSize="24sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/slotGuideline"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Verify build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/slot_tyre.xml
git commit -m "feat: add label column background to tyre slots"
```

---

### Task 3: slot_thermal.xml — add background

**Files:**
- Modify: `app/src/main/res/layout/slot_thermal.xml`

**Interfaces:**
- Consumes: `@color/telemetry_label_bg` from Task 1

- [ ] **Step 1: Add background View to slot_thermal.xml**

Replace `app/src/main/res/layout/slot_thermal.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/slotGuideline"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_percent="0.88" />

    <!-- Zone temperatures small at bottom -->
    <TextView
        android:id="@+id/slotThermalZones"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:text="--"
        android:textColor="@color/telemetry_label"
        android:textSize="18sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toStartOf="@id/slotGuideline"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- Max zone temperature fills the remaining space (auto-size) -->
    <TextView
        android:id="@+id/slotThermalMax"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:fontFamily="monospace"
        android:gravity="center"
        android:maxLines="1"
        android:text="--"
        android:textColor="@color/text_primary"
        android:textStyle="bold"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        app:autoSizeMaxTextSize="120sp"
        app:autoSizeMinTextSize="16sp"
        app:autoSizeStepGranularity="2sp"
        app:autoSizeTextType="uniform"
        app:layout_constraintBottom_toTopOf="@id/slotThermalZones"
        app:layout_constraintEnd_toStartOf="@id/slotGuideline"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@color/telemetry_label_bg"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/slotGuideline"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Position label on right side, rotated vertical -->
    <TextView
        android:id="@+id/slotThermalPos"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:fontFamily="monospace"
        android:gravity="center"
        android:rotation="90"
        android:textColor="@color/telemetry_label"
        android:textSize="24sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/slotGuideline"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Verify build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/slot_thermal.xml
git commit -m "feat: add label column background to thermal slots"
```

---

### Task 4: Final verification

- [ ] **Step 1: Run lint**

```bash
./gradlew lint
```

Expected: No new errors (pre-existing `module_troubleshoot.xml:39` `UseAppTint` error is unrelated)

- [ ] **Step 2: Run tests**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL, 42/42 tests pass

- [ ] **Step 3: Run full assemble**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL
