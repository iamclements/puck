package dev.danielclements.puck

/**
 * Which layout each widget shows at a given size, as pure functions of the
 * launcher-reported width and height in dp. WIDGET_SPEC.md sections 2.1, 3.1
 * and 4.
 *
 * Breakpoints use the reported dp size, never cell counts: cells differ
 * between launchers.
 */
object WidgetLayout {

    // ----------------------------------------------------------------- Room

    enum class Room { MICRO, TILE, CARD, STRIP3, STRIP4, PANEL, PANEL_WIDE }

    /** A Room layout and whether it should use the compact (short) variant. */
    data class RoomSpec(val layout: Room, val short: Boolean)

    fun room(w: Float, h: Float): RoomSpec {
        // The two smallest sizes are decided by width alone, ahead of height.
        if (w < 110f) {
            return if (h < 72f || w < 64f) RoomSpec(Room.MICRO, false) else RoomSpec(Room.TILE, false)
        }
        if (h >= 150f) {
            return RoomSpec(if (w >= 320f) Room.PANEL_WIDE else Room.PANEL, false)
        }
        val layout = when {
            w < 230f -> Room.CARD
            w < 320f -> Room.STRIP3
            else -> Room.STRIP4
        }
        return RoomSpec(layout, short = h < 76f)
    }

    /** Whether a Room layout carries the Play/Pause button. */
    fun hasPlayPause(layout: Room) = layout == Room.STRIP3 || layout == Room.STRIP4

    /** Whether a Room layout carries the volume rocker. */
    fun hasRocker(layout: Room) = layout == Room.STRIP4

    // ---------------------------------------------------------------- Rooms

    enum class KeySize { SHORT, NORMAL, LARGE }

    sealed interface Key {
        data class RoomKey(val index: Int) : Key
        data object Add : Key
    }

    data class RoomsGrid(
        val cols: Int,
        val rows: Int,
        val keySize: KeySize,
        /** Keys in reading order, row by row; fewer than cols*rows when slots are left empty. */
        val keys: List<Key>,
    )

    /**
     * @param roomCount how many paired TVs there are to show, already ordered.
     */
    fun rooms(w: Float, h: Float, roomCount: Int): RoomsGrid {
        val cols = if (w < 230f) 2 else 4
        val rows = if (h >= 150f) 2 else 1
        val slots = cols * rows
        val keys = ArrayList<Key>()
        repeat(minOf(roomCount, slots)) { keys += Key.RoomKey(it) }
        // A free slot after the last room becomes "Add TV".
        if (roomCount < slots) keys += Key.Add

        // Key height: the widget less 10dp padding, less 6dp between rows.
        val keyHeight = (h - 20f - 6f * (rows - 1)) / rows
        val size = when {
            h < 68f -> KeySize.SHORT
            keyHeight >= 72f -> KeySize.LARGE
            else -> KeySize.NORMAL
        }
        return RoomsGrid(cols, rows, size, keys)
    }

    // --------------------------------------------------------------- Action

    enum class Action { MICRO, TILE, CARD }

    /** The Action button grows to the card and no further (maxResizeWidth is 230dp). */
    fun action(w: Float, h: Float): Action = when {
        w >= 110f -> Action.CARD
        h < 72f || w < 64f -> Action.MICRO
        else -> Action.TILE
    }

    // ------------------------------------------------------ Android 12+ maps

    /** One entry of a responsive RemoteViews map: used from [w] x [h] dp upward. */
    data class Breakpoint<T>(val w: Float, val h: Float, val value: T)

    /**
     * What the system does with a `Map<SizeF, RemoteViews>`: it shows the entry
     * with the largest area that still fits inside the widget. Heights of 1
     * (not 0) keep "short" entries from tying with the smallest one, and each
     * wide layout also has an entry at 72dp so that it outranks the 1x1 tile
     * (64x72) in the 72-76dp band, where width alone decides.
     */
    fun <T> pickLargestFit(entries: List<Breakpoint<T>>, w: Float, h: Float): T =
        entries.filter { it.w <= w && it.h <= h }
            .maxByOrNull { it.w * it.h }?.value ?: entries.first().value

    /** The Room breakpoints; must agree with [room] at every size. */
    val roomBreakpoints: List<Breakpoint<RoomSpec>> = listOf(
        Breakpoint(0f, 0f, RoomSpec(Room.MICRO, false)),
        Breakpoint(64f, 72f, RoomSpec(Room.TILE, false)),
        Breakpoint(110f, 1f, RoomSpec(Room.CARD, true)),
        Breakpoint(110f, 72f, RoomSpec(Room.CARD, true)),
        Breakpoint(110f, 76f, RoomSpec(Room.CARD, false)),
        Breakpoint(110f, 150f, RoomSpec(Room.PANEL, false)),
        Breakpoint(230f, 1f, RoomSpec(Room.STRIP3, true)),
        Breakpoint(230f, 72f, RoomSpec(Room.STRIP3, true)),
        Breakpoint(230f, 76f, RoomSpec(Room.STRIP3, false)),
        Breakpoint(230f, 150f, RoomSpec(Room.PANEL, false)),
        Breakpoint(320f, 1f, RoomSpec(Room.STRIP4, true)),
        Breakpoint(320f, 72f, RoomSpec(Room.STRIP4, true)),
        Breakpoint(320f, 76f, RoomSpec(Room.STRIP4, false)),
        Breakpoint(320f, 150f, RoomSpec(Room.PANEL_WIDE, false)),
    )

    /** The Action button breakpoints; must agree with [action]. */
    val actionBreakpoints: List<Breakpoint<Action>> = listOf(
        Breakpoint(0f, 0f, Action.MICRO),
        Breakpoint(64f, 72f, Action.TILE),
        Breakpoint(110f, 1f, Action.CARD),
        Breakpoint(110f, 72f, Action.CARD),
    )

    /**
     * Sizes at which [rooms] can change its grid: two or four across, one or two
     * rows, and the three key heights. The key's own size is rendered at each.
     */
    val roomsBreakpoints: List<Breakpoint<Pair<Float, Float>>> = buildList {
        // The narrow column starts at 1dp wide, not 0, so the taller entries
        // have the larger area and win.
        for (w in listOf(1f, 230f)) {
            for (h in listOf(1f, 68f, 92f, 150f, 170f)) add(Breakpoint(w, h, (maxOf(w, 110f)) to h))
        }
    }
}
