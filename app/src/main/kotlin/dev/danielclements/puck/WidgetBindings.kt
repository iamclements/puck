package dev.danielclements.puck

import android.content.Context

/**
 * Which Apple TV each home-screen widget instance is pinned to.
 *
 * Separate from [CredentialStore]: a widget's binding is keyed by its
 * `appWidgetId`, which Android assigns per placed instance, not by device —
 * the same TV can be bound to more than one widget, and a widget survives a
 * device being re-paired under the same credential key.
 */
object WidgetBindings {

    private const val PREFS = "widget_bindings"

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bind(context: Context, appWidgetId: Int, credentialKey: String) {
        prefs(context).edit().putString("widget-$appWidgetId", credentialKey).apply()
    }

    fun credentialKey(context: Context, appWidgetId: Int): String? =
        prefs(context).getString("widget-$appWidgetId", null)

    /** An Action button widget also remembers which command it sends. */
    fun bindAction(context: Context, appWidgetId: Int, credentialKey: String, command: WidgetCommand) {
        prefs(context).edit()
            .putString("widget-$appWidgetId", credentialKey)
            .putString("widget-action-$appWidgetId", command.id)
            .apply()
    }

    fun command(context: Context, appWidgetId: Int): WidgetCommand? =
        WidgetCommand.fromId(prefs(context).getString("widget-action-$appWidgetId", null))

    /**
     * The name this widget last showed. Forgetting a TV deletes its stored
     * details, but a widget bound to it should still say which TV it was.
     */
    fun rememberName(context: Context, appWidgetId: Int, name: String) {
        val p = prefs(context)
        if (p.getString("widget-name-$appWidgetId", null) != name) {
            p.edit().putString("widget-name-$appWidgetId", name).apply()
        }
    }

    fun lastName(context: Context, appWidgetId: Int): String? =
        prefs(context).getString("widget-name-$appWidgetId", null)

    fun unbind(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove("widget-$appWidgetId")
            .remove("widget-action-$appWidgetId")
            .remove("widget-name-$appWidgetId")
            .apply()
    }
}
