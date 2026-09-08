package dev.injun.portrelay.ui.relay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.injun.portrelay.data.PreferencesRelaySettingsStore
import dev.injun.portrelay.data.RelayForm
import dev.injun.portrelay.data.RelaySettingsStore
import dev.injun.portrelay.relay.LogEntry
import dev.injun.portrelay.relay.RelayConfig
import dev.injun.portrelay.relay.RelayController
import dev.injun.portrelay.relay.RelayProtocol
import dev.injun.portrelay.relay.ServiceRelayController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RelayUiState(
    val form: RelayForm = RelayForm(),
    val isRunning: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val error: String? = null,
) {
    /** True when the form parses into a valid config, i.e. Start may be pressed. */
    val canStart: Boolean get() = form.toConfig().isSuccess
}

fun RelayForm.toConfig(): Result<RelayConfig> = RelayConfig.from(listenPort, remoteHost, remotePort, proto)

class RelayViewModel(
    private val settings: RelaySettingsStore,
    private val relay: RelayController,
) : ViewModel() {

    private val form = MutableStateFlow(settings.load())
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RelayUiState> =
        combine(form, relay.isRunning, relay.logs, error) { form, running, logs, error ->
            RelayUiState(form, running, logs, error)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = RelayUiState(form.value, relay.isRunning.value, relay.logs.value),
        )

    init {
        // Persist every edit; skip the initial value we just loaded.
        viewModelScope.launch { form.drop(1).collect(settings::save) }
    }

    fun onListenPortChange(value: String) = form.update { it.copy(listenPort = value.filter(Char::isDigit)) }

    fun onRemoteHostChange(value: String) = form.update { it.copy(remoteHost = value) }

    fun onRemotePortChange(value: String) = form.update { it.copy(remotePort = value.filter(Char::isDigit)) }

    fun onProtocolChange(value: RelayProtocol) = form.update { it.copy(proto = value) }

    /** Starts the relay from the current form, or stops it if it is running. */
    fun toggle() {
        if (relay.isRunning.value) {
            relay.stop()
            error.value = null
            return
        }
        form.value.toConfig()
            .onSuccess {
                relay.start(it)
                error.value = null
            }
            .onFailure { error.value = it.message }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                RelayViewModel(
                    settings = PreferencesRelaySettingsStore(app),
                    relay = ServiceRelayController(app),
                )
            }
        }
    }
}
