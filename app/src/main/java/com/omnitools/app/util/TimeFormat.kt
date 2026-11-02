package com.omnitools.app.util

import java.util.Locale

object TimeFormat {
    /** 83_450 ms -> "01:23.45" (minutes:seconds.hundredths); hours appear only when needed: "1:02:03.45". */
    fun stopwatch(ms: Long): String {
        val total = if (ms < 0) 0 else ms
        val hundredths = (total % 1000) / 10
        val seconds = (total / 1000) % 60
        val minutes = (total / 60_000) % 60
        val hours = total / 3_600_000
        return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
        else String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    /** Countdown display, rounded UP to whole seconds so it never shows 00:00 while time remains. */
    fun countdown(ms: Long): String {
        val totalSeconds = if (ms <= 0) 0 else (ms + 999) / 1000
        val s = totalSeconds % 60
        val m = (totalSeconds / 60) % 60
        val h = totalSeconds / 3600
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
    }
}
