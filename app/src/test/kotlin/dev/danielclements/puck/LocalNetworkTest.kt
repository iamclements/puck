package dev.danielclements.puck

import dev.danielclements.puck.LocalNetwork.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalNetworkTest {
    @Test fun bindsWhenDefaultIsMobileAndWifiExists() =
        assertEquals(Action.BIND, LocalNetwork.decide(defaultIsLocal = false, localAvailable = true, bound = false))

    @Test fun leavesAloneWhenDefaultIsAlreadyWifi() =
        assertEquals(Action.LEAVE, LocalNetwork.decide(true, true, bound = false))

    @Test fun unbindsOnceDefaultBecomesWifi() =
        assertEquals(Action.UNBIND, LocalNetwork.decide(true, true, bound = true))

    @Test fun unbindsWhenWifiDisappears() =
        assertEquals(Action.UNBIND, LocalNetwork.decide(false, false, bound = true))

    @Test fun leavesAloneWithNoWifiAtAll() =
        assertEquals(Action.LEAVE, LocalNetwork.decide(false, false, bound = false))
}
