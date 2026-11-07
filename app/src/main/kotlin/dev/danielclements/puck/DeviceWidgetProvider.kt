package dev.danielclements.puck

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews

/**
 * A home-screen widget (1x1, resizable) pinned to one specific, already-paired Apple TV.
 *
 * Unlike the long-press launcher shortcuts (which always track the two most
 * recently used devices), each widget instance is bound once, at add time,
 * to whichever device the user picked in [DeviceWidgetConfigActivity] — it
 * stays pointed at that TV regardless of what else gets connected to later.
 */
class DeviceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val store = CredentialStore(context)
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, store, id) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetBindings.unbind(context, it) }
    }

    companion object {
        /** Width from which the wide card layout is used. */
        private const val WIDE_DP = 110f

        /** Height (with a wide width) at which a 2x2 goes back to the big icon. */
        private const val SQUARE_DP = 110f

        /**
         * Re-renders every placed widget instance.
         *
         * `updatePeriodMillis="0"` means Android never refreshes these on
         * its own, and a widget's bound device can change state at any
         * time — renamed on a later connect, or forgotten entirely — with
         * nothing about that flowing through the normal widget lifecycle.
         * Call this wherever [CredentialStore] data changes, or a forgotten
         * or renamed device leaves its widget showing stale information
         * indefinitely.
         */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DeviceWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val store = CredentialStore(context)
            ids.forEach { id -> updateWidget(context, manager, store, id) }
        }

        fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            store: CredentialStore,
            appWidgetId: Int,
        ) {
            val credentialKey = WidgetBindings.credentialKey(context, appWidgetId)
            val device = credentialKey?.let { store.loadDevice(it) }

            val label = device?.name ?: context.getString(R.string.widget_unconfigured)

            // A binding that no longer resolves to a device — forgotten
            // since this widget was configured — sends the tap back to the
            // config picker instead of into the app with nothing to show,
            // so there's a way to rebind it short of deleting and re-adding
            // the widget.
            val pendingIntent = if (device != null) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_DEVICE_KEY, device.credentialKey)
                }
                PendingIntent.getActivity(
                    context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            } else {
                val intent = Intent(context, DeviceWidgetConfigActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                PendingIntent.getActivity(
                    context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

            fun build(layout: Int) = RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.widget_label, label)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            // From Android 12 the widget can be resized and the launcher picks
            // the layout that fits: a square icon-and-pill when small or tall,
            // an icon-beside-text card when stretched wider. Earlier versions
            // get the compact layout, which scales to any cell.
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(
                    mapOf(
                        SizeF(0f, 0f) to build(R.layout.widget_device),
                        SizeF(WIDE_DP, 0f) to build(R.layout.widget_device_wide),
                        SizeF(WIDE_DP, SQUARE_DP) to build(R.layout.widget_device),
                    )
                )
            } else {
                build(R.layout.widget_device)
            }
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
