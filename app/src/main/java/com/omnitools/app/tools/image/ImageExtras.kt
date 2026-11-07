package com.omnitools.app.tools.image

import java.util.Locale
import kotlin.math.abs

/** Finds the main colours of a picture (pure maths, works on an array of ARGB pixels). */
object Palette {
    data class Swatch(val rgb: Int, val share: Double)

    fun hex(rgb: Int): String = String.format(Locale.US, "#%06X", rgb and 0xFFFFFF)

    fun extract(pixels: IntArray, count: Int): List<Swatch> {
        require(count in 1..12) { "Ask for between 1 and 12 colours." }
        val n = IntArray(4096); val sr = LongArray(4096); val sg = LongArray(4096); val sb = LongArray(4096)
        var total = 0
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val r = (p shr 16) and 255; val g = (p shr 8) and 255; val b = p and 255
            val k = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            n[k]++; sr[k] += r.toLong(); sg[k] += g.toLong(); sb[k] += b.toLong(); total++
        }
        if (total == 0) return emptyList()
        val order = (0 until 4096).filter { n[it] > 0 }.sortedByDescending { n[it] }
        val chosen = ArrayList<Swatch>()
        val rgbs = ArrayList<Int>()
        for (k in order) {
            val r = (sr[k] / n[k]).toInt(); val g = (sg[k] / n[k]).toInt(); val b = (sb[k] / n[k]).toInt()
            val rgb = (r shl 16) or (g shl 8) or b
            val similar = rgbs.indexOfFirst { distance(it, rgb) < 48 }
            if (similar >= 0) {
                chosen[similar] = chosen[similar].copy(share = chosen[similar].share + n[k].toDouble() / total)
            } else if (chosen.size < count) {
                rgbs.add(rgb); chosen.add(Swatch(rgb, n[k].toDouble() / total))
            }
        }
        return chosen.sortedByDescending { it.share }
    }

    fun distance(a: Int, b: Int): Int =
        abs(((a shr 16) and 255) - ((b shr 16) and 255)) + abs(((a shr 8) and 255) - ((b shr 8) and 255)) + abs((a and 255) - (b and 255))
}

/** Picks the best quality (and, if needed, a smaller size) so a picture fits under a target file size. */
object SizeFit {
    data class Plan(val quality: Int, val scalePercent: Int, val bytes: Int)

    val scales = listOf(100, 85, 70, 55, 40, 30, 20)
    const val MIN_QUALITY = 20

    /** [encode] returns the file size in bytes for a given quality and scale. Sizes are assumed to grow with quality. */
    fun fit(targetBytes: Int, encode: (quality: Int, scalePercent: Int) -> Int): Plan? {
        require(targetBytes > 0) { "The target size must be above zero." }
        for (scale in scales) {
            if (encode(MIN_QUALITY, scale) > targetBytes) continue
            var lo = MIN_QUALITY; var hi = 95
            var best = Plan(MIN_QUALITY, scale, encode(MIN_QUALITY, scale))
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                val size = encode(mid, scale)
                if (size <= targetBytes) { best = Plan(mid, scale, size); lo = mid + 1 } else hi = mid - 1
            }
            return best
        }
        return null
    }
}
