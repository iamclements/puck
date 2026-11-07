package dev.danielclements.puck

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.atvremote.protocol.discovery.AppleTvDevice

/**
 * A Quick Settings tile pinned to one specific, already-paired Apple TV,
 * chosen via a long-press (see [TileConfigActivity]) — mirroring what the
 * home-screen widget does.
 *
 * Android doesn't let an app spawn one tile per paired device dynamically —
 * each tile is a separately declared component the user adds by hand from
 * the Quick Settings editor — so there is a fixed set of slots ([QuickTiles]),
 * of which only as many as there are saved TVs are offered. Until a slot
 * is explicitly bound, it falls back to "the Nth most recently used Apple
 * TV," same as the long-press launcher shortcuts ([AppShortcuts]).
 */
abstract class DeviceTileServiceBase(private val slot: Int) : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val device = resolveDevice()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (device != null) putExtra(MainActivity.EXTRA_DEVICE_KEY, device.credentialKey)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun resolveDevice(): AppleTvDevice? {
        val store = CredentialStore(this)
        val bound = TileBindings.credentialKey(this, slot)?.let { store.loadDevice(it) }
        return bound ?: store.recentDevices(slot + 1).getOrNull(slot)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val device = resolveDevice()
        tile.label = device?.name ?: getString(R.string.tile_unconfigured_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_remote)
        tile.state = if (device != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (device != null) getString(R.string.tile_subtitle) else null
        }
        tile.updateTile()
    }
}

/** Slot 0: the single most recently used Apple TV. */
class DeviceTileOne : DeviceTileServiceBase(slot = 0)

/** Slot 1: the next most recently used Apple TV. */
class DeviceTileTwo : DeviceTileServiceBase(slot = 1)

/** Slot 2: the next most recently used Apple TV. */
class DeviceTileThree : DeviceTileServiceBase(slot = 2)

/** Slot 3: the next most recently used Apple TV. */
class DeviceTileFour : DeviceTileServiceBase(slot = 3)

/** Slot 4: the next most recently used Apple TV. */
class DeviceTileFive : DeviceTileServiceBase(slot = 4)

/** Slot 5: the next most recently used Apple TV. */
class DeviceTileSix : DeviceTileServiceBase(slot = 5)

/** Slot 6: the next most recently used Apple TV. */
class DeviceTileSeven : DeviceTileServiceBase(slot = 6)

/** Slot 7: the next most recently used Apple TV. */
class DeviceTileEight : DeviceTileServiceBase(slot = 7)
