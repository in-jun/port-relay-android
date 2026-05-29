package io.github.injun.portrelay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

object RelayEngine {
    var isRunning by mutableStateOf(false)
        private set

    val logs = mutableStateListOf<LogEntry>()

    private val main = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var work: CoroutineScope? = null
    private var relays: List<Relay> = emptyList()

    fun start(config: RelayConfig) {
        stop()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        work = scope
        scope.launch {
            val bound = config.proto.relays(config, ::log).filter { relay ->
                runCatching { relay.bind() }
                    .onFailure { log("${relay.label}: bind failed (${it.message})") }
                    .isSuccess
            }
            if (bound.isEmpty()) {
                log("Failed to bind. Check if the listen port is already in use.")
                stop()
                return@launch
            }
            relays = bound
            setRunning(true)
            bound.forEach { relay -> launch { relay.serve() } }
        }
    }

    fun stop() {
        relays.forEach(Relay::close)
        relays = emptyList()
        work?.cancel()
        work = null
        setRunning(false, logStopped = true)
    }

    private fun setRunning(running: Boolean, logStopped: Boolean = false) {
        main.launch {
            if (logStopped && isRunning) append("Stopped")
            isRunning = running
        }
    }

    private fun log(message: String) {
        main.launch { append(message) }
    }

    private fun append(message: String) {
        logs.add(LogEntry(message))
        if (logs.size > MAX_LOGS) logs.removeAt(0)
    }

    private const val MAX_LOGS = 200
}
