package com.omnitools.app.tools.music

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MusicState { EMPTY, LOADING, PLAYING, PAUSED, STOPPED, ERROR }

/**
 * Plays one local audio file. It is an object (not tied to a screen), so the music keeps playing while the
 * user opens other tools. Nothing is downloaded or streamed: the file is read from the phone through the system picker.
 * All calls are made on the main thread.
 */
object MusicController {
    var state by mutableStateOf(MusicState.EMPTY); private set
    var fileName by mutableStateOf<String?>(null); private set
    var durationMs by mutableIntStateOf(0); private set
    var error by mutableStateOf<String?>(null); private set
    var repeat by mutableStateOf(false); private set

    private var player: MediaPlayer? = null
    private var uri: Uri? = null
    private var appContext: Context? = null
    private var generation = 0          // ignores callbacks from a player that has already been replaced
    private var startWhenReady = false
    private var resumeAt = 0
    private var focus: AudioFocusRequest? = null

    /** Current position in ms. Cheap enough to poll a few times a second. */
    fun position(): Int = try {
        when (state) {
            MusicState.PLAYING, MusicState.PAUSED -> player?.currentPosition ?: 0
            else -> 0
        }
    } catch (_: IllegalStateException) { 0 }

    /** Loads the chosen file and starts playing it. Any earlier song is released first. */
    fun open(context: Context, picked: Uri) {
        val ctx = context.applicationContext
        appContext = ctx
        release()
        uri = picked
        fileName = MusicFormat.title(queryName(ctx, picked))
        error = null
        durationMs = 0
        resumeAt = 0
        prepare(autoPlay = true)
    }

    private fun prepare(autoPlay: Boolean) {
        val ctx = appContext ?: return
        val u = uri ?: return
        val gen = ++generation
        state = MusicState.LOADING
        startWhenReady = autoPlay
        val p = MediaPlayer()
        player = p
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            )
            p.setDataSource(ctx, u)
            p.isLooping = repeat
            p.setOnPreparedListener {
                if (gen != generation) return@setOnPreparedListener
                durationMs = it.duration.coerceAtLeast(0)
                if (resumeAt > 0 && resumeAt < durationMs) it.seekTo(resumeAt)
                resumeAt = 0
                if (startWhenReady) startPlayback() else state = MusicState.PAUSED
            }
            p.setOnCompletionListener {
                if (gen != generation) return@setOnCompletionListener
                stop()
            }
            p.setOnErrorListener { _, _, _ ->
                if (gen == generation) fail("This file could not be played. It may be damaged or in a format this phone does not support.")
                true
            }
            p.prepareAsync()
        } catch (e: Exception) {
            if (gen == generation) fail("This file could not be opened. Pick an audio file such as MP3, M4A, WAV, OGG or FLAC.")
        }
    }

    private fun startPlayback() {
        val p = player ?: return
        if (!requestFocus()) { state = MusicState.PAUSED; return }
        try {
            p.start()
            state = MusicState.PLAYING
        } catch (_: IllegalStateException) {
            fail("Playback could not start.")
        }
    }

    fun play() {
        when (state) {
            MusicState.PAUSED -> startPlayback()
            MusicState.STOPPED -> prepare(autoPlay = true)   // Stop freed the player, so load the file again
            MusicState.ERROR -> if (uri != null) { error = null; prepare(autoPlay = true) }
            else -> {}
        }
    }

    fun pause() {
        if (state != MusicState.PLAYING) return
        try { player?.pause() } catch (_: IllegalStateException) {}
        state = MusicState.PAUSED
    }

    fun togglePlayPause() { if (state == MusicState.PLAYING) pause() else play() }

    /** Stops playback and frees the player. The file stays chosen, so Play can start it again from the beginning. */
    fun stop() {
        if (state == MusicState.EMPTY) return
        freePlayer()
        resumeAt = 0
        state = if (uri != null) MusicState.STOPPED else MusicState.EMPTY
    }

    fun seekToFraction(fraction: Float) {
        val p = player ?: return
        if (state != MusicState.PLAYING && state != MusicState.PAUSED) return
        try { p.seekTo(MusicFormat.seekTarget(fraction, durationMs)) } catch (_: IllegalStateException) {}
    }

    fun skip(deltaMs: Int) {
        if (durationMs <= 0) return
        seekToFraction(MusicFormat.fraction(position() + deltaMs, durationMs))
    }

    fun setRepeat(on: Boolean) {
        repeat = on
        try { player?.isLooping = on } catch (_: IllegalStateException) {}
    }

    /** Frees everything (used when the app closes or a new song replaces the old one). */
    fun release() {
        freePlayer()
        uri = null
        fileName = null
        durationMs = 0
        error = null
        state = MusicState.EMPTY
    }

    private fun freePlayer() {
        generation++
        val p = player
        player = null
        abandonFocus()
        if (p != null) {
            try { p.setOnPreparedListener(null); p.setOnCompletionListener(null); p.setOnErrorListener(null) } catch (_: Exception) {}
            try { p.reset() } catch (_: Exception) {}
            try { p.release() } catch (_: Exception) {}
        }
    }

    private fun fail(message: String) {
        freePlayer()
        error = message
        state = MusicState.ERROR
    }

    private fun requestFocus(): Boolean {
        val am = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        val req = focus ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                // Another app (a call, another player) took the sound: pause instead of talking over it.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
            }
            .build().also { focus = it }
        return am.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    private fun abandonFocus() {
        val req = focus ?: return
        (appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.abandonAudioFocusRequest(req)
        focus = null
    }

    private fun queryName(ctx: Context, u: Uri): String? = try {
        ctx.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: u.lastPathSegment?.substringAfterLast('/')
    } catch (_: Exception) { u.lastPathSegment?.substringAfterLast('/') }
}
