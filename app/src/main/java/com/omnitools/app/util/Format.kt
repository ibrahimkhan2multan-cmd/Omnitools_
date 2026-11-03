package com.omnitools.app.util

import java.util.Locale

object Format {
    /** Human-readable size using binary (1024) steps with IEC unit names, e.g. 1536 -> "1.5 KiB". */
    fun bytes(value: Long): String {
        if (value < 0) return "unknown"
        if (value < 1024) return "$value B"
        val units = arrayOf("KiB", "MiB", "GiB", "TiB")
        var v = value.toDouble()
        var i = -1
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        return String.format(Locale.ROOT, "%.1f %s", v, units[i])
    }

    /** 1234.5 -> "1,234.50" (always two decimals, English digits). */
    fun money(value: Double): String = String.format(java.util.Locale.US, "%,.2f", value)
}
