package com.qtwl.YitongAIzhuanzhan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class GatewayService : Service() {

    enum class RuntimeState {
        STOPPED,
        STARTING,
        RUNNING,
        ERROR
    }

    companion object {
        const val CHANNEL_ID = "qitong_gateway"
        const val NOTIF_ID = 1001
        const val ACTION_START = "com.qtwl.YitongAIzhuanzhan.gateway.START"
        const val ACTION_RESTART = "com.qtwl.YitongAIzhuanzhan.gateway.RESTART"
        const val ACTION_STOP = "com.qtwl.YitongAIzhuanzhan.gateway.STOP"

        private const val TAG = "GatewayService"

        @Volatile
        private var runtimeState: RuntimeState = RuntimeState.STOPPED

        @Volatile
        private var runningPort: Int? = null

        @Volatile
        private var runtimeError: String? = null

        fun state(): RuntimeState = runtimeState
        fun isRunning(): Boolean = runtimeState == RuntimeState.RUNNING
        fun activePort(): Int? = runningPort
        fun lastError(): String? = runtimeError

        fun start(context: Context) {
            val appContext = context.applicationContext
            GatewayPrefs.setEnabled(appContext, true)
            sendStartCommand(appContext, ACTION_START)
        }

        fun restart(context: Context) {
            val appContext = context.applicationContext
            if (!GatewayPrefs.isEnabled(appContext)) return
            sendStartCommand(appContext, ACTION_RESTART)
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            GatewayPrefs.setEnabled(appContext, false)
            runtimeState = RuntimeState.STOPPED
            runningPort = null
            runtimeError = null
            appContext.stopService(Intent(appContext, GatewayService::class.java))
        }

        private fun sendStartCommand(context: Context, action: String) {
            runtimeState = RuntimeState.STARTING
            runtimeError = null
            val intent = Intent(context, GatewayService::class.java).apply { this.action = action }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var gateway: GatewayServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                GatewayPrefs.setEnabled(this, false)
                shutdown(removeNotification = true)
                return START_NOT_STICKY
            }

            ACTION_RESTART -> startGateway(forceRestart = true)
            ACTION_START -> startGateway(forceRestart = false)
            null -> {
                if (GatewayPrefs.isEnabled(this)) {
                    startGateway(forceRestart = false)
                } else {
                    shutdown(removeNotification = true)
                    return START_NOT_STICKY
                }
            }

            else -> {
                if (GatewayPrefs.isEnabled(this)) startGateway(forceRestart = false)
            }
        }

        return if (GatewayPrefs.isEnabled(this)) START_STICKY else START_NOT_STICKY
    }

    @Synchronized
    private fun startGateway(forceRestart: Boolean) {
        val port = GatewayPrefs.getPortNumber(this)
        val existing = gateway
        if (!forceRestart && existing?.isRunning() == true && runningPort == port) {
            publishState(RuntimeState.RUNNING, port = port)
            updateNotification("綦桐AI转站", "网关运行中 · 端口 $port · 等待请求")
            return
        }

        publishState(RuntimeState.STARTING, port = null)
        startForeground(
            NOTIF_ID,
            buildNotification("綦桐AI转站", "正在启动网关 · 端口 $port")
        )

        existing?.stop()
        gateway = null

        val server = GatewayServer(this, port).apply {
            onRequestReceived = { prompt ->
                updateNotification("正在处理", prompt.take(80))
            }
            onReplyReady = { reply ->
                updateNotification("綦桐AI转站", "已收到回复 (${reply.length} 字) · 端口 $port")
            }
            onRequestFailed = { detail ->
                updateNotification("网关请求失败", detail.take(120))
            }
        }

        try {
            server.startServer()
            gateway = server
            GatewayPrefs.setEnabled(this, true)
            publishState(RuntimeState.RUNNING, port = port)
            updateNotification("綦桐AI转站", "网关运行中 · 端口 $port · 等待请求")
        } catch (error: Exception) {
            Log.e(TAG, "Could not start gateway on port $port", error)
            runCatching { server.stop() }
            gateway = null
            GatewayPrefs.setEnabled(this, false)
            publishState(
                RuntimeState.ERROR,
                port = null,
                error = error.message ?: "Could not bind to port $port"
            )
            stopForegroundCompat(remove = true)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "綦桐 AI 网关",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示 OpenAI 兼容网关的实际运行状态"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIF_ID, buildNotification(title, text))
    }

    @Synchronized
    private fun shutdown(removeNotification: Boolean) {
        runCatching { gateway?.stop() }
        gateway = null
        publishState(RuntimeState.STOPPED, port = null)
        stopForegroundCompat(removeNotification)
        stopSelf()
    }

    private fun publishState(state: RuntimeState, port: Int?, error: String? = null) {
        runtimeState = state
        runningPort = port
        runtimeError = error
    }

    private fun stopForegroundCompat(remove: Boolean) {
        stopForeground(if (remove) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { gateway?.stop() }
        gateway = null
        if (runtimeState != RuntimeState.ERROR) {
            publishState(RuntimeState.STOPPED, port = null)
        }
        stopForegroundCompat(remove = true)
        super.onDestroy()
    }
}
