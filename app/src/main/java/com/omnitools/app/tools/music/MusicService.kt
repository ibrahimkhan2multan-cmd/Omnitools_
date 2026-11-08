package com.omnitools.app.tools.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.omnitools.app.MainActivity
import com.omnitools.app.R

/**
 * Keeps the music alive outside the app: a foreground service with a notification (previous / play-pause / next / stop),
 * lock-screen and headset-button control through a media session. All the actual playing is done by [MusicController].
 */
class MusicService : Service() {
    private var session: MediaSession? = null
    private var foreground = false
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { MusicController.pause() }   // headphones unplugged
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Music player", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Controls for the music that is playing"
            setShowBadge(false)
        })
        session = MediaSession(this, "OmniTools music").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = MusicController.play()
                override fun onPause() = MusicController.pause()
                override fun onSkipToNext() = MusicController.next()
                override fun onSkipToPrevious() = MusicController.previous()
                override fun onStop() = MusicController.stop()
                override fun onSeekTo(pos: Long) = MusicController.seekToMs(pos)
            })
            isActive = true
        }
        ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A foreground service must show its notification right away, whatever else happens next.
        postForeground(buildNotification())
        when (intent?.action) {
            ACTION_TOGGLE -> MusicController.togglePlayPause()
            ACTION_NEXT -> MusicController.next()
            ACTION_PREVIOUS -> MusicController.previous()
            ACTION_STOP -> MusicController.stop()
        }
        refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        instance = null
        try { unregisterReceiver(noisy) } catch (_: Exception) {}
        session?.release()
        session = null
        // If the system ends the service while music is playing, free the player too.
        if (MusicController.state == MusicState.PLAYING || MusicController.state == MusicState.LOADING) MusicController.stop()
        super.onDestroy()
    }

    private fun postForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(ID, n)
        foreground = true
    }

    /** Brings the notification and media session in line with the controller. Stops the service when nothing is playing. */
    fun refresh() {
        updateSession()
        val nm = getSystemService(NotificationManager::class.java)
        when (MusicController.state) {
            MusicState.PLAYING, MusicState.LOADING -> postForeground(buildNotification())
            MusicState.PAUSED -> {
                if (foreground) { stopForeground(STOP_FOREGROUND_DETACH); foreground = false }   // lets the user swipe it away
                try { nm.notify(ID, buildNotification()) } catch (_: Exception) {}
            }
            else -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                foreground = false
                nm.cancel(ID)
                stopSelf()
            }
        }
    }

    private fun updateSession() {
        val s = session ?: return
        val c = MusicController
        val st = when (c.state) {
            MusicState.PLAYING -> PlaybackState.STATE_PLAYING
            MusicState.PAUSED -> PlaybackState.STATE_PAUSED
            MusicState.LOADING -> PlaybackState.STATE_BUFFERING
            MusicState.ERROR -> PlaybackState.STATE_ERROR
            else -> PlaybackState.STATE_STOPPED
        }
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP or PlaybackState.ACTION_SEEK_TO,
                )
                .setState(st, c.position().toLong(), if (st == PlaybackState.STATE_PLAYING) 1f else 0f)
                .build(),
        )
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, MusicFormat.titleNoExtension(c.fileName))
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "OmniTools Music Player")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, c.durationMs.toLong())
                .build(),
        )
    }

    private fun action(act: String, icon: Int, label: String, code: Int): Notification.Action {
        val pi = PendingIntent.getService(
            this, code, Intent(this, MusicService::class.java).setAction(act),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, icon), label, pi).build()
    }

    private fun buildNotification(): Notification {
        val c = MusicController
        val playing = c.state == MusicState.PLAYING || c.state == MusicState.LOADING
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val style = Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        session?.let { style.setMediaSession(it.sessionToken) }
        val count = c.queue.size
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(if (c.current == null) "Music Player" else MusicFormat.titleNoExtension(c.fileName))
            .setContentText(if (count > 1) "Song ${c.index + 1} of $count" else "OmniTools Music Player")
            .setContentIntent(open)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(action(ACTION_PREVIOUS, android.R.drawable.ic_media_previous, "Previous", 1))
            .addAction(
                if (playing) action(ACTION_TOGGLE, android.R.drawable.ic_media_pause, "Pause", 2)
                else action(ACTION_TOGGLE, android.R.drawable.ic_media_play, "Play", 2),
            )
            .addAction(action(ACTION_NEXT, android.R.drawable.ic_media_next, "Next", 3))
            .addAction(action(ACTION_STOP, android.R.drawable.ic_menu_close_clear_cancel, "Stop", 4))
            .setStyle(style)
            .build()
    }

    companion object {
        private const val CHANNEL = "music_player"
        private const val ID = 4711
        private const val ACTION_TOGGLE = "com.omnitools.app.music.TOGGLE"
        private const val ACTION_NEXT = "com.omnitools.app.music.NEXT"
        private const val ACTION_PREVIOUS = "com.omnitools.app.music.PREVIOUS"
        private const val ACTION_STOP = "com.omnitools.app.music.STOP"

        @Volatile private var instance: MusicService? = null

        /** Called by the controller whenever something changes. Starts the service on first playback. */
        fun onControllerChanged(context: Context) {
            val running = instance
            if (running != null) { running.refresh(); return }
            val s = MusicController.state
            if (s == MusicState.PLAYING || s == MusicState.LOADING) {
                try {
                    ContextCompat.startForegroundService(context, Intent(context, MusicService::class.java))
                } catch (_: Exception) {
                    // Android may refuse to start it from the background; the music still plays while the app is open.
                }
            }
        }
    }
}
