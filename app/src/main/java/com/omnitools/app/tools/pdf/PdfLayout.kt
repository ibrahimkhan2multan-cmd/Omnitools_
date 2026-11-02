package com.omnitools.app.tools.pdf

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Page maths for images-to-PDF. Units are PDF points (1/72 inch). Pure Kotlin, unit tested. */
object PdfLayout {

    enum class PageSize(val label: String, val widthPt: Int, val heightPt: Int) {
        A4("A4", 595, 842),
        LETTER("Letter", 612, 792),
        FIT("Fit to picture", 0, 0),
    }

    enum class Orientation(val label: String) { PORTRAIT("Portrait"), LANDSCAPE("Landscape"), AUTO("Auto") }

    enum class Margin(val label: String, val points: Int) { NONE("None", 0), SMALL("Small", 18), NORMAL("Normal", 36) }

    /** Longest side, in pixels, that each picture is reduced to before it goes into the PDF. */
    enum class Quality(val label: String, val maxSidePx: Int) {
        SMALL("Small file", 1000),
        STANDARD("Standard", 1600),
        HIGH("High detail", 2400),
    }

    const val FIT_LONG_SIDE_PT = 842
    const val MAX_PAGES = 50

    data class PageDims(val width: Int, val height: Int)
    data class Placement(val left: Float, val top: Float, val width: Float, val height: Float)

    fun pageDims(size: PageSize, orientation: Orientation, margin: Margin, imgW: Int, imgH: Int): PageDims {
        require(imgW > 0 && imgH > 0) { "Picture has no size." }
        if (size == PageSize.FIT) {
            val scale = FIT_LONG_SIDE_PT.toFloat() / max(imgW, imgH)
            val w = (imgW * scale).roundToInt().coerceAtLeast(1) + 2 * margin.points
            val h = (imgH * scale).roundToInt().coerceAtLeast(1) + 2 * margin.points
            return PageDims(w, h)
        }
        val landscape = when (orientation) {
            Orientation.PORTRAIT -> false
            Orientation.LANDSCAPE -> true
            Orientation.AUTO -> imgW > imgH
        }
        return if (landscape) PageDims(size.heightPt, size.widthPt) else PageDims(size.widthPt, size.heightPt)
    }

    /** Largest placement of the picture that fits inside the page margins, centered, keeping its aspect ratio. */
    fun placement(page: PageDims, margin: Margin, imgW: Int, imgH: Int): Placement {
        val availW = (page.width - 2 * margin.points).coerceAtLeast(1)
        val availH = (page.height - 2 * margin.points).coerceAtLeast(1)
        val scale = min(availW.toFloat() / imgW, availH.toFloat() / imgH)
        val w = imgW * scale
        val h = imgH * scale
        return Placement(
            left = margin.points + (availW - w) / 2f,
            top = margin.points + (availH - h) / 2f,
            width = w,
            height = h,
        )
    }
}
