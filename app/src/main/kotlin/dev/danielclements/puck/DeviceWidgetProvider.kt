package dev.danielclements.puck

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import dev.atvremote.protocol.discovery.AppleTvDevice
import dev.danielclements.puck.WidgetLayout.Room
import dev.danielclements.puck.WidgetRender.applyStatus
import dev.danielclements.puck.WidgetRender.background
import dev.danielclements.puck.WidgetRender.alt
import dev.danielclements.puck.WidgetRender.show

/**
 * The Room widget: one paired Apple TV, from a bare mark up to a panel with
 * Play/Pause, Sleep and volume. WIDGET_SPEC.md section 2.
 *
 * The class keeps its original name so widgets placed by earlier versions stay
 * attached to it.
 */
class DeviceWidgetProvider : AppWidgetProvider() {

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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetBindings.unbind(context, it) }
    }

    /** What this instance is showing. */
    private sealed interface State {
        data class Ready(val device: AppleTvDevice) : State

        /** Placed but never given a TV (configuration was optional, or cancelled). */
        data object NotChosen : State

        /** Bound to a TV that is not paired any more. */
        data object Removed : State
    }

    companion object {

        /**
         * Re-renders every placed instance. `updatePeriodMillis="0"` means
         * Android never does it on its own, and what a widget shows can change
         * at any time (a TV renamed, forgotten, or used), so call this wherever
         * [CredentialStore] data changes.
         */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val store = CredentialStore(context)
            WidgetRefresh.ids(context, DeviceWidgetProvider::class.java)
                .forEach { update(context, manager, store, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, store: CredentialStore, id: Int) {
            val key = WidgetBindings.credentialKey(context, id)
            val state = when {
                key == null -> State.NotChosen
                else -> store.loadDevice(key)?.let { State.Ready(it) } ?: State.Removed
            }
            val views = WidgetRender.build(
                manager, id,
                breakpoints = WidgetLayout.roomBreakpoints,
                pickBySize = WidgetLayout::room,
                render = { spec -> render(context, id, spec, state) },
            )
            manager.updateAppWidget(id, views)
        }

        private fun layoutFor(spec: WidgetLayout.RoomSpec): Int = when (spec.layout) {
            Room.MICRO -> R.layout.widget_room_micro
            Room.TILE -> R.layout.widget_room_tile
            Room.CARD, Room.STRIP3, Room.STRIP4 ->
                if (spec.short) R.layout.widget_room_card_short else R.layout.widget_room_card
            Room.PANEL -> R.layout.widget_room_panel
            Room.PANEL_WIDE -> R.layout.widget_room_panel_wide
        }

        private fun render(context: Context, id: Int, spec: WidgetLayout.RoomSpec, state: State): RemoteViews {
            val rv = RemoteViews(context.packageName, layoutFor(spec))
            val isTile = spec.layout == Room.TILE
            val isMicro = spec.layout == Room.MICRO
            val hasButtons = spec.layout == Room.PANEL || spec.layout == Room.PANEL_WIDE ||
                WidgetLayout.hasPlayPause(spec.layout)

            resetToDefaults(context, rv, spec)

            // Buttons exist only for a TV that is there to receive them.
            rv.show(R.id.w_play, false)
            rv.show(R.id.w_rocker, false)

            when (state) {
                is State.Ready -> {
                    val device = state.device
                    WidgetBindings.rememberName(context, id, device.name)
                    val open = WidgetRender.openRemote(context, id, device.credentialKey)
                    rv.setTextViewText(R.id.w_name, device.name)
                    rv.setOnClickPendingIntent(WidgetRender.ROOT, open)
                    rv.setOnClickPendingIntent(R.id.w_open, open)
                    rv.setContentDescription(WidgetRender.ROOT, context.getString(R.string.widget_cd_open, device.name))
                    rv.applyStatus(context, id, normalSecondary(context, spec, device))

                    if (hasButtons) {
                        fun wire(viewId: Int, command: WidgetCommand) {
                            rv.setOnClickPendingIntent(viewId, WidgetRender.command(context, id, device.credentialKey, command))
                            rv.setContentDescription(viewId, context.getString(command.descriptionRes, device.name))
                        }
                        wire(R.id.w_play, WidgetCommand.PLAY_PAUSE)
                        wire(R.id.w_vol_down, WidgetCommand.VOLUME_DOWN)
                        wire(R.id.w_vol_up, WidgetCommand.VOLUME_UP)
                        wire(R.id.w_sleep, WidgetCommand.SLEEP)
                        if (WidgetLayout.hasPlayPause(spec.layout)) rv.show(R.id.w_play, true)
                        if (WidgetLayout.hasRocker(spec.layout)) rv.show(R.id.w_rocker, true)
                    }
                }

                State.NotChosen -> {
                    val configure = WidgetRender.configure(context, id, DeviceWidgetConfigActivity::class.java)
                    rv.setOnClickPendingIntent(WidgetRender.ROOT, configure)
                    rv.setOnClickPendingIntent(R.id.w_open, configure)
                    rv.background(WidgetRender.ROOT, R.drawable.w_bg_card)
                    rv.setContentDescription(WidgetRender.ROOT, context.getString(R.string.widget_choose_a_tv))
                    // A dashed set-up mark in place of the ring and glyph.
                    rv.show(R.id.w_ring, false); rv.show(R.id.w_glyph, false)
                    rv.show(R.id.w_dashed, isMicro || isTile)
                    rv.show(R.id.w_add, true)
                    rv.setTextViewText(R.id.w_name, context.getString(if (isTile) R.string.widget_choose_tv else R.string.widget_choose_a_tv))
                    // The tile's name is in the glyph colour; this state wants on-surface.
                    rv.setTextViewText(R.id.w_name_alt, context.getString(R.string.widget_choose_tv))
                    rv.show(R.id.w_name, !isTile)
                    rv.show(R.id.w_name_alt, isTile)
                    if (!isTile && !isMicro) rv.background(R.id.w_tile, R.drawable.w_square_dashed_56)
                    rv.applyStatus(context, id, context.getString(R.string.widget_tap_to_set_up))
                    rv.show(R.id.w_grid, false)
                }

                is State.Removed -> {
                    val configure = WidgetRender.configure(context, id, DeviceWidgetConfigActivity::class.java)
                    rv.setOnClickPendingIntent(WidgetRender.ROOT, configure)
                    rv.setOnClickPendingIntent(R.id.w_open, configure)
                    rv.background(WidgetRender.ROOT, R.drawable.w_bg_card)
                    rv.setContentDescription(WidgetRender.ROOT, context.getString(R.string.widget_no_longer_paired))
                    rv.setImageViewResource(R.id.w_ring, R.drawable.w_ring_muted)
                    rv.alt(R.id.w_glyph)
                    rv.show(R.id.w_badge, true)
                    rv.setTextViewText(R.id.w_name, WidgetBindings.lastName(context, id) ?: context.getString(R.string.widget_room_label))
                    rv.alt(R.id.w_name)
                    if (!isTile && !isMicro) rv.background(R.id.w_tile, R.drawable.w_mark_outline_56)
                    rv.show(R.id.w_progress, false); rv.show(R.id.w_status_icon, false)
                    rv.setTextViewText(R.id.w_secondary, context.getString(R.string.widget_no_longer_paired))
                    rv.alt(R.id.w_secondary)
                    rv.show(R.id.w_grid, false)
                }
            }
            return rv
        }

        /**
         * Put every property a state can change back to its normal value.
         *
         * The launcher applies an update on top of the views it already has
         * when the layout is the same, so anything one state sets and another
         * does not undo would leak into it: the dashed set-up square staying
         * on a widget that has since been given a TV.
         */
        private fun resetToDefaults(context: Context, rv: RemoteViews, spec: WidgetLayout.RoomSpec) {
            val layout = spec.layout
            rv.background(
                WidgetRender.ROOT,
                when (layout) {
                    Room.MICRO -> R.drawable.w_bg_micro
                    Room.TILE -> R.drawable.w_bg_tile
                    else -> R.drawable.w_bg_card
                },
            )
            rv.show(R.id.w_ring, true)
            rv.setImageViewResource(R.id.w_ring, R.drawable.w_ring)
            rv.show(R.id.w_dashed, false)
            rv.show(R.id.w_glyph, true)
            rv.alt(R.id.w_glyph, false)
            rv.show(R.id.w_add, false)
            rv.show(R.id.w_badge, false)
            val smallTile = spec.short || layout == Room.PANEL || layout == Room.PANEL_WIDE
            rv.background(R.id.w_tile, if (smallTile) R.drawable.w_mark_44 else R.drawable.w_mark_56)
            rv.alt(R.id.w_name, false)
            rv.show(R.id.w_name, true)
            rv.show(R.id.w_name_alt, false)
            rv.show(R.id.w_grid, true)
        }

        /** The second line of the card, strip and panel: what the open area says it will do. */
        private fun normalSecondary(context: Context, spec: WidgetLayout.RoomSpec, device: AppleTvDevice): String =
            when (spec.layout) {
                Room.PANEL, Room.PANEL_WIDE -> context.getString(R.string.widget_open_remote)
                else -> AppleTvModels.friendly(device.model)
            }
    }
}
