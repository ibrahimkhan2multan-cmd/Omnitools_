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
 * Plays a queue of local audio files. It is an object (not tied to a screen) and is kept alive by [MusicService],
 * so music keeps playing when other tools are open and when the app is in the background.
 * Nothing is downloaded or streamed: files are read from the phone. All calls are made on the main thread.
 */
object MusicController {
    var state by mutableStateOf(MusicState.EMPTY); private set
    var queue by mutableStateOf<List<Track>>(emptyList()); private set
    var index by mutableIntStateOf(0); private set
    var durationMs by mutableIntStateOf(0); private set
    var error by mutableStateOf<String?>(null); private set
    var repeat by mutableStateOf(RepeatMode.OFF); private set
    var shuffle by mutableStateOf(false); private set

    val current: Track? get() = queue.getOrNull(index)
    val fileName: String? get() = current?.name

    private var player: MediaPlayer? = null
    private var appContext: Context? = null
    private var generation = 0          // ignores callbacks from a player that has already been replaced
    private var failStreak = 0
    private var order: List<Int> = emptyList()
    private var focus: AudioFocusRequest? = null
    private var resumeOnGain = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Set when the user pressed Pause while the song was still loading. */
    var pauseWhenReady by mutableStateOf(false); private set

    /** True when the controls should show "pause" (playing, or loading and about to play). */
    val showsPause: Boolean get() = state == MusicState.PLAYING || (state == MusicState.LOADING && !pauseWhenReady)

    /** Current position in ms. Cheap enough to poll a few times a second. */
    fun position(): Int = try {
        when (state) {
            MusicState.PLAYING, MusicState.PAUSED -> player?.currentPosition ?: 0
            else -> 0
        }
    } catch (_: IllegalStateException) { 0 }

    // ---- building the queue -------------------------------------------------------------------------------

    /** Replaces the queue with [tracks] and starts playing at [start]. */
    fun playQueue(context: Context, tracks: List<Track>, start: Int = 0, shuffleOn: Boolean = shuffle) {
        if (tracks.isEmpty()) return
        appContext = context.applicationContext
        queue = tracks
        shuffle = shuffleOn
        index = start.coerceIn(0, tracks.lastIndex)
        order = QueueLogic.order(tracks.size, index, shuffle)
        error = null
        failStreak = 0
        prepareCurrent()
    }

    /** Adds [tracks] to the end of the queue. If nothing was queued, starts playing them. */
    fun addToQueue(context: Context, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        if (queue.isEmpty()) { playQueue(context, tracks); return }
        appContext = context.applicationContext
        val oldSize = queue.size
        queue = queue + tracks
        order = QueueLogic.orderAfterAppend(order, oldSize, tracks.size, shuffle)
    }

    /** Jumps to the song at [i] in the queue. */
    fun playIndex(i: Int) {
        if (i !in queue.indices) return
        index = i
        if (shuffle) order = QueueLogic.order(queue.size, index, true)
        error = null
        failStreak = 0
        prepareCurrent()
    }

    fun removeFromQueue(i: Int) {
        if (i !in queue.indices) return
        val wasCurrent = i == index
        val wasActive = state == MusicState.PLAYING || state == MusicState.LOADING
        val newQueue = queue.filterIndexed { n, _ -> n != i }
        if (newQueue.isEmpty()) { clear(); return }
        if (wasCurrent) freePlayer()
        queue = newQueue
        index = QueueLogic.indexAfterRemoval(index, i, newQueue.size)
        order = QueueLogic.orderAfterRemoval(order, i)
        if (wasCurrent) {
            if (wasActive) prepareCurrent() else moveTo(MusicState.STOPPED)
        }
    }

    /** Stops, frees everything and empties the queue. */
    fun clear() {
        freePlayer()
        queue = emptyList()
        order = emptyList()
        index = 0
        durationMs = 0
        error = null
        moveTo(MusicState.EMPTY)
    }

    // ---- playback -----------------------------------------------------------------------------------------

    private fun prepareCurrent() {
        val ctx = appContext ?: return
        val track = current ?: return
        freePlayer(keepFocus = true)   // keep audio focus between songs so other apps cannot grab it in the gap
        val gen = ++generation
        durationMs = 0
        pauseWhenReady = false
        moveTo(MusicState.LOADING)
        val p = MediaPlayer()
        player = p
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
            )
            p.setWakeMode(ctx, android.os.PowerManager.PARTIAL_WAKE_LOCK)   // keeps the CPU awake while the screen is off
            p.setDataSource(ctx, Uri.parse(track.uri))
            p.setOnPreparedListener {
                if (gen != generation) return@setOnPreparedListener
                failStreak = 0
                durationMs = it.duration.coerceAtLeast(0)
                if (pauseWhenReady) { pauseWhenReady = false; moveTo(MusicState.PAUSED) } else startPlayback()
            }
            p.setOnCompletionListener { if (gen == generation) onTrackEnded() }
            p.setOnErrorListener { _, _, _ -> onTrackFailed(gen); true }
            p.prepareAsync()
        } catch (e: Exception) {
            onTrackFailed(gen)
        }
    }

    private fun startPlayback() {
        val p = player ?: return
        if (!requestFocus()) { moveTo(MusicState.PAUSED); return }
        try {
            p.start()
            moveTo(MusicState.PLAYING)
        } catch (_: IllegalStateException) {
            fail("Playback could not start.")
        }
    }

    private fun onTrackEnded() {
        error = null
        val next = QueueLogic.next(order, index, repeat, auto = true)
        if (next == null) {
            // End of the queue: free the player and get ready to start again from the top.
            freePlayer()
            index = order.firstOrNull() ?: 0
            moveTo(MusicState.STOPPED)
        } else {
            index = next
            prepareCurrent()
        }
    }

    private fun onTrackFailed(gen: Int) {
        if (gen != generation) return
        val bad = current?.name.orEmpty()
        failStreak++
        freePlayer()
        if (queue.size > 1 && failStreak < queue.size) {
            val next = QueueLogic.next(order, index, if (repeat == RepeatMode.OFF) RepeatMode.OFF else RepeatMode.ALL, auto = false)
            if (next != null) {
                error = "Skipped \"$bad\": it could not be played."
                index = next
                // Posted, not called directly, so a long run of missing files cannot pile up on the call stack.
                val g = generation
                handler.post { if (g == generation) prepareCurrent() }
                return
            }
        }
        failStreak = 0
        fail(
            if (queue.size > 1) "None of the remaining songs could be played. They may be moved, deleted or in an unsupported format."
            else "This file could not be played. It may be damaged, moved, or in a format this phone does not support.",
        )
    }

    fun play() {
        when (state) {
            MusicState.PAUSED -> startPlayback()
            MusicState.STOPPED, MusicState.ERROR -> if (current != null) { error = null; failStreak = 0; prepareCurrent() }
            else -> {}
        }
    }

    fun pause() {
        resumeOnGain = false
        if (state != MusicState.PLAYING) return
        try { player?.pause() } catch (_: IllegalStateException) {}
        moveTo(MusicState.PAUSED)
    }

    fun togglePlayPause() {
        when (state) {
            MusicState.PLAYING -> pause()
            MusicState.LOADING -> pauseWhenReady = !pauseWhenReady
            else -> play()
        }
    }

    /** Stops playback and frees the player. The queue stays, so Play starts the current song again from the beginning. */
    fun stop() {
        if (state == MusicState.EMPTY) return
        freePlayer()
        moveTo(if (queue.isNotEmpty()) MusicState.STOPPED else MusicState.EMPTY)
    }

    fun next() {
        if (queue.isEmpty()) return
        val n = QueueLogic.next(order, index, if (repeat == RepeatMode.ONE) RepeatMode.ALL else repeat, auto = false) ?: return
        index = n; error = null; failStreak = 0
        prepareCurrent()
    }

    /** Restarts the song if it has played for more than 3 seconds, otherwise goes to the one before. */
    fun previous() {
        if (queue.isEmpty()) return
        if (position() > 3000) { seekToFraction(0f); return }
        val n = QueueLogic.previous(order, index, repeat)
        if (n == null) { seekToFraction(0f); return }
        index = n; error = null; failStreak = 0
        prepareCurrent()
    }

    fun seekToFraction(fraction: Float) {
        val p = player ?: return
        if (state != MusicState.PLAYING && state != MusicState.PAUSED) return
        try { p.seekTo(MusicFormat.seekTarget(fraction, durationMs)) } catch (_: IllegalStateException) {}
        changed()
    }

    fun seekToMs(ms: Long) { if (durationMs > 0) seekToFraction(MusicFormat.fraction(ms.toInt(), durationMs)) }

    fun skip(deltaMs: Int) {
        if (durationMs <= 0) return
        seekToFraction(MusicFormat.fraction(position() + deltaMs, durationMs))
    }

    fun cycleRepeat() { repeat = repeat.next() }

    fun toggleShuffle() {
        shuffle = !shuffle
        order = QueueLogic.order(queue.size, index, shuffle)   // a fresh shuffle (or back to normal order) starting at this song
    }

    /** Frees everything (used when the service is destroyed). */
    fun release() = clear()

    // ---- internals ----------------------------------------------------------------------------------------

    private fun moveTo(s: MusicState) {
        state = s
        changed()
    }

    private fun changed() {
        val ctx = appContext ?: return
        MusicService.onControllerChanged(ctx)
    }

    private fun freePlayer(keepFocus: Boolean = false) {
        generation++
        val p = player
        player = null
        resumeOnGain = false
        if (!keepFocus) abandonFocus()
        if (p != null) {
            try { p.setOnPreparedListener(null); p.setOnCompletionListener(null); p.setOnErrorListener(null) } catch (_: Exception) {}
            try { p.reset() } catch (_: Exception) {}
            try { p.release() } catch (_: Exception) {}
        }
    }

    private fun fail(message: String) {
        freePlayer()
        error = message
        moveTo(MusicState.ERROR)
    }

    private fun requestFocus(): Boolean {
        val am = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        val req = focus ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    // A call or another player took the sound for good: pause and stay paused.
                    AudioManager.AUDIOFOCUS_LOSS -> pause()
                    // Taken for a moment (a call, an alarm): pause now, carry on by ourselves when it is given back.
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { val was = state == MusicState.PLAYING; pause(); resumeOnGain = was }
                    // A short notification sound: just turn down.
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> try { player?.setVolume(0.25f, 0.25f) } catch (_: Exception) {}
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        try { player?.setVolume(1f, 1f) } catch (_: Exception) {}
                        if (resumeOnGain) { resumeOnGain = false; if (state == MusicState.PAUSED) startPlayback() }
                    }
                }
            }
            .build().also { focus = it }
        return am.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    private fun abandonFocus() {
        val req = focus ?: return
        (appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.abandonAudioFocusRequest(req)
        focus = null
    }

    /** The display name of a picked file, or null if the provider does not give one. */
    fun queryName(ctx: Context, u: Uri): String? = try {
        ctx.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: u.lastPathSegment?.substringAfterLast('/')
    } catch (_: Exception) { u.lastPathSegment?.substringAfterLast('/') }
}
