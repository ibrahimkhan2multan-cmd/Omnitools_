package com.omnitools.app.tools.timer.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.omnitools.app.R
import com.omnitools.app.tools.timer.TimerController
import com.omnitools.app.util.TimeFormat

/**
 * Foreground service that plays the alarm sound and vibrates until the user taps Stop (or 5 minutes pass).
 * The sound is the phone's default alarm tone, played on the alarm audio stream, so it follows the Alarm volume.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ringing = false
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRinging()
        } else {
            startRinging(intent?.getLongExtra(EXTRA_TOTAL, 0L) ?: 0L)
        }
        return START_NOT_STICKY
    }

    private fun startRinging(totalMs: Long) {
        ensureChannel()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(totalMs), type)
        } catch (e: Exception) {
            // Still try to make noise below, even if the system refused the notification.
        }
        if (ringing) return
        ringing = true
        playSound()
        startVibration()
        handler.postDelayed(autoStop, MAX_RING_MS)
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStop)
        player?.let {
            try { it.stop() } catch (e: Exception) { /* not started */ }
            it.release()
        }
        player = null
        try { vibrator?.cancel() } catch (e: Exception) { /* ignore */ }
        vibrator = null
        ringing = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        TimerController.onRingStopped()
    }

    override fun onDestroy() {
        if (ringing) stopRinging()
        super.onDestroy()
    }

    private fun playSound() {
        val uris = listOfNotNull(
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri in uris) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                mp.prepare()
                mp.start()
                player = mp
                return
            } catch (e: Exception) {
                mp.release()
            }
        }
        // No sound could be played; vibration and the notification still tell the user.
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (vib == null || !vib.hasVibrator()) return
        try {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attrs)
            vibrator = vib
        } catch (e: Exception) {
            // Vibration is a bonus.
        }
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "Timer alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while the timer is ringing"
            setSound(null, null) // the service plays the alarm sound itself, on the alarm stream
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(totalMs: Long): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, AlarmService::class.java).setAction(ACTION_STOP), flags,
        )
        val screenIntent = PendingIntent.getActivity(
            this, 3,
            Intent(this, AlarmRingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )
        val detail = if (totalMs > 0) "Set for ${TimeFormat.countdownPrecise(totalMs)}" else "Your timer has finished"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Time's up!")
            .setContentText(detail)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(screenIntent)
            .setFullScreenIntent(screenIntent, true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    companion object {
        const val ACTION_RING = "com.omnitools.app.RING"
        const val ACTION_STOP = "com.omnitools.app.STOP_RING"
        const val EXTRA_TOTAL = "total_ms"
        private const val CHANNEL_ID = "timer_alarm"
        private const val NOTIFICATION_ID = 4001
        private const val MAX_RING_MS = 5 * 60_000L

        /** Starts ringing. Returns false if Android refused (for example when the app is in the background). */
        fun ring(context: Context, totalMs: Long): Boolean = try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java).setAction(ACTION_RING).putExtra(EXTRA_TOTAL, totalMs),
            )
            true
        } catch (e: Exception) {
            false
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                TimerController.onRingStopped()
            }
        }
    }
}
