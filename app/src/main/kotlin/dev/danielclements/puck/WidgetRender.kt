package dev.danielclements.puck

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews

/** Pieces shared by the three widgets' renderers. */
object WidgetRender {

    /** Id of the root view the launcher clips to its own corner radius. */
    val ROOT = android.R.id.background

    /** The widget's current size in dp, from the options the launcher reports. */
    fun sizeOf(manager: AppWidgetManager, id: Int): Pair<Float, Float> {
        val o = manager.getAppWidgetOptions(id)
        // The smaller of the two orientations, so the layout is safe in both.
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        return (if (w > 0) w.toFloat() else 76f) to (if (h > 0) h.toFloat() else 84f)
    }

    /**
     * From Android 12 hand the launcher every layout and let it pick by size;
     * before that, pick by the size it last reported.
     */
    fun <T> build(
        manager: AppWidgetManager,
        id: Int,
        breakpoints: List<WidgetLayout.Breakpoint<T>>,
        pickBySize: (Float, Float) -> T,
        render: (T) -> RemoteViews,
    ): RemoteViews =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RemoteViews(breakpoints.associate { SizeF(it.w, it.h) to render(it.value) })
        } else {
            val (w, h) = sizeOf(manager, id)
            render(pickBySize(w, h))
        }

    // --------------------------------------------------------------- intents

    fun openRemote(context: Context, requestCode: Int, key: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DEVICE_KEY, key)
        }
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }

    /** Opens the app on its device list, where a TV is paired. */
    fun openPairing(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }

    fun configure(context: Context, widgetId: Int, activity: Class<*>): PendingIntent {
        val intent = Intent(context, activity).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getActivity(context, widgetId, intent, FLAGS)
    }

    fun command(context: Context, widgetId: Int, key: String, command: WidgetCommand): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            putExtra(WidgetActionReceiver.EXTRA_KEY, key)
            putExtra(WidgetActionReceiver.EXTRA_COMMAND, command.id)
            putExtra(WidgetActionReceiver.EXTRA_WIDGET_ID, widgetId)
        }
        // One request code per (widget, command), so their extras never collide.
        return PendingIntent.getBroadcast(context, widgetId * 16 + command.ordinal + 1, intent, FLAGS)
    }

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    // ----------------------------------------------------------- view helpers

    fun RemoteViews.background(id: Int, drawable: Int) = setInt(id, "setBackgroundResource", drawable)

    /**
     * Switch a view to its alternate colour.
     *
     * The widget layouts colour text and icons with selectors
     * (res/color/puck_w_c_*) that have a normal and a disabled colour, so the
     * colour is resolved when the widget is inflated and follows light and dark
     * and the wallpaper. A colour number set from here would be baked in at
     * render time and be wrong after either changed, because nothing re-renders
     * a widget then. `setEnabled` is one of the few methods RemoteViews allows.
     */
    fun RemoteViews.alt(id: Int, on: Boolean = true) = setBoolean(id, "setEnabled", !on)

    fun RemoteViews.show(id: Int, visible: Boolean) =
        setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)

    /**
     * Put the last tap's outcome in the secondary line, falling back to
     * [normal]. A layout without these views ignores the calls, which is how
     * the compact layouts (no secondary line) opt out.
     *
     * "Sent" has its own line (`w_secondary_sent`, on-surface) because the
     * normal one only has a muted and an error colour.
     */
    fun RemoteViews.applyStatus(context: Context, widgetId: Int, normal: String) {
        val state = WidgetFeedback.get(widgetId)
        val sent = state is WidgetFeedback.State.Sent
        show(R.id.w_secondary, !sent)
        show(R.id.w_secondary_sent, sent)
        alt(R.id.w_status_icon, false)
        alt(R.id.w_secondary, false)
        when (state) {
            null -> {
                show(R.id.w_progress, false); show(R.id.w_status_icon, false)
                setTextViewText(R.id.w_secondary, normal)
            }
            WidgetFeedback.State.Connecting -> {
                show(R.id.w_progress, true); show(R.id.w_status_icon, false)
                setTextViewText(R.id.w_secondary, context.getString(R.string.widget_connecting))
            }
            is WidgetFeedback.State.Sent -> {
                show(R.id.w_progress, false); show(R.id.w_status_icon, true)
                setImageViewResource(R.id.w_status_icon, R.drawable.ic_w_check)
                setTextViewText(
                    R.id.w_secondary_sent,
                    context.getString(R.string.widget_sent, context.getString(state.command.labelRes)),
                )
            }
            is WidgetFeedback.State.Failed -> {
                show(R.id.w_progress, false); show(R.id.w_status_icon, true)
                setImageViewResource(R.id.w_status_icon, R.drawable.ic_w_alert)
                alt(R.id.w_status_icon)
                setTextViewText(R.id.w_secondary, context.getString(R.string.widget_cant_reach))
                alt(R.id.w_secondary)
            }
        }
    }
}

/** Re-render widgets after something they show changed. */
object WidgetRefresh {

    /** Every placed instance of all three widgets. */
    fun all(context: Context) {
        DeviceWidgetProvider.refreshAll(context)
        RoomsWidgetProvider.refreshAll(context)
        ActionWidgetProvider.refreshAll(context)
        WidgetAvailability.sync(context, CredentialStore(context))
    }

    /** One instance, whichever widget it is. */
    fun one(context: Context, widgetId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = manager.getAppWidgetInfo(widgetId)?.provider ?: return
        val store = CredentialStore(context)
        when (provider.className) {
            DeviceWidgetProvider::class.java.name -> DeviceWidgetProvider.update(context, manager, store, widgetId)
            RoomsWidgetProvider::class.java.name -> RoomsWidgetProvider.update(context, manager, store, widgetId)
            ActionWidgetProvider::class.java.name -> ActionWidgetProvider.update(context, manager, store, widgetId)
        }
    }

    internal fun ids(context: Context, provider: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))
}
