package com.omnitools.app.tools.timer.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omnitools.app.tools.timer.TimerController

/** Receives the system alarm (even when the app is closed) and starts the ringing service. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val app = context.applicationContext
        TimerController.init(app)
        TimerController.fire(app, fromAlarm = true)
    }
}

/** Puts a pending timer back into the system alarm after the phone restarts. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val saved = AlarmStore.load(app) ?: return
        if (saved.endWallMs > System.currentTimeMillis()) {
            AlarmScheduler.schedule(app, saved.endWallMs)
        } else {
            // The time passed while the phone was off, so there is nothing left to ring.
            AlarmStore.clear(app)
        }
    }
}
