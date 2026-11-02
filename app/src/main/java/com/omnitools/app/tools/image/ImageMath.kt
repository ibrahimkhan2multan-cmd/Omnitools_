package com.omnitools.app.tools.image

import kotlin.math.roundToInt

/** Geometry helpers for the image editor. Pure Kotlin, unit tested. */
object ImageMath {

    data class CropBox(val x: Int, val y: Int, val width: Int, val height: Int)

    /**
     * Crop box after removing the given percentages from each edge. Always leaves at least 1x1 pixel,
     * and never lets opposite edges cross.
     */
    fun cropBox(width: Int, height: Int, leftPct: Float, topPct: Float, rightPct: Float, bottomPct: Float): CropBox {
        require(width > 0 && height > 0) { "Image has no size." }
        val l = (width * leftPct / 100f).roundToInt().coerceIn(0, width - 1)
        val t = (height * topPct / 100f).roundToInt().coerceIn(0, height - 1)
        val r = (width * rightPct / 100f).roundToInt().coerceIn(0, width - 1)
        val b = (height * bottomPct / 100f).roundToInt().coerceIn(0, height - 1)
        val w = (width - l - r).coerceAtLeast(1)
        val h = (height - t - b).coerceAtLeast(1)
        return CropBox(l.coerceAtMost(width - w), t.coerceAtMost(height - h), w, h)
    }

    /** Size after scaling both sides by [percent] (1..400). Never returns less than 1 pixel per side. */
    fun scaledSize(width: Int, height: Int, percent: Int): Pair<Int, Int> {
        val p = percent.coerceIn(1, 400) / 100.0
        return Pair((width * p).roundToInt().coerceAtLeast(1), (height * p).roundToInt().coerceAtLeast(1))
    }

    /** Suggested output name: "photo.jpg" -> "photo-edited.png". */
    fun outputName(original: String?, extension: String): String {
        val base = original?.substringBeforeLast('.', original)?.takeIf { it.isNotBlank() } ?: "image"
        return "$base-edited.$extension"
    }
}
