package com.omnitools.app.tools.timer.alarm

import android.content.Context

/** Remembers a running timer so it survives the app being closed or the phone restarting. Wall-clock based. */
object AlarmStore {
    private const val FILE = "timer_state"
    private const val KEY_END = "end_wall_ms"
    private const val KEY_TOTAL = "total_ms"

    data class Saved(val endWallMs: Long, val totalMs: Long)

    fun save(context: Context, endWallMs: Long, totalMs: Long) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putLong(KEY_END, endWallMs).putLong(KEY_TOTAL, totalMs).apply()
    }

    fun load(context: Context): Saved? {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val end = p.getLong(KEY_END, 0L)
        if (end <= 0L) return null
        return Saved(end, p.getLong(KEY_TOTAL, 0L).coerceAtLeast(1L))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
