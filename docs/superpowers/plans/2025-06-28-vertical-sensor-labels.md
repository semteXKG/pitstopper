# Vertical Sensor Labels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move sensor name labels from occupying a horizontal row to a narrow vertical strip on the right side of each telemetry slot.

**Architecture:** Three XML layout files are modified to add a `Guideline` at 88% width, reposition the label to the right column with `rotation="90"`, and expand the value to fill the left 88%. No Java changes needed — the existing `findViewById` references remain valid.

**Tech Stack:** Android XML layouts (ConstraintLayout), `android:rotation="90"`, `Guideline`

## Global Constraints

- No Java code changes
- slot_throttle_brake.xml is untouched
- Label text size: 24sp (down from 44sp numeric / 36sp tyre+thermal)
- Guideline at 88% for all three slot types
- Build must succeed: `./gradlew assembleDebug`

---

### Task 1: slot_numeric.xml — vertical label

**Files:**
- Modify: `app/src/main/res/layout/slot_numeric.xml`

**Interfaces:**
- Produces: `slotLabel` (id unchanged), `slotValue` (id unchanged), `slotGuideline` (new id)
- Consumes by TelemetryModule.java: `findViewById(R.id.slotLabel)`, `findViewById(R.id.slotValue)` — unchanged

- [ ] **Step 1: Replace slot_numeric.xml**

Replace the entire file content with:

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

- [ ] **Step 2: Verify XML compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/slot_numeric.xml
git commit -m "feat: move numeric sensor labels to vertical right side"
```

---

### Task 2: slot_tyre.xml — vertical position label

**Files:**
- Modify: `app/src/main/res/layout/slot_tyre.xml`

**Interfaces:**
- Produces: `slotTyrePos` (id unchanged), `slotTyrePres` (id unchanged), `slotTyreTemp` (id unchanged)
- Consumes by TelemetryModule.java: `findViewById(R.id.slotTyrePos)`, `findViewById(R.id.slotTyrePres)`, `findViewById(R.id.slotTyreTemp)` — unchanged

- [ ] **Step 1: Replace slot_tyre.xml**

Replace the entire file content with:

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

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Verify XML compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/slot_tyre.xml
git commit -m "feat: move tyre position labels to vertical right side"
```

---

### Task 3: slot_thermal.xml — vertical position label

**Files:**
- Modify: `app/src/main/res/layout/slot_thermal.xml`

**Interfaces:**
- Produces: `slotThermalPos` (id unchanged), `slotThermalMax` (id unchanged), `slotThermalZones` (id unchanged)
- Consumes by TelemetryModule.java: `findViewById(R.id.slotThermalPos)`, `findViewById(R.id.slotThermalMax)`, `findViewById(R.id.slotThermalZones)` — unchanged

- [ ] **Step 1: Replace slot_thermal.xml**

Replace the entire file content with:

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

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 2: Verify XML compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/slot_thermal.xml
git commit -m "feat: move thermal position labels to vertical right side"
```

---

### Task 4: Final verification

- [ ] **Step 1: Run lint**

```bash
./gradlew lint
```

Expected: No new errors (ignore pre-existing warnings)

- [ ] **Step 2: Run tests**

```bash
./gradlew test
```

Expected: All tests pass (42/42 in PitWindowAlertManagerTest)

- [ ] **Step 3: Run full assemble**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL
