package dev.atvremote.protocol.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

/**
 * Finds an Apple TV at a known address by asking it directly.
 *
 * The Companion port is ephemeral and only advertised over Bonjour, so an IP
 * address alone is not enough to connect. Multicast discovery can fail
 * (guest networks, mesh Wi-Fi, VLANs), but a DNS-SD question sent straight to
 * the device's own UDP 5353 is answered by unicast (RFC 6762 §6.7, a "legacy
 * unicast" query from an ephemeral source port), and carries the port, model
 * and identifier we need.
 */
object UnicastMdns {

    private const val MDNS_PORT = 5353
    private const val TYPE_PTR = 12
    private const val TYPE_TXT = 16
    private const val TYPE_SRV = 33
    private const val TYPE_ANY = 255
    private const val SERVICE = "_companion-link._tcp.local"

    /** A hostname or IP literal, loosely: enough to reject obvious typos before touching the network. */
    fun isValidHost(host: String): Boolean =
        host.length in 1..253 && Regex("^[A-Za-z0-9]([A-Za-z0-9.:\\-]*[A-Za-z0-9])?$").matches(host)

    /** Asks [address] for its Companion service, or returns null if it does not answer. */
    suspend fun lookup(address: String, timeoutMs: Long = 4000): AppleTvDevice? = withContext(Dispatchers.IO) {
        val target = InetAddress.getByName(address)
        DatagramSocket().use { socket ->
            val first = exchange(socket, target, buildQuery(SERVICE, TYPE_PTR), timeoutMs / 2)
                ?: return@withContext null
            var parsed = parse(first) ?: return@withContext null
            if (deviceFrom(address, parsed) == null) {
                // The answer named the service but left out its port: ask for the instance itself.
                val instance = parsed.ptrs.firstOrNull() ?: return@withContext null
                val second = exchange(socket, target, buildQuery(instance, TYPE_ANY), timeoutMs / 2)
                    ?.let { parse(it) }
                if (second != null) parsed = parsed.plus(second)
            }
            deviceFrom(address, parsed)
        }
    }

    private fun exchange(socket: DatagramSocket, target: InetAddress, query: ByteArray, timeoutMs: Long): ByteArray? {
        val deadline = System.currentTimeMillis() + timeoutMs
        val buffer = ByteArray(4096)
        // UDP: send up to twice, in case the first datagram is lost.
        repeat(2) { attempt ->
            socket.send(DatagramPacket(query, query.size, target, MDNS_PORT))
            val slice = if (attempt == 0) timeoutMs / 2 else maxOf(1L, deadline - System.currentTimeMillis())
            socket.soTimeout = slice.coerceAtLeast(1L).toInt()
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                return packet.data.copyOf(packet.length)
            } catch (_: SocketTimeoutException) {
                // fall through to the retry
            }
        }
        return null
    }

    // ---------------------------------------------------------------- wire

    internal fun buildQuery(name: String, type: Int, id: Int = 0): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fun u16(v: Int) { out.write(v shr 8 and 0xFF); out.write(v and 0xFF) }
        u16(id); u16(0); u16(1); u16(0); u16(0); u16(0)
        name.split('.').filter { it.isNotEmpty() }.forEach { label ->
            val bytes = label.toByteArray(Charsets.UTF_8)
            out.write(bytes.size); out.write(bytes)
        }
        out.write(0)
        u16(type)
        u16(0x0001) // class IN; the unicast-response bit is unnecessary from a non-5353 port
        return out.toByteArray()
    }

    internal data class Srv(val port: Int, val target: String)

    internal data class Parsed(
        /** Service instance names from PTR records, e.g. "Living Room._companion-link._tcp.local". */
        val ptrs: List<String>,
        val srv: Map<String, Srv>,
        val txt: Map<String, Map<String, String>>,
    ) {
        fun plus(other: Parsed) = Parsed(
            (ptrs + other.ptrs).distinct(),
            srv + other.srv,
            txt + other.txt,
        )
    }

    internal fun parse(msg: ByteArray): Parsed? = runCatching {
        val buf = ByteBuffer.wrap(msg)
        buf.position(4)
        val questions = buf.short.toInt() and 0xFFFF
        val records = (buf.short.toInt() and 0xFFFF) + (buf.short.toInt() and 0xFFFF) + (buf.short.toInt() and 0xFFFF)
        var pos = 12
        repeat(questions) {
            pos = readName(msg, pos).second + 4
        }
        val ptrs = mutableListOf<String>()
        val srv = mutableMapOf<String, Srv>()
        val txt = mutableMapOf<String, Map<String, String>>()
        repeat(records) {
            val (name, afterName) = readName(msg, pos)
            val type = u16(msg, afterName)
            val rdLen = u16(msg, afterName + 8)
            val rd = afterName + 10
            when (type) {
                TYPE_PTR -> ptrs += readName(msg, rd).first
                TYPE_SRV -> srv[name] = Srv(u16(msg, rd + 4), readName(msg, rd + 6).first)
                TYPE_TXT -> txt[name] = parseTxt(msg, rd, rdLen)
            }
            pos = rd + rdLen
        }
        Parsed(ptrs.distinct(), srv, txt)
    }.getOrNull()

    internal fun deviceFrom(address: String, p: Parsed): AppleTvDevice? {
        val instance = p.ptrs.firstOrNull { it.endsWith(".$SERVICE", ignoreCase = true) } ?: return null
        val port = p.srv.entries.firstOrNull { it.key.equals(instance, ignoreCase = true) }?.value?.port ?: return null
        val txt = p.txt.entries.firstOrNull { it.key.equals(instance, ignoreCase = true) }?.value.orEmpty()
        return AppleTvDevice(
            name = instance.dropLast(SERVICE.length + 1),
            address = address,
            port = port,
            model = txt["rpMd"],
            identifier = txt["rpMRtID"],
        )
    }

    private fun u16(b: ByteArray, at: Int) = (b[at].toInt() and 0xFF shl 8) or (b[at + 1].toInt() and 0xFF)

    private fun parseTxt(b: ByteArray, start: Int, length: Int): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var i = start
        val end = start + length
        while (i < end) {
            val len = b[i].toInt() and 0xFF
            i++
            if (len == 0 || i + len > end) break
            val entry = String(b, i, len, Charsets.UTF_8)
            val eq = entry.indexOf('=')
            if (eq > 0) map[entry.substring(0, eq)] = entry.substring(eq + 1)
            i += len
        }
        return map
    }

    /** Reads a (possibly compressed) DNS name; returns it and the offset just past it in the original position. */
    private fun readName(b: ByteArray, start: Int): Pair<String, Int> {
        val labels = mutableListOf<String>()
        var pos = start
        var end = -1
        var jumps = 0
        while (true) {
            val len = b[pos].toInt() and 0xFF
            when {
                len == 0 -> { if (end < 0) end = pos + 1; break }
                len and 0xC0 == 0xC0 -> {
                    if (++jumps > 32) error("compression loop")
                    if (end < 0) end = pos + 2
                    pos = ((len and 0x3F) shl 8) or (b[pos + 1].toInt() and 0xFF)
                }
                else -> {
                    labels += String(b, pos + 1, len, Charsets.UTF_8)
                    pos += 1 + len
                }
            }
        }
        return labels.joinToString(".") to end
    }
}
