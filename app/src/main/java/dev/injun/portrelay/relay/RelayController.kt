package dev.injun.portrelay.relay

import android.content.Context
import dev.injun.portrelay.service.RelayService
import kotlinx.coroutines.flow.StateFlow

/** What the UI needs from the relay: observe it, start it, stop it. */
interface RelayController {
    val isRunning: StateFlow<Boolean>
    val logs: StateFlow<List<LogEntry>>
    fun start(config: RelayConfig)
    fun stop()
}

/** Runs the relay inside [RelayService] so it survives the app going to the background. */
class ServiceRelayController(context: Context) : RelayController {
    private val context = context.applicationContext

    override val isRunning: StateFlow<Boolean> get() = RelayEngine.isRunning
    override val logs: StateFlow<List<LogEntry>> get() = RelayEngine.logs

    override fun start(config: RelayConfig) = RelayService.start(context, config)

    override fun stop() = RelayService.stop(context)
}
