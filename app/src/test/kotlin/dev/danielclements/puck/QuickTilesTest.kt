package dev.danielclements.puck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickTilesTest {
    @Test fun noSavedTvsStillOffersOneTile() = assertEquals(1, QuickTiles.visibleCount(0))
    @Test fun oneTvOffersOneTile() = assertEquals(1, QuickTiles.visibleCount(1))
    @Test fun threeTvsOfferThreeTiles() = assertEquals(3, QuickTiles.visibleCount(3))
    @Test fun neverMoreTilesThanSlots() = assertEquals(QuickTiles.SLOTS, QuickTiles.visibleCount(50))

    @Test fun slotsAreFoundByClassName() {
        assertEquals(0, QuickTiles.slotOf(DeviceTileOne::class.java.name))
        assertEquals(2, QuickTiles.slotOf(DeviceTileThree::class.java.name))
        assertNull(QuickTiles.slotOf("some.other.Service"))
        assertNull(QuickTiles.slotOf(null))
    }

    @Test fun everySlotIsDistinct() =
        assertEquals(QuickTiles.SLOTS, QuickTiles.services.map { it.name }.toSet().size)
}
