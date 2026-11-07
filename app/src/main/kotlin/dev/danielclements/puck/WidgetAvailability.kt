package dev.danielclements.puck

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Keeps the Rooms widget out of the picker until there is more than one TV to
 * list. WIDGET_SPEC.md section 3.
 *
 * It is never switched off while an instance is already on a home screen:
 * disabling a widget's receiver breaks the placed widgets.
 */
object WidgetAvailability {

    /** Rooms is only offered from this many paired TVs. */
    const val ROOMS_MIN_TVS = 2

    fun roomsOffered(pairedTvs: Int, placedInstances: Int): Boolean =
        pairedTvs >= ROOMS_MIN_TVS || placedInstances > 0

    fun sync(context: Context, store: CredentialStore) {
        val placed = WidgetRefresh.ids(context, RoomsWidgetProvider::class.java).size
        val want = roomsOffered(store.pairedKeys().size, placed)
        val state = if (want) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val component = ComponentName(context, RoomsWidgetProvider::class.java)
        runCatching {
            if (context.packageManager.getComponentEnabledSetting(component) != state) {
                context.packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
