package dev.danielclements.puck

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Opened on a long-press of a Quick Settings tile — the only
 * configuration hook Android gives a tile, since there is no add-time picker
 * like the home-screen widget has. [Intent.EXTRA_COMPONENT_NAME] says which
 * of the [QuickTiles] slots was pressed, so one activity serves them all.
 *
 * Long-press support for third-party tiles is a launcher behaviour, not
 * guaranteed by the platform — where it isn't wired up, the tile keeps
 * falling back to the most-recently-used device instead.
 */
class TileConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = componentExtra()
        val slot = QuickTiles.slotOf(component?.className)
        if (slot == null) {
            finish()
            return
        }

        val store = CredentialStore(this)
        val devices = store.pairedKeys().mapNotNull { store.loadDevice(it) }

        setContent {
            PuckTheme {
                Surface(Modifier.fillMaxSize()) {
                    DevicePicker(
                        devices,
                        titleRes = R.string.tile_pick_device_title,
                        emptyRes = R.string.tile_pick_device_empty,
                    ) { device ->
                        TileBindings.bind(this, slot, device.credentialKey)
                        TileService.requestListeningState(this, component)
                        finish()
                    }
                }
            }
        }
    }

    private fun componentExtra(): ComponentName? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        }
}
