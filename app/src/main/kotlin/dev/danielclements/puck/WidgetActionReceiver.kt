package dev.danielclements.puck

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles a tap on a widget button: sends the command in the background and
 * reports progress on the widget that was tapped.
 *
 * It needs no foreground service and so no notification: a tap is a user
 * action, and goAsync() gives the receiver up to 10 seconds, which
 * [WidgetCommander] stays inside.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val command = WidgetCommand.fromId(intent.getStringExtra(EXTRA_COMMAND)) ?: return
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val app = context.applicationContext
        android.util.Log.i("atv", "widget tap: ${command.id} on widget $widgetId")
        val pending = goAsync()

        WidgetCommander.scope.launch {
            try {
                // Only worth showing if the tap has not already resolved.
                val connecting = launch {
                    delay(CONNECTING_AFTER_MS)
                    WidgetFeedback.set(widgetId, WidgetFeedback.State.Connecting)
                    WidgetRefresh.one(app, widgetId)
                }
                val result = WidgetCommander.run(app, key, command)
                connecting.cancel()
                android.util.Log.i("atv", "widget command ${command.id}: ${if (result.isSuccess) "sent" else "failed: ${result.exceptionOrNull()}"}")

                if (result.isSuccess) {
                    val sent = WidgetFeedback.State.Sent(command, System.currentTimeMillis() + WidgetFeedback.SENT_MS)
                    WidgetFeedback.set(widgetId, sent)
                    WidgetRefresh.one(app, widgetId)
                    delay(WidgetFeedback.SENT_MS)
                    // Clear only our own message: a newer tap may have replaced it.
                    if (WidgetFeedback.get(widgetId) == sent) WidgetFeedback.set(widgetId, null)
                    WidgetRefresh.one(app, widgetId)
                } else {
                    val failed = WidgetFeedback.State.Failed(System.currentTimeMillis() + WidgetFeedback.FAILED_MS)
                    WidgetFeedback.set(widgetId, failed)
                    WidgetRefresh.one(app, widgetId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_KEY = "dev.danielclements.puck.WIDGET_KEY"
        const val EXTRA_COMMAND = "dev.danielclements.puck.WIDGET_COMMAND"
        const val EXTRA_WIDGET_ID = "dev.danielclements.puck.WIDGET_ID"
        private const val CONNECTING_AFTER_MS = 400L
    }
}
