package dev.atvremote.protocol.mrp

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayheadTest {
    private val now = 1_800_000_000.0           // a Unix time
    private val cocoa = now - 978_307_200.0     // the same instant, Apple-style

    @Test fun `a snapshot from ten minutes ago advances ten minutes while playing`() {
        val got = Mrp.extrapolateElapsed(6.0, cocoa - 600, 1.0, 1530.0, now)
        assertEquals(606.0, got, 0.001)
    }

    @Test fun `unix-based timestamps are understood too`() {
        assertEquals(106.0, Mrp.extrapolateElapsed(6.0, now - 100, 1.0, null, now), 0.001)
    }

    @Test fun `paused does not advance`() =
        assertEquals(6.0, Mrp.extrapolateElapsed(6.0, cocoa - 600, 0.0, 1530.0, now), 0.001)

    @Test fun `never runs past the end`() =
        assertEquals(100.0, Mrp.extrapolateElapsed(90.0, cocoa - 600, 1.0, 100.0, now), 0.001)

    @Test fun `half speed advances half as fast`() =
        assertEquals(36.0, Mrp.extrapolateElapsed(6.0, cocoa - 60, 0.5, null, now), 0.001)

    @Test fun `no timestamp, a future one or an ancient one leave it alone`() {
        assertEquals(6.0, Mrp.extrapolateElapsed(6.0, null, 1.0, null, now), 0.0)
        assertEquals(6.0, Mrp.extrapolateElapsed(6.0, cocoa + 30, 1.0, null, now), 0.0)
        assertEquals(6.0, Mrp.extrapolateElapsed(6.0, cocoa - 13 * 3600, 1.0, null, now), 0.0)
    }
}
