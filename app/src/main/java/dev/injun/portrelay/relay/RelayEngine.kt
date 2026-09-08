package dev.injun.portrelay.relay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Process-wide relay runtime. Owned by [dev.injun.portrelay.service.RelayService],
 * observed by the UI through [isRunning] and [logs]. Has no Android dependencies so it
 * stays unit-testable.
 */
object RelayEngine {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val lock = Any()
    private var work: CoroutineScope? = null
    private var relays: List<Relay> = emptyList()

    fun start(config: RelayConfig) {
        stop()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        synchronized(lock) { work = scope }
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
            synchronized(lock) { relays = bound }
            _isRunning.value = true
            bound.forEach { relay -> launch { relay.serve() } }
        }
    }

    fun stop() {
        synchronized(lock) {
            relays.forEach(Relay::close)
            relays = emptyList()
            work?.cancel()
            work = null
        }
        if (_isRunning.getAndUpdate { false }) log("Stopped")
    }

    private fun log(message: String) {
        _logs.update { (it + LogEntry(message)).takeLast(MAX_LOGS) }
    }

    private const val MAX_LOGS = 200
}
