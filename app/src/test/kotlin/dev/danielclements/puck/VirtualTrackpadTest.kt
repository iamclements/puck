package dev.danielclements.puck

import dev.atvremote.protocol.companion.TouchPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VirtualTrackpadTest {

    @Test fun rightwardStrokeStartsOnTheLeftEdge() {
        val s = VirtualTrackpad().begin(1f, 0.1f).single()
        assertEquals(TouchPhase.PRESS, s.phase)
        assertEquals(VirtualTrackpad.LOW.toInt(), s.x)
        assertEquals(500, s.y)
    }

    @Test fun leftwardAndVerticalStartOnTheMatchingEdge() {
        assertEquals(VirtualTrackpad.HIGH.toInt(), VirtualTrackpad().begin(-1f, 0f).single().x)
        val down = VirtualTrackpad().begin(0f, 1f).single()
        assertEquals(VirtualTrackpad.LOW.toInt(), down.y)
        assertEquals(500, down.x)
        assertEquals(VirtualTrackpad.HIGH.toInt(), VirtualTrackpad().begin(0f, -1f).single().y)
    }

    @Test fun aShortMoveIsOneHoldWithNoLift() {
        val pad = VirtualTrackpad().also { it.begin(1f, 0f) }
        val out = pad.move(200f, 0f)
        assertEquals(1, out.size)
        assertEquals(TouchPhase.HOLD, out[0].phase)
        assertEquals(VirtualTrackpad.LOW.toInt() + 200, out[0].x)
    }

    @Test fun aLongDragIsNotCutOffAtTheFarEdge() {
        // The old code clamped at the wall: a 3000-unit drag delivered ~500.
        val pad = VirtualTrackpad().also { it.begin(1f, 0f) }
        val all = pad.move(3000f, 0f)
        assertTrue("expected re-anchors", all.count { it.phase == TouchPhase.RELEASE } >= 2)
        assertEquals(3000f, travelledX(all, VirtualTrackpad.LOW), 3f)
    }

    @Test fun neverLeavesTheSurface() {
        val pad = VirtualTrackpad().also { it.begin(1f, 1f) }
        val all = pad.move(2500f, -1800f) + pad.move(-4000f, 900f) + pad.end()
        all.forEach {
            assertTrue("x=${it.x}", it.x in 0..1000)
            assertTrue("y=${it.y}", it.y in 0..1000)
        }
    }

    @Test fun fingerRestsBeforeEveryLiftSoTheTvSeesNoFling() {
        val pad = VirtualTrackpad().also { it.begin(1f, 0f) }
        val all = pad.move(2500f, 0f)
        all.forEachIndexed { i, s ->
            if (s.phase == TouchPhase.RELEASE) {
                val before = all[i - 1]
                assertEquals(TouchPhase.HOLD, before.phase)
                assertTrue("settle delay", before.delayMs > 0)
                assertEquals(s.x, before.x)
                assertEquals(s.y, before.y)
            }
        }
    }

    @Test fun everyLiftIsFollowedByANewPressOnTheOppositeEdge() {
        val pad = VirtualTrackpad().also { it.begin(1f, 0f) }
        val all = pad.move(2500f, 0f)
        all.forEachIndexed { i, s ->
            if (s.phase == TouchPhase.RELEASE) {
                val next = all[i + 1]
                assertEquals(TouchPhase.PRESS, next.phase)
                assertEquals(VirtualTrackpad.LOW.toInt(), next.x)
            }
        }
    }

    @Test fun leftwardDragWrapsFromTheLeftEdgeToTheRight() {
        val pad = VirtualTrackpad().also { it.begin(-1f, 0f) }
        val all = pad.move(-2500f, 0f)
        val press = all.first { it.phase == TouchPhase.PRESS }
        assertEquals(VirtualTrackpad.HIGH.toInt(), press.x)
        assertEquals(2500f, travelledX(all, VirtualTrackpad.HIGH), 3f)
    }

    @Test fun endReleasesWhereTheFingerIs() {
        val pad = VirtualTrackpad().also { it.begin(1f, 0f) }
        pad.move(100f, 0f)
        val end = pad.end().single()
        assertEquals(TouchPhase.RELEASE, end.phase)
        assertEquals(VirtualTrackpad.LOW.toInt() + 100, end.x)
    }

    /** Sum of horizontal movement inside strokes (a re-anchor jump is not finger travel). */
    private fun travelledX(samples: List<TrackpadSample>, startX: Float): Float {
        var total = 0f
        var last = startX
        for (s in samples) {
            if (s.phase == TouchPhase.PRESS) { last = s.x.toFloat(); continue }
            if (s.phase == TouchPhase.HOLD) { total += abs(s.x - last); last = s.x.toFloat() }
        }
        return total
    }
}
