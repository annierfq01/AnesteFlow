package com.anestesia.app.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.anestesia.app.MainActivity
import com.anestesia.app.data.local.AnestesiaDatabase
import com.anestesia.app.data.local.entity.toDomain
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground Service para monitorizar temporizadores de fármacos.
 *
 * Política de notificaciones:
 * - La notificación PERSISTENTE del Foreground Service es silenciosa, sin sonido,
 *   sin vibración, importancia LOW → aparece en la bandeja pero NO interrumpe.
 * - Las notificaciones de ALERTA (80% y 100%) usan un canal separado de importancia
 *   HIGH, se disparan UNA SOLA VEZ por evento (flag alertAt80Sent / alertAt100Sent),
 *   y son auto-cancelables. Después no se vuelven a emitir.
 * - El servicio se detiene solo cuando no quedan timers activos.
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject
    lateinit var database: AnestesiaDatabase

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitoringJob: Job? = null

    companion object {
        const val ACTION_START  = "ACTION_START_TIMER_SERVICE"
        const val ACTION_STOP   = "ACTION_STOP_TIMER_SERVICE"

        // Dos canales distintos con importancias distintas
        const val CHANNEL_SILENT = "anestesia_silent_channel"   // Foreground persistente - LOW
        const val CHANNEL_ALERT  = "anestesia_alert_channel"    // Alertas clínicas     - HIGH

        const val NOTIF_ID_FOREGROUND = 1001  // Notificación persistente obligatoria del FG Service

        fun startService(context: Context) {
            context.startForegroundService(
                Intent(context, TimerForegroundService::class.java).apply { action = ACTION_START }
            )
        }

        fun stopService(context: Context) {
            context.startService(
                Intent(context, TimerForegroundService::class.java).apply { action = ACTION_STOP }
            )
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Notificación silenciosa obligatoria para el ForegroundService
                startForeground(NOTIF_ID_FOREGROUND, buildSilentForegroundNotification())
                startMonitoringIfNeeded()
            }
            ACTION_STOP -> {
                shutdown()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        serviceScope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Monitoring loop ───────────────────────────────────────────────────────

    private fun startMonitoringIfNeeded() {
        if (monitoringJob?.isActive == true) return
        acquireWakeLock()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                val hadActiveTimers = checkTimers()
                if (!hadActiveTimers) {
                    // Sin timers: apagar el servicio limpiamente
                    withContext(Dispatchers.Main) { shutdown() }
                    break
                }
                delay(10_000L) // Revisar cada 10 segundos — suficiente precisión clínica
            }
        }
    }

    /**
     * Revisa todos los timers activos y emite alertas si corresponde.
     * @return true si hay al menos un timer activo, false si no hay ninguno.
     *
     * REGLA: cada alerta (80% y 100%) se emite exactamente UNA VEZ.
     * Los flags alertAt80Sent y alertAt100Sent se persisten en Room
     * para sobrevivir reinicios del servicio o del dispositivo.
     */
    private suspend fun checkTimers(): Boolean {
        val entities = database.activeTimerDao().getActiveTimers()
        val active = entities.map { it.toDomain() }.filter { !it.isExpired }

        if (active.isEmpty()) return false

        for (timer in active) {
            // Re-leer la entidad fresca para evitar condiciones de carrera
            val entity = database.activeTimerDao().getTimerById(timer.id) ?: continue

            when {
                // ── Alerta 100%: tiempo agotado ──────────────────────────────
                timer.isCritical && !entity.alertAt100Sent -> {
                    sendAlertNotification(
                        id        = 10_000 + timer.id,  // ID único por fármaco
                        title     = "⚠️ ${timer.drugName}: tiempo agotado",
                        body      = buildString {
                            append("Evaluar reinyección.")
                            if (timer.antidote.isNotBlank() && timer.antidote != "N/A")
                                append(" Antídoto: ${timer.antidote}")
                        }
                    )
                    playBeepCritical()
                    database.activeTimerDao().markAlert100Sent(timer.id)
                    database.activeTimerDao().markExpired(timer.id)
                }

                // ── Alerta 80%: ventana cerrándose ───────────────────────────
                timer.isWarning && !entity.alertAt80Sent && !entity.alertAt100Sent -> {
                    sendAlertNotification(
                        id    = 20_000 + timer.id,
                        title = "⚡ ${timer.drugName}: ventana terapéutica",
                        body  = "Efecto al 80%. Preparar reinyección."
                    )
                    playBeepWarning()
                    database.activeTimerDao().markAlert80Sent(timer.id)
                }
            }
        }
        return true
    }

    private fun shutdown() {
        monitoringJob?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Audio (solo cuando hay alerta real) ──────────────────────────────────

    /** Un beep suave — aviso preventivo al 80% */
    private fun playBeepWarning() {
        var tg: ToneGenerator? = null
        try {
            tg = ToneGenerator(AudioManager.STREAM_ALARM, 70)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 600)
            Thread.sleep(700)
            tg.stopTone()
        } catch (_: Exception) {
        } finally {
            tg?.release()
        }
    }

    /** Tres beeps urgentes — tiempo agotado */
    private fun playBeepCritical() {
        var tg: ToneGenerator? = null
        try {
            tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            repeat(3) {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                Thread.sleep(550)
                tg.stopTone()
                Thread.sleep(150)
            }
        } catch (_: Exception) {
        } finally {
            tg?.release()
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // Canal SILENCIOSO para la notificación persistente del Foreground Service.
        // IMPORTANCE_MIN = no hace sonido, no vibra, aparece al final de la bandeja.
        val silentChannel = NotificationChannel(
            CHANNEL_SILENT,
            "AnesteFlow activo",
            NotificationManager.IMPORTANCE_MIN   // ← clave: no molesta
        ).apply {
            description = "Indica que los temporizadores están corriendo"
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }

        // Canal de ALERTAS clínicas reales — sí hace sonido y vibración.
        // Solo se usa cuando hay un evento real (80% o 100%).
        val alertChannel = NotificationChannel(
            CHANNEL_ALERT,
            "Alertas de fármacos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones cuando el efecto de un fármaco se está agotando"
            enableVibration(true)
            setBypassDnd(true)
        }

        nm.createNotificationChannel(silentChannel)
        nm.createNotificationChannel(alertChannel)
    }

    /**
     * Notificación silenciosa OBLIGATORIA para que el ForegroundService
     * pueda correr en segundo plano. El usuario la ve en la bandeja pero
     * no hace ruido ni vibración — no interrumpe.
     */
    private fun buildSilentForegroundNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_SILENT)
            .setContentTitle("AnesteFlow")
            .setContentText("Temporizadores activos en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)                             // sin sonido ni vibración
            .setPriority(NotificationCompat.PRIORITY_MIN) // lo más abajo posible
            .build()
    }

    /**
     * Notificación de alerta clínica real.
     * Se emite exactamente UNA vez por evento (80% o 100%) y es auto-cancelable.
     * NO se actualiza ni se repite.
     */
    private fun sendAlertNotification(id: Int, title: String, body: String) {
        val pi = PendingIntent.getActivity(
            this, id,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pi)
            .setAutoCancel(true)                         // desaparece al tocarla
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOnlyAlertOnce(true)                      // si el mismo ID ya existe, no re-suena
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    // ── Wake lock ─────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AnesteFlow::TimerWakeLock")
            .also { it.acquire(60 * 60 * 1000L) } // máx 1h; se libera en shutdown()
    }
}

// ── Boot receiver ─────────────────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            TimerForegroundService.startService(context)
        }
    }
}
