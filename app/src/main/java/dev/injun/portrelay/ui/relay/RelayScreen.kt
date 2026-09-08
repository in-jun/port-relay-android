package dev.injun.portrelay.ui.relay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.injun.portrelay.data.RelayForm
import dev.injun.portrelay.relay.LogEntry
import dev.injun.portrelay.relay.RelayProtocol
import dev.injun.portrelay.theme.PortRelayTheme

/** Stateful entry point: binds [RelayViewModel] to the stateless [RelayScreen]. */
@Composable
fun RelayRoute(
    modifier: Modifier = Modifier,
    viewModel: RelayViewModel = viewModel(factory = RelayViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RelayScreen(
        state = state,
        onListenPortChange = viewModel::onListenPortChange,
        onRemoteHostChange = viewModel::onRemoteHostChange,
        onRemotePortChange = viewModel::onRemotePortChange,
        onProtocolChange = viewModel::onProtocolChange,
        onToggle = viewModel::toggle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayScreen(
    state: RelayUiState,
    onListenPortChange: (String) -> Unit,
    onRemoteHostChange: (String) -> Unit,
    onRemotePortChange: (String) -> Unit,
    onProtocolChange: (RelayProtocol) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val editable = !state.isRunning

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Port Relay") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Listen") {
                OutlinedTextField(
                    value = state.form.listenPort,
                    onValueChange = onListenPortChange,
                    label = { Text("Port") },
                    singleLine = true,
                    enabled = editable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Remote") {
                OutlinedTextField(
                    value = state.form.remoteHost,
                    onValueChange = onRemoteHostChange,
                    label = { Text("Host (IP)") },
                    singleLine = true,
                    enabled = editable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.form.remotePort,
                    onValueChange = onRemotePortChange,
                    label = { Text("Port") },
                    singleLine = true,
                    enabled = editable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Protocol") {
                val options = RelayProtocol.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = state.form.proto == option,
                            onClick = { onProtocolChange(option) },
                            enabled = editable,
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(option.label) }
                    }
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    focus.clearFocus()
                    onToggle()
                },
                enabled = state.isRunning || state.canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isRunning) "Stop" else "Start")
            }

            Section("Log") {
                LogView(
                    logs = state.logs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 320.dp)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun LogView(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) state.animateScrollToItem(logs.lastIndex)
    }

    OutlinedCard(modifier = modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(items = logs, key = { it.id }) { entry ->
                Text(
                    text = entry.formatted,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RelayScreenPreview() {
    PortRelayTheme {
        RelayScreen(
            state = RelayUiState(
                form = RelayForm(listenPort = "5000", remoteHost = "192.168.0.10", remotePort = "5000"),
                logs = listOf(LogEntry("UDP: listening on 5000")),
            ),
            onListenPortChange = {},
            onRemoteHostChange = {},
            onRemotePortChange = {},
            onProtocolChange = {},
            onToggle = {},
        )
    }
}
