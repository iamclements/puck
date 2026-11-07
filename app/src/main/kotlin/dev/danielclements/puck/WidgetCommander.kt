package dev.danielclements.puck

import android.content.Context
import dev.atvremote.protocol.companion.AppleTvRemote
import dev.atvremote.protocol.companion.Button
import dev.atvremote.protocol.companion.CompanionClient
import dev.atvremote.protocol.discovery.AppleTvDevice
import dev.atvremote.protocol.hap.Credentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Sends one command to an Apple TV from a widget, with no screen open.
 *
 * The connection is kept in an app-scoped holder and closed after
 * [IDLE_CLOSE_MS] of quiet, so a run of volume taps reuses it for as long as
 * the process lives. A TV that has moved ports (they rotate) is rediscovered
 * once before giving up. WIDGET_SPEC.md section 1.5.
 */
object WidgetCommander {

    /** Also used by the receiver for its own timers. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lock = Mutex()
    private var session: Session? = null
    private var idle: Job? = null

    private class Session(val key: String, val remote: AppleTvRemote)

    suspend fun run(context: Context, key: String, command: WidgetCommand): Result<Unit> =
        runCatching {
            withTimeout(TOTAL_MS) {
                lock.withLock { execute(context.applicationContext, key, command) }
            }
        }

    private suspend fun execute(context: Context, key: String, command: WidgetCommand) {
        idle?.cancel()
        val store = CredentialStore(context)
        val credentials = store.load(key) ?: throw IOException("not paired")
        val device = store.loadDevice(key) ?: throw IOException("not paired")
        try {
            if (command == WidgetCommand.WAKE) {
                wake(context, device, credentials)
            } else {
                obtain(context, device, credentials).press(command.button)
            }
        } catch (e: Exception) {
            // A half-dead session must not be handed to the next tap.
            dropSession()
            throw e
        } finally {
            scheduleIdleClose()
        }
    }

    /**
     * A sleeping Apple TV only answers a bare Companion session, which is all
     * a wake needs: connect, authenticate, one button press.
     */
    private suspend fun wake(context: Context, device: AppleTvDevice, credentials: Credentials) {
        val target = reachable(context, device) { d ->
            val client = CompanionClient(d.address, d.port, scope)
            try {
                withTimeout(CONNECT_MS) {
                    client.connect()
                    client.authenticate(credentials)
                }
                client.hidButton(Button.WAKE.code, down = true)
                client.hidButton(Button.WAKE.code, down = false)
                true
            } catch (e: Exception) {
                false
            } finally {
                runCatching { client.close() }
            }
        }
        if (!target) throw IOException("unreachable")
    }

    private suspend fun obtain(
        context: Context,
        device: AppleTvDevice,
        credentials: Credentials,
    ): AppleTvRemote {
        session?.let {
            if (it.key == device.credentialKey && it.remote.isConnected) return it.remote
            dropSession()
        }
        var connected: AppleTvRemote? = null
        val ok = reachable(context, device) { d ->
            connected = tryConnect(context, d, credentials)
            connected != null
        }
        if (!ok) throw IOException("unreachable")
        return connected!!.also { session = Session(device.credentialKey, it) }
    }

    /**
     * Run [attempt] against the stored address, and if that fails against a
     * freshly discovered one (an Apple TV rotates its Companion port, so a
     * stored port is often stale). True when either attempt succeeds.
     */
    private suspend fun reachable(
        context: Context,
        device: AppleTvDevice,
        attempt: suspend (AppleTvDevice) -> Boolean,
    ): Boolean {
        if (attempt(device)) return true
        val fresh = runCatching {
            NsdDiscovery(context).scan(SCAN_MS).firstOrNull { it.credentialKey == device.credentialKey }
        }.getOrNull() ?: return false
        return attempt(fresh)
    }

    private suspend fun tryConnect(
        context: Context,
        device: AppleTvDevice,
        credentials: Credentials,
    ): AppleTvRemote? {
        val remote = AppleTvRemote(device.address, device.port, credentials, scope, context.ownDeviceName())
        return try {
            withTimeout(CONNECT_MS) { remote.connect() }
            remote
        } catch (e: Exception) {
            runCatching { remote.close() }
            null
        }
    }

    private fun dropSession() {
        session?.let { runCatching { it.remote.close() } }
        session = null
    }

    private fun scheduleIdleClose() {
        idle?.cancel()
        idle = scope.launch {
            delay(IDLE_CLOSE_MS)
            lock.withLock { dropSession() }
        }
    }

    const val IDLE_CLOSE_MS = 30_000L
    private const val CONNECT_MS = 2_500L
    private const val SCAN_MS = 1_500L

    /** Under the 10 s a goAsync() receiver is given. */
    private const val TOTAL_MS = 8_000L
}
