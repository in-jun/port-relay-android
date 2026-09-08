package dev.injun.portrelay.ui.relay

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.injun.portrelay.data.RelayForm
import dev.injun.portrelay.theme.PortRelayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RelayScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startIsDisabledUntilTheFormIsValid() {
        composeTestRule.setContent { PortRelayTheme { RelayScreen(state = RelayUiState()) } }

        composeTestRule.onNodeWithText("Start").assertIsNotEnabled()
    }

    @Test
    fun startIsEnabledForAValidForm() {
        val form = RelayForm(listenPort = "5000", remoteHost = "10.0.0.2", remotePort = "6000")
        composeTestRule.setContent { PortRelayTheme { RelayScreen(state = RelayUiState(form = form)) } }

        composeTestRule.onNodeWithText("Start").assertIsEnabled()
    }

    @Test
    fun showsStopWhileRunning() {
        composeTestRule.setContent { PortRelayTheme { RelayScreen(state = RelayUiState(isRunning = true)) } }

        composeTestRule.onNodeWithText("Stop").assertIsEnabled()
    }
}

@Composable
private fun RelayScreen(state: RelayUiState) = RelayScreen(
    state = state,
    onListenPortChange = {},
    onRemoteHostChange = {},
    onRemotePortChange = {},
    onProtocolChange = {},
    onToggle = {},
)
