# Main Menu Button Design

## Problem

`MainActivity` currently displays two 64dp x 64dp buttons in the top-right corner:
- Settings gear icon
- Team Session icon

These buttons are always visible and float over the telemetry module content on the right side, obstructing the display. The user does not need access to these actions while driving; the priority is to keep the main screen clean.

## Goal

Replace the two always-visible buttons with a single, compact hamburger menu button that expands to reveal Settings and Team Session only when needed.

## Design

### Layout

In `activity_main.xml`:

1. Remove the existing `buttonSettings` and `buttonSession` `ImageButton`s.
2. Add a single 48dp hamburger `ImageButton` (`buttonMenu`) in the same top-right corner.
   - Constrain its end to the start of the live timing panel (when visible) or to the parent end.
   - Use a 12dp margin to keep it compact.
3. Add a vertical menu panel (`menuActions`) directly below `buttonMenu`, initially `visibility="gone"`.
   - The panel uses a dark, semi-transparent background consistent with the telemetry card style.
   - It contains two rows:
     - Gear icon + "Settings"
     - Session icon + "Team Session"

### Interaction

In `MainActivity.java`:

1. **Open / close:** Tapping `buttonMenu` toggles the menu's visibility with a short fade/slide animation.
2. **Action launch:** Tapping a menu row launches the corresponding activity (`SettingsActivity` or `SessionActivity`) and closes the menu.
3. **Outside tap:** Tapping anywhere outside `menuActions` closes the menu.
4. **Auto-collapse:** If the menu is left open, a `Handler` collapses it automatically after 4 seconds of no interaction.
5. **Lifecycle:** The menu closes automatically when `MainActivity` pauses.

### Edge Cases

- When the live timing panel is visible, `buttonMenu` remains in the same relative corner and `menuActions` expands downward, avoiding overlap with the timing panel.
- The menu closes before launching a new activity so it is not visible on return.

## Files to Modify

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/at/semmal/pitstopper/activities/MainActivity.java`

## Out of Scope

- Changing the behavior or content of `SettingsActivity` or `SessionActivity`.
- Adding new icons beyond the existing gear and session icons.
