package com.anestesia.app.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.anestesia.app.MainActivity
import com.anestesia.app.R
import com.anestesia.app.data.local.AnestesiaDatabase
import com.anestesia.app.data.local.entity.toDomain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject
    lateinit var database: AnestesiaDatabase

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var soundPool: SoundPool? = null
    private var beepShortId: Int = 0
    private var beepLongId: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "ACTION_START_TIMER_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_TIMER_SERVICE"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "anestesia_timer_channel"

        fun startService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initSoundPool()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Monitorizando fármacos..."))
                startMonitoring()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                checkTimers()
                delay(5_000L) // Revisar cada 5 segundos
            }
        }
    }

    private suspend fun checkTimers() {
        val timers = database.activeTimerDao().getActiveTimers()
        val activeTimers = timers.map { it.toDomain() }.filter { !it.isExpired }

        if (activeTimers.isEmpty()) {
            updateNotification("Sin fármacos activos")
            return
        }

        val criticalCount = activeTimers.count { it.isCritical }
        val warningCount = activeTimers.count { it.isWarning && !it.isCritical }

        val statusText = buildString {
            append("${activeTimers.size} fármaco(s) activo(s)")
            if (criticalCount > 0) append(" · ⚠️ $criticalCount CRÍTICO(S)")
            else if (warningCount > 0) append(" · ⚡ $warningCount en ventana")
        }
        updateNotification(statusText)

        for (timer in activeTimers) {
            val entity = database.activeTimerDao().getTimerById(timer.id) ?: continue

            when {
                timer.isCritical && !entity.alertAt100Sent -> {
                    playBeepCritical()
                    sendAlertNotification(
                        id = timer.id + 2000,
                        title = "⚠️ REINYECCIÓN: ${timer.drugName}",
                        body = "Tiempo agotado. Evaluar dosis. Antídoto: ${timer.antidote.ifEmpty { "N/A" }}"
                    )
                    database.activeTimerDao().markAlert100Sent(timer.id)
                }
                timer.isWarning && !entity.alertAt80Sent -> {
                    playBeepWarning()
                    sendAlertNotification(
                        id = timer.id + 3000,
                        title = "⚡ Ventana cerrando: ${timer.drugName}",
                        body = "80% del tiempo transcurrido. Preparar reinyección."
                    )
                    database.activeTimerDao().markAlert80Sent(timer.id)
                }
            }

            if (timer.isCritical && !entity.isExpired) {
                database.activeTimerDao().markExpired(timer.id)
            }
        }
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Los sonidos se generan programáticamente via ToneGenerator como fallback
        // En producción se agregarían archivos .ogg en res/raw/
        beepShortId = 0
        beepLongId = 0
    }

    private fun playBeepWarning() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 90)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 500)
            serviceScope.launch {
                delay(600)
                toneGen.stopTone()
                toneGen.release()
            }
        } catch (_: Exception) { }
    }

    private fun playBeepCritical() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            repeat(3) { i ->
                serviceScope.launch {
                    delay(i * 600L)
                    toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 400)
                }
            }
            serviceScope.launch {
                delay(2000)
                toneGen.stopTone()
                toneGen.release()
            }
        } catch (_: Exception) { }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Temporizadores Activos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas de fármacos en curso"
            enableVibration(true)
            setBypassDnd(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AnestesIA – Monitorización Activa")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun sendAlertNotification(id: Int, title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AnestesIA::TimerWakeLock"
        ).also { it.acquire(10 * 60 * 1000L) } // 10 min max
    }

    override fun onDestroy() {
        serviceScope.cancel()
        soundPool?.release()
        soundPool = null
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// Boot receiver: reinicia el servicio si había timers activos
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            TimerForegroundService.startService(context)
        }
    }
}
