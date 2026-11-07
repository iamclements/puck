package dev.danielclements.puck

import dev.danielclements.puck.WidgetLayout.Action
import dev.danielclements.puck.WidgetLayout.Key
import dev.danielclements.puck.WidgetLayout.KeySize
import dev.danielclements.puck.WidgetLayout.Room
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    // ---- Room: the sizes the design canvas draws
    @Test fun oneByOneIsTheTile() = assertEquals(Room.TILE, WidgetLayout.room(76f, 84f).layout)
    @Test fun twoByOneIsTheCard() = assertEquals(Room.CARD, WidgetLayout.room(164f, 84f).layout)
    @Test fun threeByOneIsStrip3() = assertEquals(Room.STRIP3, WidgetLayout.room(252f, 84f).layout)
    @Test fun fourByOneIsStrip4() = assertEquals(Room.STRIP4, WidgetLayout.room(340f, 84f).layout)
    @Test fun twoByTwoIsThePanel() = assertEquals(Room.PANEL, WidgetLayout.room(164f, 180f).layout)

    @Test fun tinyCellsGetTheMicroMark() {
        assertEquals(Room.MICRO, WidgetLayout.room(40f, 40f).layout)
        assertEquals(Room.MICRO, WidgetLayout.room(90f, 60f).layout)   // short
        assertEquals(Room.MICRO, WidgetLayout.room(60f, 90f).layout)   // narrow
    }

    @Test fun breakpointsAreInclusiveAtTheLowerEdge() {
        assertEquals(Room.TILE, WidgetLayout.room(109.9f, 84f).layout)
        assertEquals(Room.CARD, WidgetLayout.room(110f, 84f).layout)
        assertEquals(Room.CARD, WidgetLayout.room(229.9f, 84f).layout)
        assertEquals(Room.STRIP3, WidgetLayout.room(230f, 84f).layout)
        assertEquals(Room.STRIP3, WidgetLayout.room(319.9f, 84f).layout)
        assertEquals(Room.STRIP4, WidgetLayout.room(320f, 84f).layout)
    }

    @Test fun shortVariantBelow76dpTall() {
        assertTrue(WidgetLayout.room(164f, 60f).short)
        assertTrue(WidgetLayout.room(340f, 75.9f).short)
        assertFalse(WidgetLayout.room(164f, 76f).short)
    }

    @Test fun panelNeedsWidthAndHeightAndGoesWideAt320() {
        assertEquals(Room.CARD, WidgetLayout.room(164f, 149f).layout)
        assertEquals(Room.PANEL, WidgetLayout.room(164f, 150f).layout)
        assertEquals(Room.PANEL, WidgetLayout.room(319f, 200f).layout)
        assertEquals(Room.PANEL_WIDE, WidgetLayout.room(320f, 200f).layout)
        // A narrow tall widget stays a tile: it has no room for the 2x2.
        assertEquals(Room.TILE, WidgetLayout.room(76f, 200f).layout)
    }

    @Test fun onlyTheStripsCarryButtons() {
        assertTrue(WidgetLayout.hasPlayPause(Room.STRIP3))
        assertTrue(WidgetLayout.hasPlayPause(Room.STRIP4))
        assertFalse(WidgetLayout.hasPlayPause(Room.CARD))
        assertTrue(WidgetLayout.hasRocker(Room.STRIP4))
        assertFalse(WidgetLayout.hasRocker(Room.STRIP3))
    }

    // ---- Rooms
    @Test fun roomsTwoAcrossWhenNarrow() {
        val g = WidgetLayout.rooms(164f, 84f, 3)
        assertEquals(2, g.cols); assertEquals(1, g.rows)
        // 3 rooms into 2 slots: the first two, and no Add key.
        assertEquals(listOf<Key>(Key.RoomKey(0), Key.RoomKey(1)), g.keys)
    }

    @Test fun roomsFourAcrossWhenWide() {
        val g = WidgetLayout.rooms(300f, 84f, 2)
        assertEquals(4, g.cols)
        assertEquals(listOf(Key.RoomKey(0), Key.RoomKey(1), Key.Add), g.keys)
    }

    @Test fun roomsAddKeyOnlyWhenThereIsASlotLeft() {
        assertTrue(WidgetLayout.rooms(300f, 84f, 3).keys.last() == Key.Add)
        assertFalse(WidgetLayout.rooms(300f, 84f, 4).keys.contains(Key.Add))
        assertEquals(listOf<Key>(Key.Add), WidgetLayout.rooms(300f, 84f, 0).keys)
    }

    @Test fun roomsTwoRowsWhenTall() {
        val g = WidgetLayout.rooms(300f, 180f, 6)
        assertEquals(2, g.rows)
        assertEquals(8, g.cols * g.rows)
        assertEquals(7, g.keys.size) // six rooms plus Add
    }

    @Test fun roomKeySizes() {
        assertEquals(KeySize.SHORT, WidgetLayout.rooms(164f, 60f, 2).keySize)
        assertEquals(KeySize.NORMAL, WidgetLayout.rooms(164f, 84f, 2).keySize)
        assertEquals(KeySize.LARGE, WidgetLayout.rooms(164f, 100f, 2).keySize)
        assertEquals(KeySize.LARGE, WidgetLayout.rooms(164f, 180f, 2).keySize) // two rows of 72
    }

    // ---- Action
    @Test fun actionGrowsToTheCardOnly() {
        assertEquals(Action.TILE, WidgetLayout.action(76f, 84f))
        assertEquals(Action.MICRO, WidgetLayout.action(40f, 40f))
        assertEquals(Action.CARD, WidgetLayout.action(164f, 84f))
        assertEquals(Action.CARD, WidgetLayout.action(400f, 84f))
    }

    // ---- The Android 12+ maps must agree with the pure functions everywhere.
    private val sweep = (30..420 step 7).flatMap { w -> (30..260 step 5).map { h -> w.toFloat() to h.toFloat() } }

    @Test fun roomBreakpointsAgreeWithRoomEverywhere() {
        for ((w, h) in sweep) {
            assertEquals("room at ${w}x$h", WidgetLayout.room(w, h),
                WidgetLayout.pickLargestFit(WidgetLayout.roomBreakpoints, w, h))
        }
    }

    @Test fun actionBreakpointsAgreeWithActionEverywhere() {
        for ((w, h) in sweep) {
            assertEquals("action at ${w}x$h", WidgetLayout.action(w, h),
                WidgetLayout.pickLargestFit(WidgetLayout.actionBreakpoints, w, h))
        }
    }

    @Test fun roomsBreakpointsAgreeOnTheGridEverywhere() {
        for ((w, h) in sweep) {
            val (kw, kh) = WidgetLayout.pickLargestFit(WidgetLayout.roomsBreakpoints, w, h)
            for (count in listOf(0, 1, 3, 9)) {
                val actual = WidgetLayout.rooms(w, h, count)
                val atKey = WidgetLayout.rooms(kw, kh, count)
                assertEquals("grid at ${w}x$h ($count)", actual, atKey)
            }
        }
    }
}
