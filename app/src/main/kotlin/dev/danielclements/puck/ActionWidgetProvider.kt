package dev.danielclements.puck

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import dev.danielclements.puck.WidgetLayout.Action
import dev.danielclements.puck.WidgetRender.applyStatus
import dev.danielclements.puck.WidgetRender.background
import dev.danielclements.puck.WidgetRender.alt
import dev.danielclements.puck.WidgetRender.show

/**
 * The Action button: one tap sends one command (Play/Pause, Wake or Sleep) to
 * one TV, with no app in front. WIDGET_SPEC.md section 4.
 */
class ActionWidgetProvider : AppWidgetProvider() {

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

    /** Colours and icon for each command (WIDGET_SPEC.md section 4). */
    private class Look(val fill: Int, val disc: Int, val icon: Int, val mutedIcon: Int, val tile: Int)

    companion object {

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val store = CredentialStore(context)
            WidgetRefresh.ids(context, ActionWidgetProvider::class.java)
                .forEach { update(context, manager, store, it) }
        }

        private fun look(command: WidgetCommand): Look = when (command) {
            WidgetCommand.WAKE -> Look(R.drawable.w_bg_button, R.drawable.w_disc_button, R.drawable.ic_w_wake_button, R.drawable.ic_w_wake_muted, R.layout.widget_action_tile_button)
            WidgetCommand.SLEEP -> Look(R.drawable.w_bg_tertiary, R.drawable.w_disc_tertiary, R.drawable.ic_w_sleep_tertiary, R.drawable.ic_w_sleep_muted, R.layout.widget_action_tile_tertiary)
            else -> Look(R.drawable.w_bg_hero, R.drawable.w_disc_hero, R.drawable.ic_w_play_pause_hero, R.drawable.ic_w_play_pause_muted, R.layout.widget_action_tile_hero)
        }

        fun update(context: Context, manager: AppWidgetManager, store: CredentialStore, id: Int) {
            val key = WidgetBindings.credentialKey(context, id)
            val command = WidgetBindings.command(context, id)
            val device = key?.let { store.loadDevice(it) }
            if (device != null) WidgetBindings.rememberName(context, id, device.name)

            val views = WidgetRender.build(
                manager, id,
                breakpoints = WidgetLayout.actionBreakpoints,
                pickBySize = WidgetLayout::action,
                render = { size -> render(context, id, size, key, command, device) },
            )
            manager.updateAppWidget(id, views)
        }

        private fun render(
            context: Context,
            id: Int,
            size: Action,
            key: String?,
            command: WidgetCommand?,
            device: dev.atvremote.protocol.discovery.AppleTvDevice?,
        ): RemoteViews {
            // The tile has one layout per fill, because its label colour (on hero, on
            // button, on tertiary) is fixed in the layout. With no command yet the
            // hero one is used, dimmed.
            val layout = when (size) {
                Action.MICRO -> R.layout.widget_action_micro
                Action.TILE -> look(command ?: WidgetCommand.PLAY_PAUSE).tile
                Action.CARD -> R.layout.widget_action_card
            }
            val rv = RemoteViews(context.packageName, layout)
            val configure = WidgetRender.configure(context, id, ActionWidgetConfigActivity::class.java)
            val roomName = device?.name ?: WidgetBindings.lastName(context, id)
            val card = size == Action.CARD

            // Reset what a state can change; see DeviceWidgetProvider.resetToDefaults.
            rv.show(R.id.w_dashed, false)
            rv.show(R.id.w_badge, false)
            rv.show(R.id.w_progress, false)
            rv.show(R.id.w_status_icon, false)
            rv.show(R.id.w_icon, true)
            if (card) rv.background(R.id.w_disc, R.drawable.w_disc_hero)
            rv.alt(R.id.w_name, false)
            rv.alt(R.id.w_secondary, false)

            when {
                // Ready: bound to a TV that exists, with a command.
                device != null && command != null -> {
                    val look = look(command)
                    if (card) {
                        rv.background(R.id.w_disc, look.disc)
                        rv.setTextViewText(R.id.w_name, context.getString(commandTitle(command)))
                        rv.applyStatus(context, id, roomName.orEmpty())
                    } else {
                        rv.background(WidgetRender.ROOT, look.fill)
                        rv.setTextViewText(R.id.w_name, device.name)
                    }
                    rv.setImageViewResource(R.id.w_icon, look.icon)
                    val send = WidgetRender.command(context, id, device.credentialKey, command)
                    rv.setOnClickPendingIntent(WidgetRender.ROOT, send)
                    rv.setContentDescription(WidgetRender.ROOT, context.getString(command.descriptionRes, device.name))
                }

                // Not set up: no TV or command chosen yet.
                key == null || command == null -> {
                    rv.background(WidgetRender.ROOT, R.drawable.w_bg_card)
                    rv.show(R.id.w_dashed, true)
                    rv.setImageViewResource(R.id.w_icon, R.drawable.ic_w_add_muted)
                    rv.alt(R.id.w_name) // dimmed, not the command's colour
                    rv.setTextViewText(R.id.w_name, context.getString(if (card) R.string.widget_choose_a_tv else R.string.widget_set_up))
                    rv.applyStatus(context, id, context.getString(R.string.widget_tap_to_set_up))
                    rv.setOnClickPendingIntent(WidgetRender.ROOT, configure)
                    rv.setContentDescription(WidgetRender.ROOT, context.getString(R.string.widget_set_up))
                }

                // Removed: its TV is not paired any more.
                else -> {
                    val look = look(command)
                    rv.background(WidgetRender.ROOT, R.drawable.w_bg_card)
                    if (card) rv.background(R.id.w_disc, R.drawable.w_disc_surface)
                    rv.setImageViewResource(R.id.w_icon, look.mutedIcon)
                    rv.show(R.id.w_badge, true)
                    rv.setTextViewText(R.id.w_name, if (card) context.getString(commandTitle(command)) else roomName ?: context.getString(R.string.widget_action_label))
                    rv.alt(R.id.w_name)
                    rv.show(R.id.w_progress, false); rv.show(R.id.w_status_icon, false)
                    rv.setTextViewText(R.id.w_secondary, context.getString(R.string.widget_no_longer_paired))
                    rv.alt(R.id.w_secondary)
                    rv.setOnClickPendingIntent(WidgetRender.ROOT, configure)
                    rv.setContentDescription(WidgetRender.ROOT, context.getString(R.string.widget_no_longer_paired))
                }
            }
            return rv
        }

        fun commandTitle(command: WidgetCommand): Int = when (command) {
            WidgetCommand.WAKE -> R.string.widget_action_wake
            WidgetCommand.SLEEP -> R.string.widget_action_sleep
            else -> R.string.widget_action_play_pause
        }
    }
}
