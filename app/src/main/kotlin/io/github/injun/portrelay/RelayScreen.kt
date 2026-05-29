package io.github.injun.portrelay

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.flow.collectLatest

private const val PREFS = "relay"
private const val K_LISTEN = "listenPort"
private const val K_HOST = "remoteHost"
private const val K_REMOTE = "remotePort"
private const val K_PROTO = "proto"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val focus = LocalFocusManager.current

    var listenPort by rememberSaveable { mutableStateOf(prefs.getString(K_LISTEN, "") ?: "") }
    var remoteHost by rememberSaveable { mutableStateOf(prefs.getString(K_HOST, "") ?: "") }
    var remotePort by rememberSaveable { mutableStateOf(prefs.getString(K_REMOTE, "") ?: "") }
    var proto by rememberSaveable {
        mutableStateOf(
            prefs.getString(K_PROTO, RelayProtocol.UDP.name)
                ?.let { runCatching { RelayProtocol.valueOf(it) }.getOrNull() }
                ?: RelayProtocol.UDP,
        )
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(listenPort, remoteHost, remotePort, proto) {
        prefs.edit {
            putString(K_LISTEN, listenPort)
            putString(K_HOST, remoteHost)
            putString(K_REMOTE, remotePort)
            putString(K_PROTO, proto.name)
        }
    }

    val isRunning = RelayEngine.isRunning
    val pendingConfig = RelayConfig.from(listenPort, remoteHost, remotePort, proto).getOrNull()

    fun toggle() {
        focus.clearFocus()
        if (isRunning) {
            RelayService.stop(context)
            errorMessage = null
            return
        }
        RelayConfig.from(listenPort, remoteHost, remotePort, proto)
            .onSuccess {
                RelayService.start(context, it)
                errorMessage = null
            }
            .onFailure { errorMessage = it.message }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Port Relay") },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Listen") {
                OutlinedTextField(
                    value = listenPort,
                    onValueChange = { listenPort = it.filter(Char::isDigit) },
                    label = { Text("Port") },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Remote") {
                OutlinedTextField(
                    value = remoteHost,
                    onValueChange = { remoteHost = it },
                    label = { Text("Host (IP)") },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = remotePort,
                    onValueChange = { remotePort = it.filter(Char::isDigit) },
                    label = { Text("Port") },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Protocol") {
                val options = RelayProtocol.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = proto == option,
                            onClick = { proto = option },
                            enabled = !isRunning,
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(option.label) }
                    }
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = ::toggle,
                enabled = isRunning || pendingConfig != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }

            Section("Log") {
                LogView(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 320.dp))
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
private fun LogView(modifier: Modifier = Modifier) {
    val logs = RelayEngine.logs
    val state = rememberLazyListState()

    LaunchedEffect(state) {
        snapshotFlow { logs.size }.collectLatest { size ->
            if (size > 0) state.animateScrollToItem(size - 1)
        }
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
