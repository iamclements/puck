package dev.atvremote.protocol.discovery

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnicastMdnsTest {

    private fun ByteArrayOutputStream.u16(v: Int) { write(v shr 8 and 0xFF); write(v and 0xFF) }
    private fun ByteArrayOutputStream.name(n: String) {
        n.split('.').forEach { write(it.length); write(it.toByteArray()) }
        write(0)
    }

    /** A response with PTR + SRV + TXT, as an Apple TV would send it. */
    private fun response(compressed: Boolean): ByteArray {
        val instance = "Living Room._companion-link._tcp.local"
        val out = ByteArrayOutputStream()
        out.u16(0); out.u16(0x8400); out.u16(0); out.u16(3); out.u16(0); out.u16(0)
        // PTR: _companion-link._tcp.local -> instance
        val ptrNameOffset = out.size()
        out.name("_companion-link._tcp.local"); out.u16(12); out.u16(1); out.u16(0); out.u16(10)
        val ptrRdStart = out.size()
        val ptrRd = ByteArrayOutputStream().also { it.name(instance) }.toByteArray()
        out.u16(ptrRd.size); out.write(ptrRd)
        val instanceOffset = ptrRdStart + 2
        // SRV: instance -> port 49153
        if (compressed) { out.write(0xC0 or (instanceOffset shr 8)); out.write(instanceOffset and 0xFF) } else out.name(instance)
        out.u16(33); out.u16(1); out.u16(0); out.u16(10)
        val srvRd = ByteArrayOutputStream().also { it.u16(0); it.u16(0); it.u16(49153); it.name("Living-Room.local") }.toByteArray()
        out.u16(srvRd.size); out.write(srvRd)
        // TXT
        out.name(instance); out.u16(16); out.u16(1); out.u16(0); out.u16(10)
        val txtRd = ByteArrayOutputStream().also {
            listOf("rpMd=AppleTV14,1", "rpMRtID=ABCD-1234").forEach { e -> it.write(e.length); it.write(e.toByteArray()) }
        }.toByteArray()
        out.u16(txtRd.size); out.write(txtRd)
        check(ptrNameOffset == 12)
        return out.toByteArray()
    }

    @Test
    fun `parses a service answer into a device`() {
        val device = UnicastMdns.deviceFrom("10.0.0.7", assertNotNull(UnicastMdns.parse(response(compressed = false))))
        assertNotNull(device)
        assertEquals("Living Room", device.name)
        assertEquals("10.0.0.7", device.address)
        assertEquals(49153, device.port)
        assertEquals("AppleTV14,1", device.model)
        assertEquals("ABCD-1234", device.identifier)
    }

    @Test
    fun `follows name compression pointers`() {
        val device = UnicastMdns.deviceFrom("10.0.0.7", assertNotNull(UnicastMdns.parse(response(compressed = true))))
        assertEquals(49153, assertNotNull(device).port)
    }

    @Test
    fun `an answer without a port is not a device yet`() {
        val parsed = assertNotNull(UnicastMdns.parse(response(false)))
        assertNull(UnicastMdns.deviceFrom("10.0.0.7", parsed.copy(srv = emptyMap())))
    }

    @Test
    fun `garbage does not crash the parser`() {
        assertNull(UnicastMdns.parse(byteArrayOf(1, 2, 3)))
        assertNull(UnicastMdns.parse(ByteArray(40) { 0xFF.toByte() }))
    }

    @Test
    fun `query encodes the name and type`() {
        val q = UnicastMdns.buildQuery("_companion-link._tcp.local", 12)
        assertEquals(1, q[5].toInt()) // one question
        assertEquals(12, q[q.size - 3].toInt())
        assertEquals(1, q[q.size - 1].toInt())
    }

    @Test
    fun `host validation`() {
        assertTrue(UnicastMdns.isValidHost("192.168.1.20"))
        assertTrue(UnicastMdns.isValidHost("living-room.local"))
        assertFalse(UnicastMdns.isValidHost(""))
        assertFalse(UnicastMdns.isValidHost("192.168.1.20 "))
        assertFalse(UnicastMdns.isValidHost("http://192.168.1.20"))
        assertFalse(UnicastMdns.isValidHost("-bad"))
    }
}
