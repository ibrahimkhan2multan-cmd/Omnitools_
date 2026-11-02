package com.omnitools.app.tools.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/**
 * An open PDF. Wraps PdfRenderer, which allows only one page to be open at a time and is not thread-safe,
 * so every call that touches it is synchronized.
 */
class PdfSession private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : AutoCloseable {

    val pageCount: Int = renderer.pageCount

    /** Page size in PDF points (1/72 inch). */
    @Synchronized
    fun pageSize(index: Int): Pair<Int, Int> {
        renderer.openPage(index).use { return it.width to it.height }
    }

    /**
     * Renders page [index] at [targetWidthPx] wide (capped, and reduced further when the page is very tall, so a
     * single bitmap never grows beyond about 12 million pixels).
     */
    @Synchronized
    fun render(index: Int, targetWidthPx: Int): Bitmap {
        renderer.openPage(index).use { page ->
            var w = targetWidthPx.coerceIn(100, MAX_WIDTH_PX)
            var h = (w.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
            if (w.toLong() * h > MAX_PIXELS) {
                val shrink = Math.sqrt(MAX_PIXELS.toDouble() / (w.toLong() * h))
                w = (w * shrink).roundToInt().coerceAtLeast(1)
                h = (h * shrink).roundToInt().coerceAtLeast(1)
            }
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE) // PdfRenderer draws onto the bitmap as-is; most pages assume a white sheet.
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp
        }
    }

    @Synchronized
    override fun close() {
        try { renderer.close() } catch (e: Exception) { /* already closed */ }
        try { pfd.close() } catch (e: Exception) { /* already closed */ }
    }

    class OpenException(message: String) : Exception(message)

    companion object {
        const val MAX_WIDTH_PX = 2400
        const val MAX_PIXELS = 12_000_000L

        /**
         * Opens [uri]. Providers that cannot give a seekable file descriptor are handled by copying the file to
         * the app cache first. Throws [OpenException] with a message that is safe to show.
         */
        fun open(context: Context, uri: Uri): PdfSession {
            try {
                return fromDescriptor(
                    context.contentResolver.openFileDescriptor(uri, "r") ?: throw OpenException("Could not open the file."),
                )
            } catch (e: OpenException) {
                throw e
            } catch (e: SecurityException) {
                throw OpenException("This PDF is password protected or restricted, which the built-in viewer cannot open.")
            } catch (e: IOException) {
                // Not seekable or not a PDF: retry once through a cached copy before giving up.
            } catch (e: Exception) {
                // Fall through to the copy path below.
            }
            val copy = File(context.cacheDir, "open.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use { input -> copy.outputStream().use { input.copyTo(it) } }
                    ?: throw OpenException("Could not open the file.")
                return fromDescriptor(ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY))
            } catch (e: OpenException) {
                throw e
            } catch (e: SecurityException) {
                throw OpenException("This PDF is password protected or restricted, which the built-in viewer cannot open.")
            } catch (e: Exception) {
                throw OpenException("This file is not a valid PDF, or it is damaged.")
            }
        }

        private fun fromDescriptor(pfd: ParcelFileDescriptor): PdfSession {
            return try {
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    pfd.close()
                    throw OpenException("This PDF has no pages.")
                }
                PdfSession(pfd, renderer)
            } catch (e: OpenException) {
                throw e
            } catch (e: Exception) {
                try { pfd.close() } catch (_: Exception) {}
                throw e
            }
        }
    }
}
