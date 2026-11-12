package com.omnitools.app.tools.viewer

/** Small pure helpers for the Photo Viewer and Video Player (unit tested). */
object ViewerLogic {
    /** Power-of-two shrink factor so the longest side is at most [maxDim] (never below 1). */
    fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        if (width <= 0 || height <= 0 || maxDim <= 0) return 1
        var s = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxDim) { longest /= 2; s *= 2 }
        return s
    }

    /** "3 / 12" position label. */
    fun position(index: Int, total: Int): String = if (total <= 0) "" else "${index.coerceIn(0, total - 1) + 1} / $total"

    /** Next zoom after a double tap: zoomed in goes back to 1, otherwise zoom to 2.5x. */
    fun doubleTapScale(current: Float): Float = if (current > 1.01f) 1f else 2.5f

    /** How far the picture may be dragged at [scale] inside a box of [size] (0 when not zoomed). */
    fun maxPan(size: Float, scale: Float): Float = if (scale <= 1f) 0f else size * (scale - 1f) / 2f

    val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    fun speedLabel(speed: Float): String = (if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()) + "x"
}
