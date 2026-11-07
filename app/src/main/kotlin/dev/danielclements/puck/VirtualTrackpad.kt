package dev.danielclements.puck

import dev.atvremote.protocol.companion.TouchPhase
import kotlin.math.abs

/**
 * One sample to send to the Apple TV's virtual touch surface.
 *
 * [delayMs] is a pause *before* sending it, used to make the Apple TV see the
 * finger stop before a re-anchor lift.
 */
data class TrackpadSample(val x: Int, val y: Int, val phase: TouchPhase, val delayMs: Long = 0)

/**
 * Turns an unbounded finger drag into touches on the Apple TV's bounded
 * 0..1000 virtual surface.
 *
 * The surface is the size of a Siri Remote clickpad and the TV reads motion as
 * the *change* in absolute position. A phone drag is far longer than that, and
 * the old code clamped the position to the surface, starting from its centre:
 * after about half a surface of travel the virtual finger was against the wall
 * and everything past that was thrown away, so a full-width swipe moved focus
 * one item.
 *
 * This keeps the motion going instead:
 *  - the stroke starts on the far side of the surface from the direction of
 *    travel, so nearly the whole surface is usable (not half);
 *  - when the virtual finger reaches the far edge it is held still for an
 *    instant (so the TV sees zero velocity and does not fling), lifted, and
 *    put back down on the near edge, and the drag carries on from there.
 *
 * Pure Kotlin with no Android types, so it can be unit tested.
 */
class VirtualTrackpad {

    private var x = CENTRE
    private var y = CENTRE

    /** Start a stroke for a drag first heading in direction ([dirX], [dirY]). */
    fun begin(dirX: Float, dirY: Float): List<TrackpadSample> {
        if (abs(dirX) >= abs(dirY)) {
            x = if (dirX >= 0) LOW else HIGH
            y = CENTRE
        } else {
            x = CENTRE
            y = if (dirY >= 0) LOW else HIGH
        }
        return listOf(sample(TouchPhase.PRESS))
    }

    /** Move the virtual finger by ([dx], [dy]) surface units, re-anchoring at the edges. */
    fun move(dx: Float, dy: Float): List<TrackpadSample> {
        val out = ArrayList<TrackpadSample>()
        var remX = dx
        var remY = dy
        var guard = 0
        while ((remX != 0f || remY != 0f) && guard++ < MAX_WRAPS) {
            val nx = x + remX
            val ny = y + remY
            if (nx in LOW..HIGH && ny in LOW..HIGH) {
                x = nx
                y = ny
                out += sample(TouchPhase.HOLD)
                return out
            }

            // Run to whichever edge is reached first, then wrap on that axis.
            val tx = fractionToEdge(x, remX)
            val ty = fractionToEdge(y, remY)
            val t = minOf(tx, ty)
            x += remX * t
            y += remY * t
            out += sample(TouchPhase.HOLD)

            val wrapX = tx <= ty
            remX *= (1f - t)
            remY *= (1f - t)

            // Hold still, lift, and put the finger back on the opposite edge.
            out += TrackpadSample(x.toInt(), y.toInt(), TouchPhase.HOLD, SETTLE_MS)
            out += sample(TouchPhase.RELEASE)
            if (wrapX) x = if (x >= HIGH - 1f) LOW else HIGH
            else y = if (y >= HIGH - 1f) LOW else HIGH
            out += TrackpadSample(x.toInt(), y.toInt(), TouchPhase.PRESS, REPLACE_MS)
        }
        return out
    }

    /** Lift the finger where it is. */
    fun end(): List<TrackpadSample> = listOf(sample(TouchPhase.RELEASE))

    private fun sample(phase: TouchPhase) = TrackpadSample(x.toInt(), y.toInt(), phase)

    /** Fraction (0..1) of [delta] after which [pos] reaches the edge it is heading for. */
    private fun fractionToEdge(pos: Float, delta: Float): Float = when {
        delta > 0f && pos + delta > HIGH -> (HIGH - pos) / delta
        delta < 0f && pos + delta < LOW -> (LOW - pos) / delta
        else -> Float.MAX_VALUE
    }

    companion object {
        const val CENTRE = 500f

        /** Usable surface, with a margin so a stroke never starts on the very edge. */
        const val LOW = 60f
        const val HIGH = 940f

        /** How long the finger rests at an edge before lifting, so velocity reads as zero. */
        const val SETTLE_MS = 24L

        /** Gap between lifting and touching down again. */
        const val REPLACE_MS = 16L

        private const val MAX_WRAPS = 64
    }
}
