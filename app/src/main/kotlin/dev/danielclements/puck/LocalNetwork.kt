package dev.danielclements.puck

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.HttpURLConnection
import java.net.URL

/**
 * Keeps traffic to the Apple TV on the local network.
 *
 * Android picks one "default" network for every socket. When the phone has
 * Wi-Fi that reaches no internet (a LAN-only router, a hotel or captive
 * network) and mobile data is on, the default becomes mobile, and a plain
 * `Socket` to 192.168.x.x is sent out the cellular radio and goes nowhere —
 * even though the Apple TV is right there on Wi-Fi.
 *
 * When that is the situation, the process is bound to the Wi-Fi/Ethernet
 * network. Otherwise nothing is touched, which in particular leaves a VPN
 * (whose network advertises the underlying Wi-Fi transport) alone.
 */
object LocalNetwork {

    enum class Action { LEAVE, BIND, UNBIND }

    /** Pure decision, so it can be tested without a device. */
    fun decide(defaultIsLocal: Boolean, localAvailable: Boolean, bound: Boolean): Action = when {
        defaultIsLocal || !localAvailable -> if (bound) Action.UNBIND else Action.LEAVE
        else -> Action.BIND
    }

    private var started = false
    private var local: Network? = null
    private var defaultIsLocal = false
    private var bound = false
    private lateinit var cm: ConnectivityManager

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

        val localRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        runCatching {
            cm.registerNetworkCallback(localRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = update { local = network }
                override fun onLost(network: Network) = update { if (local == network) local = null }
            })
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = update {
                    defaultIsLocal = isLocal(cm.getNetworkCapabilities(network))
                }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                    update { defaultIsLocal = isLocal(caps) }
                override fun onLost(network: Network) = update { defaultIsLocal = false }
            })
        }.onFailure { android.util.Log.w("atv", "network monitoring unavailable", it) }
    }

    private fun isLocal(caps: NetworkCapabilities?) = caps != null &&
        (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

    @Synchronized
    private fun update(change: () -> Unit) {
        change()
        runCatching {
            when (decide(defaultIsLocal, local != null, bound)) {
                Action.BIND -> { cm.bindProcessToNetwork(local); bound = true }
                Action.UNBIND -> { cm.bindProcessToNetwork(null); bound = false }
                Action.LEAVE -> Unit
            }
        }
    }

    /**
     * A connection for public internet lookups (app icons). While the process
     * is bound to a Wi-Fi network with no internet, these go out over a
     * network that does have it instead of failing.
     */
    @Synchronized
    fun internetConnection(url: String): HttpURLConnection {
        val target = URL(url)
        if (bound) {
            val net = runCatching {
                cm.allNetworks.firstOrNull {
                    cm.getNetworkCapabilities(it)
                        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                }
            }.getOrNull()
            if (net != null) return net.openConnection(target) as HttpURLConnection
        }
        return target.openConnection() as HttpURLConnection
    }
}
