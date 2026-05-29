package io.github.injun.portrelay

enum class RelayProtocol(val label: String) {
    UDP("UDP"),
    TCP("TCP"),
    BOTH("Both"),
}

data class RelayConfig(
    val listenPort: Int,
    val remoteHost: String,
    val remotePort: Int,
    val proto: RelayProtocol,
) {
    val remoteEndpoint: String get() = "$remoteHost:$remotePort"

    companion object {
        fun from(
            listenPort: String,
            remoteHost: String,
            remotePort: String,
            proto: RelayProtocol,
        ): Result<RelayConfig> = runCatching {
            val lp = listenPort.toPort() ?: error("Listen port must be 1–65535")
            val host = remoteHost.trim().ifEmpty { error("Remote host is required") }
            val rp = remotePort.toPort() ?: error("Remote port must be 1–65535")
            RelayConfig(lp, host, rp, proto)
        }
    }
}

private fun String.toPort(): Int? = toIntOrNull()?.takeIf { it in 1..65535 }
