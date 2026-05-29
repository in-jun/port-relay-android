package io.github.injun.portrelay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress

class UdpRelay(
    private val config: RelayConfig,
    private val log: (String) -> Unit,
) : Relay {
    override val label = "UDP"

    private val lock = Any()
    private var server: DatagramSocket? = null
    private val sessions = mutableMapOf<SocketAddress, DatagramSocket>()
    private val remote by lazy { InetSocketAddress(config.remoteHost, config.remotePort) }

    override fun bind() {
        val socket = DatagramSocket(null)
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(config.listenPort))
        synchronized(lock) { server = socket }
        log("$label: listening on ${config.listenPort}")
    }

    override suspend fun serve() = coroutineScope {
        val socket = synchronized(lock) { server } ?: return@coroutineScope
        val buffer = ByteArray(BUFFER_SIZE)
        while (isActive) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                runInterruptible(Dispatchers.IO) { socket.receive(packet) }
            } catch (_: IOException) {
                break
            }
            val client = packet.socketAddress
            val payload = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
            val outbound = sessionFor(client, socket)
            try {
                runInterruptible(Dispatchers.IO) { outbound.send(DatagramPacket(payload, payload.size, remote)) }
            } catch (_: IOException) {
            }
        }
    }

    private fun CoroutineScope.sessionFor(client: SocketAddress, server: DatagramSocket): DatagramSocket =
        synchronized(lock) {
            sessions[client]?.let { return it }
            val outbound = DatagramSocket()
            sessions[client] = outbound
            log("$label: new session → ${config.remoteEndpoint}")
            launch { returnPath(outbound, client, server) }
            outbound
        }

    private suspend fun returnPath(outbound: DatagramSocket, client: SocketAddress, server: DatagramSocket) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                runInterruptible(Dispatchers.IO) { outbound.receive(packet) }
                val payload = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
                runInterruptible(Dispatchers.IO) { server.send(DatagramPacket(payload, payload.size, client)) }
            }
        } catch (_: IOException) {
        } finally {
            synchronized(lock) { sessions -= client }
            outbound.closeQuietly()
        }
    }

    override fun close() = synchronized(lock) {
        server?.closeQuietly()
        server = null
        sessions.values.forEach { it.closeQuietly() }
        sessions.clear()
    }

    private companion object {
        const val BUFFER_SIZE = 65_536
    }
}
