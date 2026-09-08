package dev.injun.portrelay.relay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayConfigTest {

    @Test
    fun `valid input builds a config`() {
        val config = RelayConfig.from("5000", " 10.0.0.2 ", "6000", RelayProtocol.TCP).getOrThrow()

        assertEquals(RelayConfig(5000, "10.0.0.2", 6000, RelayProtocol.TCP), config)
        assertEquals("10.0.0.2:6000", config.remoteEndpoint)
    }

    @Test
    fun `ports outside 1-65535 are rejected`() {
        assertTrue(RelayConfig.from("0", "h", "1", RelayProtocol.UDP).isFailure)
        assertTrue(RelayConfig.from("1", "h", "65536", RelayProtocol.UDP).isFailure)
        assertTrue(RelayConfig.from("abc", "h", "1", RelayProtocol.UDP).isFailure)
    }

    @Test
    fun `blank host is rejected`() {
        val error = RelayConfig.from("1", "   ", "1", RelayProtocol.UDP).exceptionOrNull()

        assertEquals("Remote host is required", error?.message)
    }
}
