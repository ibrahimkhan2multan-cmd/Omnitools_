package com.omnitools.app.tools.device

import java.util.Locale

object DeviceFormat {
    /** 1024-based size with one decimal, e.g. 1.5 GB. */
    fun bytes(n: Long): String {
        if (n < 0) return "unknown"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var v = n.toDouble(); var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return if (i == 0) "$n B" else String.format(Locale.US, "%.1f %s", v, units[i])
    }

    fun percent(part: Long, total: Long): Int = if (total <= 0) 0 else ((part * 100.0) / total).toInt().coerceIn(0, 100)

    fun batteryStatus(code: Int): String = when (code) {
        2 -> "Charging"; 3 -> "Discharging"; 4 -> "Not charging"; 5 -> "Full"; else -> "Unknown"
    }
    fun batteryHealth(code: Int): String = when (code) {
        2 -> "Good"; 3 -> "Overheated"; 4 -> "Dead"; 5 -> "Over voltage"; 6 -> "Failure"; 7 -> "Cold"; else -> "Unknown"
    }
    fun plugged(code: Int): String = when {
        code == 0 -> "Not plugged in"
        code and 1 != 0 -> "AC charger"
        code and 2 != 0 -> "USB"
        code and 4 != 0 -> "Wireless"
        else -> "Plugged in"
    }
    fun sensorType(type: Int): String = when (type) {
        1 -> "Accelerometer"; 2 -> "Magnetic field"; 3 -> "Orientation"; 4 -> "Gyroscope"; 5 -> "Light"; 6 -> "Pressure"
        8 -> "Proximity"; 9 -> "Gravity"; 10 -> "Linear acceleration"; 11 -> "Rotation vector"; 12 -> "Humidity"
        13 -> "Ambient temperature"; 15 -> "Game rotation vector"; 18 -> "Step detector"; 19 -> "Step counter"
        20 -> "Geomagnetic rotation"; 21 -> "Heart rate"; 34 -> "Low-latency off-body"; else -> "Other (type $type)"
    }
}
