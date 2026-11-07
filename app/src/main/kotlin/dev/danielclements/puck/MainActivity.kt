package dev.danielclements.puck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    // A home-screen quick-action shortcut for a specific, already-paired
    // Apple TV carries its credential key here, so the app can connect
    // straight to that device instead of showing the device list.
    private val vm: RemoteViewModel by viewModels {
        RemoteViewModel.factory(application, intent?.getStringExtra(EXTRA_DEVICE_KEY))
    }

    // Registered unconditionally: the contract has to be in place before the
    // activity resumes, whether or not this build will ever ask.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalNetwork.start(this)
        askForNotifications()
        setContent {
            PuckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Passed explicitly: the default `viewModel()` below would
                    // otherwise resolve its own instance via the activity's
                    // default factory, which knows nothing about directKey.
                    AppRoot(vm)
                }
            }
        }
    }

    /**
     * While the remote is on screen and the TV can route volume, the phone's
     * volume keys drive the TV's volume instead of the phone's. Key repeats
     * arrive as further ACTION_DOWNs, so holding a key steps continuously.
     */
    // Lint's RestrictedApi check misfires on overriding this specific method:
    // ComponentActivity.dispatchKeyEvent is a normal public override point,
    // not actually restricted to androidx's own library group — a known
    // false positive, not a real access violation.
    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        ) {
            if (vm.handlesVolumeKeys()) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    vm.onVolumeKey(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Without this the now-playing notification is posted and silently
     * dropped. Asked for up front rather than at the moment of connecting, so
     * the prompt does not land on top of a pairing code.
     */
    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * The activity is single-task, so tapping a shortcut while the app is
     * already running delivers here instead of creating a second instance —
     * route it to the already-live view model rather than relying on a fresh
     * [onCreate].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_DEVICE_KEY)?.let { vm.connectToKnownKey(it) }
    }

    companion object {
        const val EXTRA_DEVICE_KEY = "dev.danielclements.puck.EXTRA_DEVICE_KEY"
    }
}

@Composable
private fun AppRoot(vm: RemoteViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = state.screen) {
            is Screen.DeviceList -> DeviceListScreen(state, vm)
            is Screen.PinEntry -> PinEntryScreen(screen.device, state, vm)
            is Screen.Remote -> RemoteScreen(screen.device, state, vm)
            is Screen.Settings -> SettingsScreen(state, vm)
            is Screen.ManageDevices -> ManageDevicesScreen(state, vm)
        }
    }
}
