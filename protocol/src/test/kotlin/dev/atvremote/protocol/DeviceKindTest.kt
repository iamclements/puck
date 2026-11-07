package dev.atvremote.protocol

import dev.atvremote.protocol.discovery.AppleTvDevice
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceKindTest {
    private fun device(model: String?) = AppleTvDevice("x", "192.168.1.2", 49153, model = model)

    @Test fun `apple tv models are recognised`() {
        assertTrue(device("AppleTV6,2").isAppleTv)
        assertTrue(device("AppleTV14,1").isAppleTv)
    }

    @Test fun `a mac that sends no model is not an apple tv`() = assertFalse(device(null).isAppleTv)

    @Test fun `other apple devices are not apple tvs`() {
        assertFalse(device("MacBookPro18,3").isAppleTv)
        assertFalse(device("AudioAccessory5,1").isAppleTv)
        assertFalse(device("iPhone14,2").isAppleTv)
    }
}
