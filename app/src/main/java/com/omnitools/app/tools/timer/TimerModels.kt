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

/** Countdown timer state, same lifetime rules as [StopwatchModel]. */
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

    fun setDuration(ms: Long) {
        if (running) return
        totalMs = ms.coerceIn(1_000L, 99L * 3_600_000L)
        remainingWhenPaused = totalMs
        finished = false
    }

    fun start() {
        if (running || remainingWhenPaused <= 0L) return
        finished = false
        endAt = SystemClock.elapsedRealtime() + remainingWhenPaused
        running = true
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

    /** Called from the UI tick. Marks the timer finished once the end time has passed. */
    fun checkFinished(now: Long = SystemClock.elapsedRealtime()): Boolean {
        if (running && now >= endAt) {
            running = false
            finished = true
            remainingWhenPaused = 0L
            return true
        }
        return false
    }

    fun dismissFinished() {
        finished = false
        remainingWhenPaused = totalMs
    }
}

/** One shared instance per app process, so leaving and re-entering the screen keeps the running state. */
object TimerStore {
    val stopwatch = StopwatchModel()
    val timer = TimerModel()
}
