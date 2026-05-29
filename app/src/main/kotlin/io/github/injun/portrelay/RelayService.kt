package io.github.injun.portrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RelayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = intent?.takeIf { it.action == ACTION_START }?.toConfig()
        if (config == null) {
            shutdown()
            return START_NOT_STICKY
        }
        startForeground(
            NOTIFICATION_ID,
            notification(config),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        RelayEngine.start(config)
        return START_STICKY
    }

    override fun onDestroy() {
        RelayEngine.stop()
        super.onDestroy()
    }

    private fun shutdown() {
        RelayEngine.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(config: RelayConfig): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Relay", NotificationManager.IMPORTANCE_LOW),
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Port Relay")
            .setContentText("${config.proto.label} :${config.listenPort} → ${config.remoteEndpoint}")
            .setSmallIcon(R.drawable.ic_relay)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "relay"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "io.github.injun.portrelay.START"
        private const val ACTION_STOP = "io.github.injun.portrelay.STOP"

        fun start(context: Context, config: RelayConfig) {
            val intent = Intent(context, RelayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LISTEN, config.listenPort)
                putExtra(EXTRA_HOST, config.remoteHost)
                putExtra(EXTRA_REMOTE, config.remotePort)
                putExtra(EXTRA_PROTO, config.proto.name)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, RelayService::class.java).apply { action = ACTION_STOP })
        }

        private const val EXTRA_LISTEN = "listenPort"
        private const val EXTRA_HOST = "remoteHost"
        private const val EXTRA_REMOTE = "remotePort"
        private const val EXTRA_PROTO = "proto"

        private fun Intent.toConfig(): RelayConfig? {
            val proto = getStringExtra(EXTRA_PROTO)
                ?.let { runCatching { RelayProtocol.valueOf(it) }.getOrNull() } ?: return null
            val listenPort = getIntExtra(EXTRA_LISTEN, -1).takeIf { it > 0 } ?: return null
            val host = getStringExtra(EXTRA_HOST) ?: return null
            val remotePort = getIntExtra(EXTRA_REMOTE, -1).takeIf { it > 0 } ?: return null
            return RelayConfig(listenPort, host, remotePort, proto)
        }
    }
}
