package dev.danielclements.puck

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Shown once, when a widget is first dragged onto the home screen: picks
 * which already-paired Apple TV this particular widget instance controls.
 *
 * Standard Android widget-config contract: default to RESULT_CANCELED so
 * backing out (or being killed) before a pick leaves the widget un-added,
 * and finish with RESULT_OK + the widget id only once a device is bound.
 */
class DeviceWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Without this the status bar keeps the light icons of the dark window
        // theme, which vanish against this screen's light background.
        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val store = CredentialStore(this)
        val devices = store.pairedKeys().mapNotNull { store.loadDevice(it) }

        // One TV leaves nothing to choose.
        devices.singleOrNull()?.let {
            finishWithDevice(store, it)
            return
        }

        setContent {
            PuckTheme {
                Surface(Modifier.fillMaxSize()) {
                    DevicePicker(
                        devices,
                        titleRes = R.string.widget_pick_device_title,
                        emptyRes = R.string.widget_pick_device_empty,
                    ) { device -> finishWithDevice(store, device) }
                }
            }
        }
    }

    private fun finishWithDevice(store: CredentialStore, device: dev.atvremote.protocol.discovery.AppleTvDevice) {
        WidgetBindings.bind(this, appWidgetId, device.credentialKey)
        DeviceWidgetProvider.update(this, AppWidgetManager.getInstance(this), store, appWidgetId)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}
