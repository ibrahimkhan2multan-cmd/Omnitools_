package com.omnitools.app.tools.timer

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omnitools.app.tools.timer.alarm.AlarmScheduler
import com.omnitools.app.tools.timer.alarm.AlarmService
import com.omnitools.app.tools.timer.alarm.AlarmStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs the countdown timer end to end. Two things can ring it, whichever comes first:
 *  1. an in-app wait that ends at the exact millisecond while the process is alive, and
 *  2. a system alarm (AlarmManager) that wakes the app if it was closed or the phone was asleep.
 * Starting the ring is idempotent, so the second one is ignored. All calls are made on the main thread.
 */
object TimerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var initialized = false

    /** True while the alarm service is ringing. The UI reads this, so it is observable state. */
    var ringing by mutableStateOf(false)
        private set

    /** Restores a timer that was running when the process ended. Safe to call many times. */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val ctx = context.applicationContext
        val saved = AlarmStore.load(ctx) ?: return
        val remaining = saved.endWallMs - System.currentTimeMillis()
        if (remaining > 0) {
            TimerStore.timer.restoreRunning(remaining, saved.totalMs)
            arm(ctx, remaining)
        } else {
            AlarmStore.clear(ctx)
            TimerStore.timer.restoreFinished(saved.totalMs)
        }
    }

    fun start(context: Context) {
        val ctx = context.applicationContext
        init(ctx)
        val model = TimerStore.timer
        val remaining = model.start()
        if (remaining <= 0L) return
        val endWall = System.currentTimeMillis() + remaining
        AlarmStore.save(ctx, endWall, model.totalMs)
        AlarmScheduler.schedule(ctx, endWall)
        arm(ctx, remaining)
    }

    fun pause(context: Context) {
        val ctx = context.applicationContext
        TimerStore.timer.pause()
        disarm(ctx)
    }

    fun reset(context: Context) {
        val ctx = context.applicationContext
        TimerStore.timer.reset()
        disarm(ctx)
    }

    /** Called by the in-app wait and by the system alarm. */
    fun fire(context: Context, fromAlarm: Boolean) {
        val ctx = context.applicationContext
        if (ringing) return
        val model = TimerStore.timer
        if (!model.running && !model.finished) return // paused or reset in the meantime
        val started = AlarmService.ring(ctx, model.totalMs)
        // If Android refuses to start a service from the background, leave the system alarm to do it.
        if (!started && !fromAlarm) return
        disarm(ctx)
        model.markFinished()
        ringing = started
    }

    /** Stop button (screen or notification). */
    fun stopRinging(context: Context) {
        if (ringing) AlarmService.stop(context.applicationContext) else TimerStore.timer.dismissFinished()
    }

    /** Called by the service once the sound has stopped. */
    fun onRingStopped() {
        ringing = false
        TimerStore.timer.dismissFinished()
    }

    private fun arm(ctx: Context, remainingMs: Long) {
        job?.cancel()
        job = scope.launch {
            delay(remainingMs)
            fire(ctx, fromAlarm = false)
        }
    }

    private fun disarm(ctx: Context) {
        job?.cancel()
        job = null
        AlarmScheduler.cancel(ctx)
        AlarmStore.clear(ctx)
    }
}
