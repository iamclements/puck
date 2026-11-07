package dev.danielclements.puck

import java.util.concurrent.ConcurrentHashMap

/**
 * What a widget button's last tap did, shown in place of the widget's
 * secondary line. A widget cannot know the TV's state, so this only reports
 * that the command went out. WIDGET_SPEC.md section 1.5.
 *
 * Process-wide and in memory: a status that outlives the process is wrong by
 * definition.
 */
object WidgetFeedback {

    sealed interface State {
        /** The tap has not resolved after a moment. */
        data object Connecting : State

        data class Sent(val command: WidgetCommand, val until: Long) : State

        data class Failed(val until: Long) : State
    }

    const val SENT_MS = 3_000L
    const val FAILED_MS = 10_000L

    private val states = ConcurrentHashMap<Int, State>()

    fun set(widgetId: Int, state: State?) {
        if (state == null) states.remove(widgetId) else states[widgetId] = state
    }

    /** The current state, with anything past its time dropped. */
    fun get(widgetId: Int, now: Long = System.currentTimeMillis()): State? {
        val state = states[widgetId] ?: return null
        val until = when (state) {
            is State.Sent -> state.until
            is State.Failed -> state.until
            State.Connecting -> Long.MAX_VALUE
        }
        if (now >= until) {
            states.remove(widgetId, state)
            return null
        }
        return state
    }
}
