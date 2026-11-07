package dev.danielclements.puck

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.atvremote.protocol.discovery.AppleTvDevice

/**
 * Setting up an Action button: choose a TV, then what the button does. With
 * a single paired TV the first step is skipped.
 */
class ActionWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        setContent {
            PuckTheme {
                Surface(Modifier.fillMaxSize()) {
                    var chosen by remember { mutableStateOf<AppleTvDevice?>(devices.singleOrNull()) }
                    val device = chosen
                    if (device == null) {
                        DevicePicker(
                            devices,
                            titleRes = R.string.widget_pick_tv_title,
                            emptyRes = R.string.widget_pick_device_empty,
                        ) { chosen = it }
                    } else {
                        ActionPicker { command -> finish(store, device, command) }
                    }
                }
            }
        }
    }

    private fun finish(store: CredentialStore, device: AppleTvDevice, command: WidgetCommand) {
        WidgetBindings.bindAction(this, appWidgetId, device.credentialKey, command)
        ActionWidgetProvider.update(this, AppWidgetManager.getInstance(this), store, appWidgetId)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

@androidx.compose.runtime.Composable
private fun ActionPicker(onPick: (WidgetCommand) -> Unit) {
    val options = listOf(
        Triple(WidgetCommand.PLAY_PAUSE, R.string.widget_action_play_pause, R.string.widget_action_play_pause_desc),
        Triple(WidgetCommand.WAKE, R.string.widget_action_wake, R.string.widget_action_wake_desc),
        Triple(WidgetCommand.SLEEP, R.string.widget_action_sleep, R.string.widget_action_sleep_desc),
    )
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.widget_pick_action_title), style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.padding(top = 8.dp)) {
            options.forEach { (command, title, desc) ->
                ListItem(
                    headlineContent = { Text(stringResource(title)) },
                    supportingContent = { Text(stringResource(desc)) },
                    modifier = Modifier.fillMaxWidth().clickable { onPick(command) },
                )
            }
        }
    }
}
