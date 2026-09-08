package dev.injun.portrelay.ui.relay

import dev.injun.portrelay.data.RelayForm
import dev.injun.portrelay.data.RelaySettingsStore
import dev.injun.portrelay.relay.LogEntry
import dev.injun.portrelay.relay.RelayConfig
import dev.injun.portrelay.relay.RelayController
import dev.injun.portrelay.relay.RelayProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RelayViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val store = FakeSettingsStore()
    private val relay = FakeRelayController()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts with the persisted form`() = runTest(dispatcher) {
        store.form = RelayForm("5000", "10.0.0.2", "6000", RelayProtocol.TCP)
        val viewModel = RelayViewModel(store, relay)

        assertEquals(store.form, viewModel.uiState.value.form)
        assertTrue(viewModel.uiState.value.canStart)
    }

    @Test
    fun `edits are sanitized and persisted`() = runTest(dispatcher) {
        val viewModel = RelayViewModel(store, relay)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onListenPortChange("50a0")
        viewModel.onRemoteHostChange("host")
        viewModel.onRemotePortChange("6000")
        viewModel.onProtocolChange(RelayProtocol.BOTH)

        assertEquals(RelayForm("500", "host", "6000", RelayProtocol.BOTH), store.form)
        assertEquals(store.form, viewModel.uiState.value.form)
    }

    @Test
    fun `toggle with an invalid form surfaces the error and does not start`() = runTest(dispatcher) {
        val viewModel = RelayViewModel(store, relay)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.toggle()

        assertEquals("Listen port must be 1–65535", viewModel.uiState.value.error)
        assertFalse(relay.isRunning.value)
    }

    @Test
    fun `toggle starts then stops the relay`() = runTest(dispatcher) {
        store.form = RelayForm("5000", "10.0.0.2", "6000", RelayProtocol.UDP)
        val viewModel = RelayViewModel(store, relay)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.toggle()
        assertEquals(RelayConfig(5000, "10.0.0.2", 6000, RelayProtocol.UDP), relay.started)
        assertTrue(viewModel.uiState.value.isRunning)
        assertNull(viewModel.uiState.value.error)

        viewModel.toggle()
        assertFalse(viewModel.uiState.value.isRunning)
    }

    private class FakeSettingsStore : RelaySettingsStore {
        var form = RelayForm()
        override fun load() = form
        override fun save(form: RelayForm) {
            this.form = form
        }
    }

    private class FakeRelayController : RelayController {
        var started: RelayConfig? = null
        override val isRunning = MutableStateFlow(false)
        override val logs: StateFlow<List<LogEntry>> = MutableStateFlow(emptyList())

        override fun start(config: RelayConfig) {
            started = config
            isRunning.value = true
        }

        override fun stop() {
            isRunning.value = false
        }
    }
}
