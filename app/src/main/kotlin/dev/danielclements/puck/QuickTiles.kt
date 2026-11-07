package dev.danielclements.puck

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.quicksettings.TileService

/**
 * Offers one Quick Settings tile per saved Apple TV.
 *
 * Android has no API to add tiles at runtime: each one is a service declared
 * in the manifest. So [SLOTS] are declared, and the ones beyond the number of
 * saved TVs are switched off with the package manager, which removes them from
 * the Quick Settings editor. One TV shows one tile; three show three. The first
 * slot is always offered so a new user can still add a tile and see what it is.
 */
object QuickTiles {

    val services: List<Class<out TileService>> = listOf(
        DeviceTileOne::class.java, DeviceTileTwo::class.java,
        DeviceTileThree::class.java, DeviceTileFour::class.java,
        DeviceTileFive::class.java, DeviceTileSix::class.java,
        DeviceTileSeven::class.java, DeviceTileEight::class.java,
    )

    val SLOTS: Int get() = services.size

    /** Which slot a tile service class name is, or null if it is not one of ours. */
    fun slotOf(className: String?): Int? =
        services.indexOfFirst { it.name == className }.takeIf { it >= 0 }

    /** How many tiles to offer for [savedDevices] paired TVs. */
    fun visibleCount(savedDevices: Int): Int = savedDevices.coerceIn(1, SLOTS)

    fun sync(context: Context, store: CredentialStore) {
        val wanted = visibleCount(store.pairedKeys().size)
        val pm = context.packageManager
        services.forEachIndexed { slot, service ->
            val component = ComponentName(context, service)
            val state = if (slot < wanted) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            runCatching {
                if (pm.getComponentEnabledSetting(component) != state) {
                    pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}
