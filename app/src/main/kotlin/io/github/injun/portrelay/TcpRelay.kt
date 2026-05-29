package io.github.injun.portrelay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TcpRelay(
    private val config: RelayConfig,
    private val log: (String) -> Unit,
) : Relay {
    override val label = "TCP"

    private val lock = Any()
    private var server: ServerSocket? = null
    private val sessions = mutableSetOf<Socket>()

    override fun bind() {
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(config.listenPort))
        synchronized(lock) { server = socket }
        log("$label: listening on ${config.listenPort}")
    }

    override suspend fun serve() = coroutineScope {
        val socket = synchronized(lock) { server } ?: return@coroutineScope
        while (isActive) {
            val inbound = try {
                runInterruptible(Dispatchers.IO) { socket.accept() }
            } catch (_: IOException) {
                break
            }
            launch { session(inbound) }
        }
    }

    private suspend fun session(inbound: Socket) = coroutineScope {
        val outbound = try {
            runInterruptible(Dispatchers.IO) {
                Socket().apply {
                    tcpNoDelay = true
                    connect(InetSocketAddress(config.remoteHost, config.remotePort))
                }
            }
        } catch (_: IOException) {
            inbound.closeQuietly()
            return@coroutineScope
        }
        inbound.tcpNoDelay = true
        synchronized(lock) { sessions += inbound; sessions += outbound }
        log("$label: new session → ${config.remoteEndpoint}")
        try {
            launch { pump(inbound, outbound) }
            pump(outbound, inbound)
        } finally {
            synchronized(lock) { sessions -= inbound; sessions -= outbound }
        }
    }

    private suspend fun pump(from: Socket, to: Socket) = runInterruptible(Dispatchers.IO) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            val input = from.getInputStream()
            val output = to.getOutputStream()
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                output.flush()
            }
        } catch (_: IOException) {
        } finally {
            from.closeQuietly()
            to.closeQuietly()
        }
    }

    override fun close() = synchronized(lock) {
        server?.closeQuietly()
        server = null
        sessions.forEach { it.closeQuietly() }
        sessions.clear()
    }

    private companion object {
        const val BUFFER_SIZE = 65_536
    }
}
