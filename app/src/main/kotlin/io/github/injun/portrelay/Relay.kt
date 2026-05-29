package io.github.injun.portrelay

import java.io.Closeable

interface Relay {
    val label: String

    /** Binds the listen socket; throws if the port is unavailable. */
    fun bind()

    /** Accepts and forwards traffic until the coroutine is cancelled. */
    suspend fun serve()

    /** Releases sockets, unblocking [serve]. */
    fun close()
}

fun RelayProtocol.relays(config: RelayConfig, log: (String) -> Unit): List<Relay> = when (this) {
    RelayProtocol.UDP -> listOf(UdpRelay(config, log))
    RelayProtocol.TCP -> listOf(TcpRelay(config, log))
    RelayProtocol.BOTH -> listOf(UdpRelay(config, log), TcpRelay(config, log))
}

internal fun Closeable.closeQuietly() = try {
    close()
} catch (_: Exception) {
}
