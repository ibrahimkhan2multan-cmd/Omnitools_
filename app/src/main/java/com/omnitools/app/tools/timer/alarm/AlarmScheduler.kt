package com.omnitools.app.tools.timer.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.omnitools.app.MainActivity

/**
 * Schedules the system alarm that rings the timer when the app is closed.
 * Uses AlarmManager.setAlarmClock: it is exact, wakes the phone from sleep, works in Doze, and needs no special
 * permission. Android shows a small alarm icon in the status bar while it is pending.
 */
object AlarmScheduler {
    const val ACTION_FIRE = "com.omnitools.app.TIMER_FIRE"

    fun schedule(context: Context, endWallMs: Long): Boolean {
        return try {
            val am = context.getSystemService(AlarmManager::class.java) ?: return false
            val show = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(endWallMs, show), firePendingIntent(context))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cancel(context: Context) {
        try {
            context.getSystemService(AlarmManager::class.java)?.cancel(firePendingIntent(context))
        } catch (e: Exception) {
            // Nothing scheduled, nothing to cancel.
        }
    }

    private fun firePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
