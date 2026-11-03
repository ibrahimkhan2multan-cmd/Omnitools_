package com.omnitools.app.tools.timer

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stopwatch state. Time comes from SystemClock.elapsedRealtime(), which keeps counting while the screen is closed
 * as long as the app process stays alive. It is NOT a background service: nothing runs once Android ends the app.
 */
class StopwatchModel {
    var running by mutableStateOf(false)
        private set
    private var accumulated by mutableStateOf(0L)
    private var startedAt by mutableStateOf(0L)

    /** Elapsed time at each lap press (cumulative). */
    val laps = mutableStateListOf<Long>()

    fun elapsed(now: Long = SystemClock.elapsedRealtime()): Long = accumulated + if (running) now - startedAt else 0L

    fun start() {
        if (running) return
        startedAt = SystemClock.elapsedRealtime()
        running = true
    }

    fun pause() {
        if (!running) return
        accumulated += SystemClock.elapsedRealtime() - startedAt
        running = false
    }

    fun lap() {
        if (running) laps.add(0, elapsed())
    }

    fun reset() {
        running = false
        accumulated = 0L
        startedAt = 0L
        laps.clear()
    }
}

/**
 * Countdown timer state. Time comes from SystemClock.elapsedRealtime(). The model only holds state: ringing, the
 * system alarm and saving are handled by [TimerController], which is what the screen should call.
 */
class TimerModel {
    var totalMs by mutableStateOf(5 * 60_000L)
        private set
    var running by mutableStateOf(false)
        private set
    var finished by mutableStateOf(false)
        private set
    private var endAt by mutableStateOf(0L)
    private var remainingWhenPaused by mutableStateOf(5 * 60_000L)

    fun remaining(now: Long = SystemClock.elapsedRealtime()): Long =
        if (running) (endAt - now).coerceAtLeast(0L) else remainingWhenPaused

    /** Sets the length. Any value from 1 millisecond up to [MAX_MS] is accepted. Ignored while running. */
    fun setDuration(ms: Long) {
        if (running) return
        totalMs = ms.coerceIn(MIN_MS, MAX_MS)
        remainingWhenPaused = totalMs
        finished = false
    }

    /** Starts or resumes. Returns the milliseconds left at the moment of starting, or 0 if it could not start. */
    fun start(): Long {
        if (running || remainingWhenPaused <= 0L) return 0L
        finished = false
        endAt = SystemClock.elapsedRealtime() + remainingWhenPaused
        running = true
        return remainingWhenPaused
    }

    fun pause() {
        if (!running) return
        remainingWhenPaused = remaining()
        running = false
    }

    fun reset() {
        running = false
        finished = false
        remainingWhenPaused = totalMs
    }

    fun markFinished() {
        running = false
        finished = true
        remainingWhenPaused = 0L
    }

    fun dismissFinished() {
        finished = false
        remainingWhenPaused = totalMs
    }

    /** Puts a timer that was running when the app process ended back into the running state. */
    fun restoreRunning(remainingMs: Long, total: Long) {
        totalMs = total.coerceIn(MIN_MS, MAX_MS)
        remainingWhenPaused = remainingMs
        endAt = SystemClock.elapsedRealtime() + remainingMs
        finished = false
        running = true
    }

    fun restoreFinished(total: Long) {
        totalMs = total.coerceIn(MIN_MS, MAX_MS)
        running = false
        finished = true
        remainingWhenPaused = 0L
    }

    data class Parts(val hours: Long, val minutes: Long, val seconds: Long, val millis: Long)

    companion object {
        const val MIN_MS = 1L
        const val MAX_MS = 99L * 3_600_000L

        /** Adds up the four fields (each may be any size, e.g. 90 seconds). 0 means nothing was entered. */
        fun durationFrom(hours: Long, minutes: Long, seconds: Long, millis: Long): Long {
            fun lim(v: Long) = v.coerceIn(0L, 1_000_000L)
            val total = lim(hours) * 3_600_000L + lim(minutes) * 60_000L + lim(seconds) * 1_000L + lim(millis)
            return if (total <= 0L) 0L else total.coerceAtMost(MAX_MS)
        }

        fun split(ms: Long): Parts {
            val t = ms.coerceAtLeast(0L)
            return Parts(t / 3_600_000L, (t / 60_000L) % 60, (t / 1_000L) % 60, t % 1_000L)
        }
    }
}

/** One shared instance per app process, so leaving and re-entering the screen keeps the running state. */
object TimerStore {
    val stopwatch = StopwatchModel()
    val timer = TimerModel()
}
