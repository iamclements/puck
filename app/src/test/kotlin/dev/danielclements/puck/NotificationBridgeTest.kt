package dev.danielclements.puck

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationBridgeTest {

    private fun snapshot(title: String? = "Show", artwork: ByteArray? = null, position: Double? = 10.0) =
        NowPlayingSnapshot(
            deviceName = "Living Room", title = title, artist = null, album = null,
            artwork = artwork, playing = true, position = position, duration = 100.0, volume = null,
        )

    @After fun reset() {
        NotificationBridge.handleCommands(null)
        NotificationBridge.publish(null)
    }

    @Test fun commandsAreDroppedWithNoHandler() {
        NotificationBridge.send(RemoteCommand.PlayPause) // must not throw
    }

    @Test fun handlerReceivesCommandsUntilReleased() {
        val seen = mutableListOf<RemoteCommand>()
        NotificationBridge.handleCommands { seen += it }
        NotificationBridge.send(RemoteCommand.Skip(10.0))
        NotificationBridge.handleCommands(null)
        NotificationBridge.send(RemoteCommand.PlayPause)
        assertEquals(listOf<RemoteCommand>(RemoteCommand.Skip(10.0)), seen)
    }

    @Test fun nullSnapshotIsTheStopSignal() {
        NotificationBridge.publish(snapshot())
        assertEquals("Show", NotificationBridge.snapshot.value?.title)
        NotificationBridge.publish(null)
        assertNull(NotificationBridge.snapshot.value)
    }

    @Test fun snapshotsCompareArtworkBySizeOnly() {
        val a = snapshot(artwork = ByteArray(4) { 1 })
        val b = snapshot(artwork = ByteArray(4) { 2 })
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a != snapshot(artwork = ByteArray(5)))
    }

    @Test fun movedPlayheadIsADifferentSnapshot() {
        assertTrue(snapshot(position = 10.0) != snapshot(position = 11.0))
    }
}
