# Puck widgets — engineering spec

There are three home-screen widgets (**Room**, **Rooms**, **Action button**) plus two
surfaces that aren't widgets (**Quick Settings tile**, **pinned shortcuts**). The visual
reference is the **Widgets** page of the Puck design canvas: every size, light and dark,
bright and dark wallpaper, dynamic and brand colour, the edge states, a 2× anatomy with tap
targets, and launcher stress tests.

Units: **dp** for sizes, **sp** for text. Everything below is RemoteViews-safe: Linear,
Frame and RelativeLayout, TextView, ImageView, ProgressBar, `<shape>` and vector
drawables, colour state lists with alpha. Nothing uses blur, shadows, runtime gradients,
custom fonts or animation, apart from the system's indeterminate ProgressBar.

---

## 1. Shared foundations

### 1.1 Colour tokens

All colours are resources. Brand values go in `values/` and `values-night/`. Dynamic
values go in `values-v31/` and `values-night-v31/` and point at `@android:color/system_*`.

| Resource (`@color/…`) | Light, API 31+ | Dark, API 31+ | Brand light | Brand dark | Used for |
|---|---|---|---|---|---|
| `puck_w_surface` | `system_neutral1_50` | `system_neutral1_900` | `#F0F0F7` | `#1D1E23` | Card fill (2x1 and up), state fills |
| `puck_w_on_surface` | `system_neutral1_900` | `system_neutral1_100` | `#1A1B20` | `#E3E2E6` | Names, titles |
| `puck_w_on_surface_variant` | `system_neutral2_700` | `system_neutral2_200` | `#44474E` | `#C4C6D0` | Model line, hints, dashed set-up marks |
| `puck_w_outline` | `system_neutral2_200` | `system_neutral2_700` | `#C4C6D0` | `#44474F` | Card stroke, "Add TV" dashes |
| `puck_w_tile` | `system_accent1_100` | `system_accent1_700` | `#D6E3FF` | `#284777` | 1x1 fill, mark tiles |
| `puck_w_glyph` | `system_accent1_700` | `system_accent1_100` | `#284777` | `#D6E3FF` | Remote glyph, ring, 1x1 name |
| `puck_w_hero` | `system_accent1_600` | `system_accent1_200` | `#3D5F90` | `#AAC7FF` | Play/Pause, last-used dot, sent tick, spinner |
| `puck_w_on_hero` | `system_accent1_0` | `system_accent1_800` | `#FFFFFF` | `#0A305F` | Icon on hero |
| `puck_w_button` | `system_accent2_100` | `system_accent2_700` | `#DAE2F9` | `#3A4356` | Volume, Wake |
| `puck_w_on_button` | `system_accent2_900` | `system_accent2_100` | `#131C2B` | `#DAE2F9` | Icons on button |
| `puck_w_tertiary` | `system_accent3_100` | `system_accent3_700` | `#FAD8FD` | `#563E5C` | Sleep |
| `puck_w_on_tertiary` | `system_accent3_900` | `system_accent3_100` | `#29132E` | `#F9D8FD` | Icon on tertiary |
| `puck_w_error` | `#BA1A1A` | `#FFB4AB` | `#BA1A1A` | `#FFB4AB` | Removed badge, can't-reach line |
| `puck_w_on_error` | `#FFFFFF` | `#690005` | `#FFFFFF` | `#690005` | "!" in the badge |

**Light and dark tiles are swapped on purpose.** The brief gives tile `#284777` with glyph
`#D6E3FF`. That stays as the **dark** theme. In the light theme the pair inverts (light
tile, dark glyph), which is how Android's own themed icons behave. If you want the dark
tile in both themes, swap the two light-column values for `tile` and `glyph`; nothing else
changes.

**Alpha variants.** These are colour state lists in `res/color/`, each a single `<item
android:color="…" android:alpha="…"/>`. They work on API 23 and up.

| `@color/…` | Base | Light alpha | Dark alpha (`res/color-night/`) |
|---|---|---|---|
| `puck_w_ring` | `puck_w_glyph` | 0.40 | 0.40 |
| `puck_w_card_stroke` | `puck_w_outline` | 0.80 | 0.60 |
| `puck_w_tile_stroke` | `puck_w_glyph` | 0.12 | 0.15 |
| `puck_w_key` | `#FFFFFF` (light) / `puck_w_on_surface` (dark) | 0.70 | 0.07 |
| `puck_w_divider` | `puck_w_on_button` | 0.20 | 0.20 |
| `puck_w_muted_ring` | `puck_w_on_surface_variant` | 0.35 | 0.35 |
| `puck_w_app_placeholder` | `puck_w_on_surface_variant` | 0.18 | 0.18 |

**Contrast** (checked on the brand values; dynamic tones are tone-matched and come out at
least as high):
* Body text on its fill is 7:1 or better.
* The light-theme hero icon is about 6:1.
* `error` on `surface` is 5.5:1 or better.

No text ever sits directly on the wallpaper.

### 1.2 Type

There are no custom fonts. Use `sans-serif` for regular and `sans-serif-medium` for medium
and semibold; these map to the OEM's system face. Every TextView sets `maxLines`,
`ellipsize="end"` and `includeFontPadding="false"`.

| Role | Size | Weight | Lines |
|---|---|---|---|
| 1x1 name | 11sp, line height 13sp | medium | 2, centred |
| Card / strip name | 14sp at 2x1 (2 lines), 15sp at 3x1 and 4x1 (1 line) | medium | — |
| Panel name (2x2) | 14sp | medium | 1 |
| Secondary line (model, "Open remote", status) | 11.5sp at 2x1 and 2x2, 12sp in strips | regular (status: medium) | 1 |
| Rooms key name | 11.5sp, 12sp when keys are 72dp tall or more | medium; the last-used key uses `sans-serif-medium` + bold style (600) | 1 |

### 1.3 Shape, mark and icons

* **Widget background.** The root view has `android:id="@android:id/background"`.
  * Radius is `@android:dimen/system_app_widget_background_radius` on API 31+, otherwise
    22dp.
  * Plus the 1dp stroke described per widget.
* **Inner tiles** (the 56dp mark tile and the buttons): radius
  `@android:dimen/system_app_widget_inner_radius` on 31+, otherwise the value given per
  element.
* **Mark** = ring + glyph:
  * Ring: an `oval` shape with a 1.5dp stroke in `puck_w_ring`.
  * Glyph: `ic_widget_remote.xml`, the pill with three cut-out dots (even-odd fill), tinted
    `puck_w_glyph`.
  * ImageViews are exact dp with `scaleType="centerInside"`, never `fitXY`.
* **Icons:** 24×24 viewport vectors with 2.1–2.4 stroke. Each needs a content description.
  * `ic_w_play_pause`: a filled triangle plus two bars.
  * `ic_w_minus` and `ic_w_plus`.
  * `ic_w_sleep`: crescent.
  * `ic_w_wake`: sun.
  * `ic_w_add`: plus.
  * `ic_w_check`.
  * `ic_w_alert`: circle with "!".

  Nothing from Apple, tvOS or SF Symbols.

### 1.4 Sizing model

* **Android 12+:** build `RemoteViews(Map<SizeF, RemoteViews>)` from the breakpoint tables
  below. The system shows the largest entry that fits.
* **Android 8–11:** in `onAppWidgetOptionsChanged` and `onUpdate`, read
  `OPTION_APPWIDGET_MIN_WIDTH` and `OPTION_APPWIDGET_MIN_HEIGHT`. Use the smaller pair to
  stay safe in both orientations. Pick the same breakpoint by hand.
* Breakpoints always use the **reported dp size, never cell counts**. Cells differ between
  launchers.
* Content never scales. Only fills use `match_parent`; content is centred with
  `gravity`.
* When space runs out, drop secondary lines first (set them `GONE`), then shrink the mark
  tile from 56 to 44. Buttons never shrink below a 48dp touch target.

### 1.5 Background actions (Room buttons and the Action button)

* **Tap path:** `PendingIntent.getBroadcast` → `BroadcastReceiver` → `goAsync()` →
  connect, pair-verify, send → `finish()`.
  * The budget is 10 s and the hard timeout is 5 s.
  * This needs no foreground service, so no notification.
  * Only fall back to a `shortService` foreground service (API 34+) if testing shows the
    budget isn't enough.
* **Connection reuse:** keep the Companion connection in an app-scoped holder with a 30 s
  idle close. A run of volume taps then reuses it for as long as the process is alive.
* **Feedback:** the widget can't know the TV's state, so it only reports that the command
  went out.

| State | When | What shows | Duration |
|---|---|---|---|
| Connecting | The tap hasn't resolved after **400ms** | A 12dp `ProgressBar` (indeterminate, small, `indeterminateTint` = hero) + "Connecting…" in `on_surface_variant` | Until it resolves |
| Sent | Command acknowledged | A 14dp `ic_w_check` in hero + "Sent · Play/Pause" (12sp medium, `on_surface`) | 3 s, then revert |
| Can't reach | Timeout or error | A 14dp `ic_w_alert` in error + "Can't reach this TV" in error | Until the next tap, or 10 s |

Feedback replaces the secondary line. The 1x1 has no secondary line and no buttons, so it
shows no feedback.

* **Unverified on hardware:** any command, Play/Pause included, should wake a sleeping
  Apple TV. That's why Room has no separate Wake button. If it doesn't, add Wake to the
  2x2 next to Sleep.

---

## 2. Room (refresh of the existing widget)

**Provider:**
* `targetCellWidth="1" targetCellHeight="1"`, `minWidth="40dp" minHeight="40dp"`,
  `resizeMode="horizontal|vertical"`.
* `widgetFeatures="reconfigurable|configuration_optional"` (API 31+).
* `previewLayout="@layout/widget_room_preview"` (the card with "Your TV").
* `previewImage` PNG of the same card for API 26–30.
* Description: "Open a TV's remote. Wider sizes add Play/Pause and volume."

**Config:** if exactly one TV is paired, bind it automatically and skip the picker.
Otherwise show the picker.

### 2.1 Breakpoints

Here *w* and *h* are the reported widget size.

| Layout | When | Contents |
|---|---|---|
| **micro** | h < 72 or w < 64, and w < 110 | Mark only |
| **tile** (1x1) | w < 110 | Mark and name |
| **card** (2x1) | 110 ≤ w < 230 | Tile, name and model |
| **strip3** (3x1) | 230 ≤ w < 320 | Card plus Play/Pause |
| **strip4** (4x1) | w ≥ 320 | Strip3 plus a volume rocker |
| **panel** (2x2) | w ≥ 110 **and** h ≥ 150 | Header plus a 2×2 button grid |

Any card or strip with h < 76 uses the **short** variant:
* Tile shrinks from 56 to 44 (radius 14, ring 30, glyph 18).
* Model line `GONE`.
* Padding 8 instead of 10.
* Buttons draw as 44dp discs inside clickable columns that run the full height.

A panel with w ≥ 320 puts its four buttons in one row, each at least 64 tall. It isn't
drawn on the canvas; it uses the same parts.

### 2.2 Layouts

**micro**
* Fill: `tile`, radius 18, 1dp `tile_stroke`.
* Content: mark (ring 34, glyph 20), centred.
* The name goes into `contentDescription`.

**tile (1x1), designed at 76×84**
* Fill: `tile`, with the system radius (fallback 22) and a 1dp `tile_stroke`.
* Padding: 8 top, 6 sides, 7 bottom.
* A vertical stack, centred:
  * Ring 34 (1.5 stroke, `ring`) holding glyph 20.
  * 5dp gap.
  * Name: 11sp medium, line height 13sp, `maxLines=2`, centred, colour `glyph`.

**card (2x1), 164×84**
* Fill: `surface`, with a 1dp `card_stroke`. Padding 10.
* Horizontal row:
  * Tile 56 (radius 16 or the inner radius, fill `tile`) holding ring 38 and glyph 22.
  * 10dp gap.
  * A text column:
    * Name: 14sp medium, 2 lines, line height 17.
    * Model: 11.5sp, `on_surface_variant`, 1 line, 2dp below.

**strip3 and strip4, 252×84 and 340×84**
* Same fill, stroke and padding as the card.
* **Open area:** the tile and text from the card, with the name at 15sp (1 line) and model
  at 12sp. This area takes `layout_weight=1`.
* 8dp gap, then **Play/Pause**:
  * A 52dp circle filled `hero`.
  * Icon 24, `on_hero`.
* **strip4 only:** 8dp gap, then the **volume rocker**:
  * A 96×52 pill, radius 26, filled `button`.
  * Two clickable halves, 48 wide each, with − and + icons (20dp) in `on_button`.
  * A 1dp `divider` between them, inset 14 top and bottom.

**panel (2x2), 164×180**
* Same fill, stroke and padding.
* **Header**, 48 tall:
  * Tile 44 (radius 14) holding ring 30 and glyph 18.
  * 8dp gap.
  * Name: 14sp medium, 1 line.
  * "Open remote": 11.5sp, `on_surface_variant`.
* 8dp gap, then a **2×2 grid** with 6dp gaps, radius 18, cells sharing the remaining space:
  * Play/Pause: `hero`, icon 24.
  * Sleep: `tertiary`, icon 22.
  * Vol −: `button`, icon 22.
  * Vol +: `button`, icon 22.
* Rows must stay at least 48dp tall. At the 164×180 design size they are 49.

### 2.3 Tap targets

| Target | Action | contentDescription |
|---|---|---|
| Whole widget (micro, tile, card) | Open the remote for this TV: activity `PendingIntent` with the TV id | "Open Living Room remote" |
| Open area (strips), header (panel) | Same | Same |
| Play/Pause | Background command | "Play or pause Living Room" |
| Vol − / Vol + | Background command | "Volume down" / "Volume up" (Living Room) |
| Sleep | Background command | "Put Living Room to sleep" |

### 2.4 States

**No TV chosen.** Happens with `configuration_optional`, a cancelled config, or a restore
from backup.
* Fill: `surface`.
* 1x1:
  * A ring 34 with a 1.5 **dashed** stroke (`dashWidth 3dp`, `dashGap 2.5dp`) in
    `on_surface_variant`, with `ic_w_add` (18) inside.
  * Label "Choose TV": 11sp medium, `on_surface`.
* 2x1:
  * A 56 rounded square (radius 16) with the same dashed stroke and add icon 22.
  * "Choose a TV" (14sp medium) and "Tap to set up" (11.5sp).
* Micro: the dashed ring only.
* **Tap** opens the configure activity for this widget id.

**TV removed** (the stored id isn't paired any more).
* Fill: `surface`.
* Mark: ring in `muted_ring`, glyph in `on_surface_variant`.
* Badge: 16dp, filled `error`, with "!" (11sp bold, `on_error`) and a 2dp ring in
  `surface`. It sits at the ring's top right (offset right −5, top −4).
* Name in `on_surface_variant`.
* 2x1: the tile becomes a 1.5 `outline` stroke with no fill, and the secondary line reads
  "No longer paired" (11.5sp medium, `error`).
* **Tap** opens the configure activity.
* When a TV is forgotten in the app, push an update to every widget instance.

**Connecting, Sent, Can't reach:** see §1.5.

### 2.5 Minimum cells

* micro works down to 40×40.
* tile needs 64×72.
* card needs 110×56 (short variant).
* strip3 needs 230×56.
* strip4 needs 320×56.
* panel needs 110×150.

---

## 3. Rooms

**Provider:**
* `targetCellWidth="2" targetCellHeight="1"`, `minWidth="110dp" minHeight="40dp"`,
  `resizeMode="horizontal|vertical"`.
* `widgetFeatures="reconfigurable|configuration_optional"`.
* Description: "All your TVs. Tap a room to open its remote."

**Hide it until a second TV is paired.**
* Disable the receiver with `PackageManager.setComponentEnabledSetting` while fewer than 2
  TVs are paired **and** `getAppWidgetIds()` for it is empty.
* Never disable it while instances exist, because that breaks them.
* Re-enable it when the second TV is paired.

**Config** (optional): choose which rooms appear and in what order. By default, all paired
TVs ordered by last use.

### 3.1 Breakpoints

Cols = how many keys fit across. Rows = 1 if h < 150, otherwise 2.

| Size | Keys |
|---|---|
| w < 230 | 2 across (a 2×2 when h ≥ 150) |
| w ≥ 230 | 4 across (4×2 when h ≥ 150) |

* Rooms fill the keys in order.
* If there's a free slot, the **last** slot is "Add TV".
* If there are more rooms than slots, show the first *n* in configured order. Show no
  overflow indicator; the config sets which rooms show.

### 3.2 Layout

* Fill: `surface`, with a 1dp `card_stroke` and the system radius. Padding 10.
* Keys share the width equally, with 6dp gaps both ways.
* **Key:**
  * Fill `key`, radius 18.
  * Vertical stack, centred:
    * Mark tile: 34 radius 11 with glyph 18, or 38 radius 12 with glyph 20 when the key
      is at least 72 tall. Fill `tile`, glyph `glyph`, no ring.
    * Gap 4 (small) or 6 (large).
    * Name: 11.5 or 12sp medium, 1 line, side padding 4.
* **Last-used key:**
  * Name in bold (600).
  * A 7dp `hero` dot, inset 7 top and 8 right (8 and 9 on large keys).
  * "Last used" is a timestamp saved whenever a remote opens. Refresh all Rooms instances
    when it changes.
* **Add TV key:**
  * No fill, with a 1.5 dashed `outline` stroke.
  * `ic_w_add` in `on_surface_variant`.
  * Label "Add TV" in `on_surface_variant`.
* **Short (h < 68):**
  * Card padding 6.
  * Mark tiles `GONE`, so keys show the name only.
  * Each key's clickable area is its whole column, padding included, so it stays at least
    48 tall.

### 3.3 Tap targets

* Each key opens its room's remote. contentDescription: "Open Bedroom remote".
* Add TV opens pairing. contentDescription: "Pair another TV".

### 3.4 States

* **One TV paired** (it was placed with 2+, then some were forgotten): one room key plus
  Add TV.
* **No TVs paired:** the whole widget becomes one tap target that opens pairing.
  * A dashed 48 rounded square (radius 16) with add icon 22.
  * "No TVs yet" (14sp medium) and "Tap to pair one in Puck" (11.5sp).
* **A TV is removed:** it drops out and the remaining keys reflow. No removed state is
  needed.

### 3.5 Minimum cells

* 110×56: two keys, name-only below 68 tall.
* Comfortable from 164×84.

---

## 4. Action button

**Provider:**
* `targetCellWidth="1" targetCellHeight="1"`, `minWidth="40dp" minHeight="40dp"`.
* `maxResizeWidth="230dp"`. Above 1x1 it uses the 2x1 layout, and it won't grow beyond
  that.
* `widgetFeatures="reconfigurable"`. Config is required: choose a TV, then an action.

**Actions and colour roles:**

| Action | Fill / content | Icon | Command |
|---|---|---|---|
| Play/Pause | `hero` / `on_hero` | `ic_w_play_pause` 28 | Play/pause toggle |
| Wake | `button` / `on_button` | `ic_w_wake` 28 | Wake |
| Sleep | `tertiary` / `on_tertiary` | `ic_w_sleep` 28 | Sleep |
| Launch app | `surface` / `on_surface` | The cached app icon bitmap, 32, radius 9 (placeholder fill `app_placeholder`) | Launch that app's bundle id |

* **1x1 (76×84):**
  * Fill per action, plus a 1dp stroke of the content colour at 10% (light) or 12% (dark).
  * Padding 8 top, 6 sides, 7 bottom.
  * Icon, a 6dp gap, then the room name (11sp medium, 2 lines, centred).
* **micro:** icon only, below 72 tall.
* **2x1 (164×84):**
  * Fill `surface`, `card_stroke`, padding 10.
  * A 56 disc in the action colour holding the icon (26).
  * 10dp gap.
  * Title: "Play / Pause", "Wake", "Sleep" or the app name. 14sp medium, 1 line.
  * Room: 11.5sp `on_surface_variant`, written as "on Den" for app launches.
* **Tap:** the whole widget fires the action in the background (§1.5). The 2x1 shows
  Sent and Can't reach on its second line; the 1x1 shows none.
* **States:**
  * **Not set up:** fill `surface`, a dashed ring 32 with add icon 16, and "Set up".
  * **TV removed:** fill `surface`, the icon in `on_surface_variant`, the error badge at
    the icon's top right, and the room name in `on_surface_variant`.
  * Both open the configure activity.
* **App icons:** use the bitmap already cached for the app drawer, via
  `setImageViewBitmap`. Keep it at 96px or less, to stay well inside the RemoteViews
  bitmap memory limit.

---

## 5. Not widgets

* **Quick Settings tile (`TileService`):**
  * Label "Play/Pause", subtitle = the last-used room (API 29+).
  * Tap sends Play/Pause in the background, with no unlock, using the same path as §1.5.
  * Long-press opens the remote (via the preferences activity).
  * Tile state is always `STATE_INACTIVE`, since we can't know playback state.
  * The icon is the remote glyph.
* **Pinned shortcuts:**
  * `ShortcutManager.requestPinShortcut` from the device list ("Add to home screen") and
    from the app drawer's long-press ("Add [App] on Bedroom to home screen").
  * Publish dynamic shortcuts for the top 3 rooms, so they show on the launcher icon's
    long-press.

---

## 6. Launcher behaviour checklist

| Situation | Expected behaviour |
|---|---|
| Tall cells (One UI) | Fill stretches; content keeps its size and stays centred. |
| Short cells (dense grids) | micro / short variants (§2.1, §3.2). |
| Launcher adds ~8dp padding | No outer margin of our own; the next-smaller breakpoint applies. |
| Launcher clips to a larger radius | 10dp padding keeps content clear; inner radii follow `system_app_widget_inner_radius`. |
| No label | Every widget draws its own name on its own fill. |
| Busy or matching wallpaper | Opaque fills plus a 1dp stroke keep the edge. |
| Font scale 200% (Android 14 non-linear) | Secondary lines go first, then the name wraps (2x1) or ellipsizes (strips). Tile drops to 44. Buttons stay fixed. |
| Android 8–9 | Brand light only (no system dark mode). |
| Android 10–11 | Brand light and dark via `values-night`. |
| Picker | `previewLayout` (31+) / `previewImage` (26–30) with "Your TV". |

**Test matrix.** Pixel (stock), Samsung One UI (4×5 and 5×6), Xiaomi HyperOS, OnePlus
OxygenOS, Nothing OS and Motorola, on these configurations:
* API 26, 30, 31, 34 and the latest.
* Light and dark.
* Font scale 100% and 200%.
* A bright and a dark wallpaper.
* Sizes 1x1, 2x1, 4x1 and 2x2 (plus 4x2 for Rooms).
* Each edge state.
