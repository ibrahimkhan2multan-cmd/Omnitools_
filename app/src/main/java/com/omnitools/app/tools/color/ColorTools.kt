package com.omnitools.app.tools.color

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** HEX / RGB / HSL conversions. All values are plain ints/doubles so the logic is unit-testable. */
object ColorTools {

    data class Rgb(val r: Int, val g: Int, val b: Int)
    data class Hsl(val h: Double, val s: Double, val l: Double) // h: 0..360, s and l: 0..100

    /** Accepts "#RGB", "#RRGGBB", with or without "#". Returns null when invalid. */
    fun parseHex(text: String): Rgb? {
        var t = text.trim().removePrefix("#")
        if (t.length == 3) t = t.map { "$it$it" }.joinToString("")
        if (t.length != 6 || !t.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        val v = t.toInt(16)
        return Rgb((v shr 16) and 0xFF, (v shr 8) and 0xFF, v and 0xFF)
    }

    fun toHex(c: Rgb): String = String.format(Locale.ROOT, "#%02X%02X%02X", c.r.coerceIn(0, 255), c.g.coerceIn(0, 255), c.b.coerceIn(0, 255))

    fun toHsl(c: Rgb): Hsl {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2.0
        val d = mx - mn
        if (d == 0.0) return Hsl(0.0, 0.0, l * 100)
        val s = d / (1 - abs(2 * l - 1))
        val h = when (mx) {
            r -> 60 * (((g - b) / d) % 6)
            g -> 60 * (((b - r) / d) + 2)
            else -> 60 * (((r - g) / d) + 4)
        }
        return Hsl(if (h < 0) h + 360 else h, s * 100, l * 100)
    }

    fun fromHsl(h: Double, s: Double, l: Double): Rgb {
        val hh = ((h % 360) + 360) % 360
        val ss = s.coerceIn(0.0, 100.0) / 100.0
        val ll = l.coerceIn(0.0, 100.0) / 100.0
        val c = (1 - abs(2 * ll - 1)) * ss
        val x = c * (1 - abs((hh / 60) % 2 - 1))
        val m = ll - c / 2
        val (r1, g1, b1) = when {
            hh < 60 -> Triple(c, x, 0.0)
            hh < 120 -> Triple(x, c, 0.0)
            hh < 180 -> Triple(0.0, c, x)
            hh < 240 -> Triple(0.0, x, c)
            hh < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        return Rgb(((r1 + m) * 255).roundToInt(), ((g1 + m) * 255).roundToInt(), ((b1 + m) * 255).roundToInt())
    }

    /** WCAG relative luminance, used to pick readable text on top of a color. */
    fun luminance(c: Rgb): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(c.r) + 0.7152 * ch(c.g) + 0.0722 * ch(c.b)
    }

    /** WCAG contrast ratio between two colors (1 to 21). */
    fun contrast(a: Rgb, b: Rgb): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
