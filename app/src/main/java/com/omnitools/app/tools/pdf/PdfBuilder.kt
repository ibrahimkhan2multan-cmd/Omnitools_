package com.omnitools.app.tools.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.omnitools.app.util.BitmapLoader
import java.io.FilterOutputStream
import java.io.OutputStream

/** Builds a PDF from pictures with Android's built-in PdfDocument. Pictures are decoded one at a time. */
object PdfBuilder {

    data class Options(
        val size: PdfLayout.PageSize = PdfLayout.PageSize.A4,
        val orientation: PdfLayout.Orientation = PdfLayout.Orientation.AUTO,
        val margin: PdfLayout.Margin = PdfLayout.Margin.SMALL,
        val quality: PdfLayout.Quality = PdfLayout.Quality.STANDARD,
    )

    data class Result(val pages: Int, val skipped: Int, val bytesWritten: Long)

    class BuildException(message: String) : Exception(message)

    private class CountingStream(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
    }

    /**
     * Writes one page per readable picture to [out]. Pictures that cannot be decoded are skipped and counted.
     * [onProgress] receives (pictures handled, total). Throws [BuildException] with a readable message on failure.
     */
    fun write(
        context: Context,
        uris: List<Uri>,
        options: Options,
        out: OutputStream,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): Result {
        if (uris.isEmpty()) throw BuildException("Add at least one picture.")
        if (uris.size > PdfLayout.MAX_PAGES) throw BuildException("At most ${PdfLayout.MAX_PAGES} pictures per PDF.")

        val doc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        var pages = 0
        var skipped = 0
        try {
            uris.forEachIndexed { index, uri ->
                val bmp = BitmapLoader.decode(context, uri, options.quality.maxSidePx)
                if (bmp == null) {
                    skipped++
                } else {
                    try {
                        val dims = PdfLayout.pageDims(options.size, options.orientation, options.margin, bmp.width, bmp.height)
                        val place = PdfLayout.placement(dims, options.margin, bmp.width, bmp.height)
                        val page = doc.startPage(PdfDocument.PageInfo.Builder(dims.width, dims.height, pages + 1).create())
                        val canvas: Canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        canvas.drawBitmap(bmp, null, RectF(place.left, place.top, place.left + place.width, place.top + place.height), paint)
                        doc.finishPage(page)
                        pages++
                    } finally {
                        bmp.recycle()
                    }
                }
                onProgress(index + 1, uris.size)
            }
            if (pages == 0) throw BuildException("None of the pictures could be read.")
            val counter = CountingStream(out)
            doc.writeTo(counter)
            counter.flush()
            return Result(pages, skipped, counter.count)
        } catch (e: OutOfMemoryError) {
            throw BuildException("Not enough memory. Try fewer pictures or the \"Small file\" quality.")
        } finally {
            doc.close()
        }
    }
}
