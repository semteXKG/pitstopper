# Main Menu Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the two always-visible top-right buttons (Settings, Team Session) with a single hamburger menu that expands to reveal both actions.

**Architecture:** A single `ImageButton` opens a small vertical `LinearLayout` menu panel. An invisible full-screen overlay sits behind the menu to catch outside taps. A `Handler` auto-collapses the menu after 4 seconds of inactivity, and the menu is also collapsed in `onPause()`.

**Tech Stack:** Android Java 11, ConstraintLayout, Vector drawables, Handler.

## Global Constraints

- Keep the main screen clean; instant access while driving is not required.
- Menu expands downward from the hamburger button.
- Tapping outside the menu closes it.
- Menu auto-collapses after 4 seconds of no interaction.
- Use existing app colors (`text_primary`, `background_primary`) and existing icons (`ic_settings`, `ic_session`).
- No changes to `SettingsActivity` or `SessionActivity` behavior.

---

### Task 1: Add menu icon and background drawables

**Files:**
- Create: `app/src/main/res/drawable/ic_menu.xml`
- Create: `app/src/main/res/drawable/menu_actions_bg.xml`

**Interfaces:**
- Produces: `R.drawable.ic_menu` — white hamburger icon used by the menu button.
- Produces: `R.drawable.menu_actions_bg` — dark rounded background for the menu panel.

- [ ] **Step 1: Create the hamburger icon**

Create `app/src/main/res/drawable/ic_menu.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M3,6h18v2H3V6zm0,5h18v2H3v-2zm0,5h18v2H3v-2z"/>
</vector>
```

- [ ] **Step 2: Create the menu background**

Create `app/src/main/res/drawable/menu_actions_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E61C1C1C" />
    <corners android:radius="8dp" />
</shape>
```

- [ ] **Step 3: Verify drawables compile**

Run:

```bash
./gradlew :app:mergeDebugResources
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/ic_menu.xml app/src/main/res/drawable/menu_actions_bg.xml
git commit -m "feat: add hamburger menu icon and action menu background"
```

---

### Task 2: Add menu strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `R.string.menu` — content description for the hamburger button.
- Produces: `R.string.team_session` — label for the team session menu row.

- [ ] **Step 1: Add the strings**

Open `app/src/main/res/values/strings.xml` and add these two entries next to the existing `<string name="settings">Settings</string>`:

```xml
    <string name="menu">Menu</string>
    <string name="team_session">Team Session</string>
```

- [ ] **Step 2: Verify resources compile**

Run:

```bash
./gradlew :app:mergeDebugResources
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add menu and team session strings"
```

---

### Task 3: Update main activity layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

**Interfaces:**
- Produces: `R.id.buttonMenu` — hamburger button.
- Produces: `R.id.menuActions` — expandable menu panel.
- Produces: `R.id.menuOverlay` — full-screen transparent overlay for outside-tap dismissal.
- Produces: `R.id.menuItemSettings` — settings row inside the menu.
- Produces: `R.id.menuItemSession` — team session row inside the menu.

- [ ] **Step 1: Replace the two existing buttons**

In `app/src/main/res/layout/activity_main.xml`, remove the entire `buttonSettings` and `buttonSession` block (lines 121-149) and replace it with:

```xml
    <!-- Invisible overlay to catch taps outside the open menu -->
    <View
        android:id="@+id/menuOverlay"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@android:color/transparent"
        android:clickable="true"
        android:focusable="true"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Main menu button — opens a small action menu -->
    <ImageButton
        android:id="@+id/buttonMenu"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_margin="12dp"
        android:padding="8dp"
        android:scaleType="fitCenter"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/menu"
        android:src="@drawable/ic_menu"
        app:tint="@color/text_primary"
        app:layout_constraintEnd_toStartOf="@+id/liveTimingPanel"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Action menu — expands below the main menu button -->
    <LinearLayout
        android:id="@+id/menuActions"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginEnd="12dp"
        android:layout_marginTop="4dp"
        android:background="@drawable/menu_actions_bg"
        android:orientation="vertical"
        android:padding="8dp"
        android:visibility="gone"
        app:layout_constraintEnd_toStartOf="@+id/liveTimingPanel"
        app:layout_constraintTop_toBottomOf="@id/buttonMenu">

        <LinearLayout
            android:id="@+id/menuItemSettings"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="?attr/selectableItemBackground"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:padding="8dp">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@null"
                android:src="@drawable/ic_settings"
                app:tint="@color/text_primary" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:text="@string/settings"
                android:textColor="@color/text_primary"
                android:textSize="16sp" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/menuItemSession"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:background="?attr/selectableItemBackground"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:padding="8dp">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@null"
                android:src="@drawable/ic_session"
                app:tint="@color/text_primary" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:text="@string/team_session"
                android:textColor="@color/text_primary"
                android:textSize="16sp" />
        </LinearLayout>
    </LinearLayout>
```

Make sure this block replaces the old `buttonSettings`/`buttonSession` block and is placed **after** `centerModuleContainer`. The order within the block matters: `menuOverlay` first (covers the screen), then `buttonMenu`, then `menuActions` (on top of everything) so taps reach the menu rows and the hamburger button.

- [ ] **Step 2: Verify layout compiles**

Run:

```bash
./gradlew :app:mergeDebugResources
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "feat: replace settings/session buttons with hamburger menu layout"
```

---

### Task 4: Wire up menu behavior in MainActivity

**Files:**
- Modify: `app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java`

**Interfaces:**
- Consumes: `R.id.buttonMenu`, `R.id.menuActions`, `R.id.menuOverlay`, `R.id.menuItemSettings`, `R.id.menuItemSession` from the updated layout.
- Produces: `collapseMenu()` private helper that closes the menu and cancels the pending auto-collapse.

- [ ] **Step 1: Replace the buttonSettings field with menu fields**

In the field declarations section of `MainActivity.java`, remove:

```java
    private ImageButton buttonSettings;
```

And add in its place:

```java
    private ImageButton buttonMenu;
    private View menuActions;
    private View menuOverlay;
    private View menuItemSettings;
    private View menuItemSession;
    private Handler menuCollapseHandler;
    private Runnable menuCollapseRunnable;
```

- [ ] **Step 2: Bind the new views and remove the old binding**

In `onCreate`, replace:

```java
        buttonSettings = findViewById(R.id.buttonSettings);
```

with:

```java
        buttonMenu = findViewById(R.id.buttonMenu);
        menuActions = findViewById(R.id.menuActions);
        menuOverlay = findViewById(R.id.menuOverlay);
        menuItemSettings = findViewById(R.id.menuItemSettings);
        menuItemSession = findViewById(R.id.menuItemSession);
        menuCollapseHandler = new Handler(Looper.getMainLooper());
```

- [ ] **Step 3: Replace the old click listeners with menu logic**

Replace this block:

```java
        // Set up settings button click listener
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        ImageButton buttonSession = findViewById(R.id.buttonSession);
        buttonSession.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SessionActivity.class)));
```

with:

```java
        menuCollapseRunnable = this::collapseMenu;

        // Set up main menu button click listener
        buttonMenu.setOnClickListener(v -> {
            if (menuActions.getVisibility() == View.VISIBLE) {
                collapseMenu();
            } else {
                menuActions.setVisibility(View.VISIBLE);
                menuOverlay.setVisibility(View.VISIBLE);
                menuCollapseHandler.postDelayed(menuCollapseRunnable, 4000);
            }
        });

        menuOverlay.setOnClickListener(v -> collapseMenu());

        menuItemSettings.setOnClickListener(v -> {
            collapseMenu();
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        menuItemSession.setOnClickListener(v -> {
            collapseMenu();
            startActivity(new Intent(MainActivity.this, SessionActivity.class));
        });
```

- [ ] **Step 4: Add the collapseMenu helper**

Add a new private method in `MainActivity.java`:

```java
    private void collapseMenu() {
        if (menuActions != null) {
            menuActions.setVisibility(View.GONE);
        }
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
        if (menuCollapseHandler != null && menuCollapseRunnable != null) {
            menuCollapseHandler.removeCallbacks(menuCollapseRunnable);
        }
    }
```

Place it near the other private helper methods (for example, just after `updateTime()` or before `onPause()`).

- [ ] **Step 5: Collapse menu on activity pause**

In `onPause()`, add as the first statement after `super.onPause()`:

```java
        collapseMenu();
```

- [ ] **Step 6: Build and run unit tests**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

Run existing unit tests:

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java
git commit -m "feat: wire up hamburger menu open/close and auto-collapse"
```

---

### Task 5: Manual verification

**Files:**
- No file changes.

- [ ] **Step 1: Install debug APK**

Run:

```bash
./gradlew :app:installDebug
```

Expected: INSTALL SUCCESS.

- [ ] **Step 2: Verify UI behavior**

Launch the app and confirm:

1. Only the hamburger icon is visible in the top-right corner.
2. Tapping the hamburger icon opens the menu showing "Settings" and "Team Session".
3. Tapping "Settings" opens `SettingsActivity`; returning shows the menu closed.
4. Tapping "Team Session" opens `SessionActivity`; returning shows the menu closed.
5. Tapping the hamburger icon again closes the menu.
6. Tapping anywhere outside the menu while it is open closes it.
7. Leaving the menu open for 4 seconds collapses it automatically.

- [ ] **Step 3: Commit verification notes (optional)**

If any behavior is incorrect, fix in a follow-up commit. If all pass, no additional commit needed.

---

## Self-Review

**Spec coverage:**
- Single compact hamburger button: Task 3 layout, Task 4 wiring.
- Menu expands downward: Task 3 layout constraints.
- Tapping outside closes menu: Task 3 `menuOverlay`, Task 4 overlay click listener.
- Auto-collapse after 4 seconds: Task 4 `menuCollapseHandler` / `menuCollapseRunnable`.
- Live timing panel compatibility: Task 3 constraints keep menu end aligned to start of `liveTimingPanel`.
- Close on activity pause: Task 4 `onPause()` call.

**Placeholder scan:**
- No TBD, TODO, or vague steps.
- All code blocks contain complete code.
- All file paths are exact.

**Type consistency:**
- View IDs in layout match `findViewById` calls in Java.
- `collapseMenu()` is used consistently in listeners and `onPause()`.
