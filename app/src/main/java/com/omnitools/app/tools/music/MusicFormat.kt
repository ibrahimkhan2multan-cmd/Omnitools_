package com.omnitools.app.tools.music

/** Small pure helpers for the music player (unit tested). */
object MusicFormat {
    /** 83_000 ms -> "1:23", 3_700_000 ms -> "1:01:40". Negative values show as 0:00. */
    fun time(ms: Int): String {
        val total = (ms.coerceAtLeast(0) / 1000).toLong()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Position as 0..1; 0 when the length is unknown. */
    fun fraction(position: Int, duration: Int): Float =
        if (duration <= 0) 0f else (position.toFloat() / duration).coerceIn(0f, 1f)

    /** Slider fraction back to a position that is always inside the song. */
    fun seekTarget(fraction: Float, duration: Int): Int =
        if (duration <= 0) 0 else (fraction.coerceIn(0f, 1f) * duration).toInt().coerceIn(0, duration)

    /** A readable name: a blank or missing name becomes "Audio file". */
    fun title(fileName: String?): String = fileName?.trim().takeUnless { it.isNullOrEmpty() } ?: "Audio file"

    /** The name without its extension, for the big title. */
    fun titleNoExtension(fileName: String?): String {
        val t = title(fileName)
        val dot = t.lastIndexOf('.')
        return if (dot > 0 && t.length - dot <= 6) t.substring(0, dot) else t
    }
}
