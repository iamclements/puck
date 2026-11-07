package dev.danielclements.puck

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLogicTest {

    @After fun clear() { (1..3).forEach { WidgetFeedback.set(it, null) } }

    // ---- feedback
    @Test fun sentExpiresAfterItsTime() {
        val sent = WidgetFeedback.State.Sent(WidgetCommand.PLAY_PAUSE, until = 1_000L)
        WidgetFeedback.set(1, sent)
        assertEquals(sent, WidgetFeedback.get(1, now = 999L))
        assertNull(WidgetFeedback.get(1, now = 1_000L))
        assertNull(WidgetFeedback.get(1, now = 0L)) // already dropped
    }

    @Test fun connectingNeverExpiresOnItsOwn() {
        WidgetFeedback.set(2, WidgetFeedback.State.Connecting)
        assertEquals(WidgetFeedback.State.Connecting, WidgetFeedback.get(2, now = Long.MAX_VALUE - 1))
    }

    @Test fun eachWidgetHasItsOwnStatus() {
        WidgetFeedback.set(1, WidgetFeedback.State.Connecting)
        assertNull(WidgetFeedback.get(2))
    }

    // ---- commands
    @Test fun commandsRoundTripThroughTheirIds() {
        WidgetCommand.entries.forEach { assertEquals(it, WidgetCommand.fromId(it.id)) }
        assertNull(WidgetCommand.fromId("nope"))
        assertNull(WidgetCommand.fromId(null))
    }

    @Test fun commandIdsAreDistinct() =
        assertEquals(WidgetCommand.entries.size, WidgetCommand.entries.map { it.id }.toSet().size)

    // ---- models
    @Test fun friendlyModelNames() {
        assertEquals("Apple TV 4K", AppleTvModels.friendly("AppleTV6,2"))
        assertEquals("Apple TV HD", AppleTvModels.friendly("AppleTV5,3"))
        assertEquals("Apple TV", AppleTvModels.friendly("AppleTV99,1")) // unknown future model
        assertEquals("Apple TV", AppleTvModels.friendly(null))
    }

    // ---- Rooms availability
    @Test fun roomsHiddenUntilASecondTv() {
        assertFalse(WidgetAvailability.roomsOffered(pairedTvs = 0, placedInstances = 0))
        assertFalse(WidgetAvailability.roomsOffered(pairedTvs = 1, placedInstances = 0))
        assertTrue(WidgetAvailability.roomsOffered(pairedTvs = 2, placedInstances = 0))
    }

    @Test fun roomsNeverDisabledWhileAnInstanceExists() {
        // Disabling the receiver would break the placed widget.
        assertTrue(WidgetAvailability.roomsOffered(pairedTvs = 1, placedInstances = 1))
        assertTrue(WidgetAvailability.roomsOffered(pairedTvs = 0, placedInstances = 2))
    }
}
