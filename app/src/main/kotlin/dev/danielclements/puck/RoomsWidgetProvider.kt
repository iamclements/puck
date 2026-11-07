package dev.danielclements.puck

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import dev.danielclements.puck.WidgetLayout.Key
import dev.danielclements.puck.WidgetLayout.KeySize
import dev.danielclements.puck.WidgetRender.show

/**
 * Rooms: every paired TV as a key, the most recently used first. Tap a key to
 * open that TV's remote. WIDGET_SPEC.md section 3.
 */
class RoomsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val store = CredentialStore(context)
        appWidgetIds.forEach { update(context, appWidgetManager, store, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, CredentialStore(context), appWidgetId)
    }

    companion object {

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val store = CredentialStore(context)
            WidgetRefresh.ids(context, RoomsWidgetProvider::class.java)
                .forEach { update(context, manager, store, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, store: CredentialStore, id: Int) {
            val rooms = store.pairedDevicesByRecency()
            val lastUsed = rooms.maxByOrNull { store.lastUsedAt(it.credentialKey) }
                ?.takeIf { store.lastUsedAt(it.credentialKey) > 0L }

            val views = if (rooms.isEmpty()) {
                // Nothing paired: the whole widget is one way into pairing.
                RemoteViews(context.packageName, R.layout.widget_rooms_empty).apply {
                    setOnClickPendingIntent(WidgetRender.ROOT, WidgetRender.openPairing(context, id))
                    setContentDescription(WidgetRender.ROOT, context.getString(R.string.widget_cd_pair))
                }
            } else {
                WidgetRender.build(
                    manager, id,
                    breakpoints = WidgetLayout.roomsBreakpoints,
                    pickBySize = { w, h -> w to h },
                    render = { (w, h) -> render(context, id, WidgetLayout.rooms(w, h, rooms.size), rooms, lastUsed) },
                )
            }
            manager.updateAppWidget(id, views)
        }

        private fun render(
            context: Context,
            id: Int,
            grid: WidgetLayout.RoomsGrid,
            rooms: List<dev.atvremote.protocol.discovery.AppleTvDevice>,
            lastUsed: dev.atvremote.protocol.discovery.AppleTvDevice?,
        ): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.widget_rooms)
            rv.show(R.id.w_row2, grid.rows == 2)
            // An update is applied onto the existing views, so the old keys must
            // go or every refresh would append another set.
            rv.removeAllViews(R.id.w_row1)
            rv.removeAllViews(R.id.w_row2)

            val keyLayout = when (grid.keySize) {
                KeySize.SHORT -> R.layout.widget_rooms_key_short
                KeySize.NORMAL -> R.layout.widget_rooms_key
                KeySize.LARGE -> R.layout.widget_rooms_key_large
            }
            grid.keys.forEachIndexed { slot, key ->
                val row = if (slot < grid.cols) R.id.w_row1 else R.id.w_row2
                val keyViews = when (key) {
                    is Key.RoomKey -> {
                        val device = rooms[key.index]
                        RemoteViews(context.packageName, keyLayout).apply {
                            // The last-used room is bold and gets a dot. RemoteViews cannot
                            // change a typeface, so it is a second, bold TextView.
                            val last = device.credentialKey == lastUsed?.credentialKey
                            setTextViewText(R.id.w_name, device.name)
                            setTextViewText(R.id.w_name_last, device.name)
                            show(R.id.w_name, !last)
                            show(R.id.w_name_last, last)
                            show(R.id.w_last_dot, last)
                            setOnClickPendingIntent(
                                R.id.w_key,
                                WidgetRender.openRemote(context, id * 16 + slot + 1, device.credentialKey),
                            )
                            setContentDescription(R.id.w_key, context.getString(R.string.widget_cd_open, device.name))
                        }
                    }
                    Key.Add -> RemoteViews(context.packageName, R.layout.widget_rooms_key_add).apply {
                        setOnClickPendingIntent(R.id.w_key, WidgetRender.openPairing(context, id * 16 + slot + 1))
                        setContentDescription(R.id.w_key, context.getString(R.string.widget_cd_pair))
                    }
                }
                rv.addView(row, keyViews)
            }
            return rv
        }
    }
}
