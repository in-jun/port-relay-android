package dev.injun.portrelay.data

import android.content.Context
import androidx.core.content.edit
import dev.injun.portrelay.relay.RelayProtocol

/** Raw form input as typed by the user; validated into a RelayConfig on start. */
data class RelayForm(
    val listenPort: String = "",
    val remoteHost: String = "",
    val remotePort: String = "",
    val proto: RelayProtocol = RelayProtocol.UDP,
)

/** Persists the last-used form so the app reopens with the previous relay filled in. */
interface RelaySettingsStore {
    fun load(): RelayForm
    fun save(form: RelayForm)
}

class PreferencesRelaySettingsStore(context: Context) : RelaySettingsStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): RelayForm = RelayForm(
        listenPort = prefs.getString(K_LISTEN, "").orEmpty(),
        remoteHost = prefs.getString(K_HOST, "").orEmpty(),
        remotePort = prefs.getString(K_REMOTE, "").orEmpty(),
        proto = prefs.getString(K_PROTO, null)
            ?.let { name -> RelayProtocol.entries.firstOrNull { it.name == name } }
            ?: RelayProtocol.UDP,
    )

    override fun save(form: RelayForm) {
        prefs.edit {
            putString(K_LISTEN, form.listenPort)
            putString(K_HOST, form.remoteHost)
            putString(K_REMOTE, form.remotePort)
            putString(K_PROTO, form.proto.name)
        }
    }

    private companion object {
        const val PREFS = "relay"
        const val K_LISTEN = "listenPort"
        const val K_HOST = "remoteHost"
        const val K_REMOTE = "remotePort"
        const val K_PROTO = "proto"
    }
}
