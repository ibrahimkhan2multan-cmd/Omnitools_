package com.omnitools.app.tools.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.legere.pdfiumandroid.PdfDocument as PdfiumDocument
import io.legere.pdfiumandroid.PdfPasswordException
import io.legere.pdfiumandroid.PdfiumCore
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/**
 * An open PDF. The main engine is PDFium (the one Chrome uses): it draws pictures, transparency, odd fonts and
 * damaged files that Android's own renderer gives up on, and it opens password-protected files. If PDFium cannot
 * start on a phone, Android's PdfRenderer is used instead. Both are single-threaded, so every call is synchronized.
 */
class PdfSession private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer?,
    private val doc: PdfiumDocument?,
) : AutoCloseable {

    val pageCount: Int = doc?.getPageCount() ?: renderer!!.pageCount

    /** Which engine drew this file, for the details line. */
    val engineName: String get() = if (doc != null) "PDFium" else "Android"

    /** Page size in PDF points (1/72 inch). */
    @Synchronized
    fun pageSize(index: Int): Pair<Int, Int> {
        if (doc != null) {
            doc.openPage(index).use { return it.getPageWidthPoint().coerceAtLeast(1) to it.getPageHeightPoint().coerceAtLeast(1) }
        }
        renderer!!.openPage(index).use { return it.width to it.height }
    }

    /**
     * Renders page [index] at [targetWidthPx] wide (capped, and reduced further when the page is very tall, so a
     * single bitmap never grows beyond about 12 million pixels).
     */
    @Synchronized
    fun render(index: Int, targetWidthPx: Int): Bitmap {
        val (pw, ph) = pageSize(index)
        val (w, h) = fit(pw, ph, targetWidthPx)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE) // most pages assume a white sheet
        if (doc != null) {
            doc.openPage(index).use { it.renderPageBitmap(bmp, 0, 0, w, h, true) }
        } else {
            renderer!!.openPage(index).use { it.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
        }
        return bmp
    }

    @Synchronized
    override fun close() {
        try { doc?.close() } catch (e: Throwable) { /* already closed */ }
        try { renderer?.close() } catch (e: Exception) { /* already closed */ }
        try { pfd.close() } catch (e: Exception) { /* already closed */ }
    }

    open class OpenException(message: String) : Exception(message)

    /** The file is locked. Ask for the password, then open again with it. */
    class PasswordRequired(val wrong: Boolean) : OpenException(if (wrong) "That password is not correct." else "This PDF is password protected.")

    companion object {
        const val MAX_WIDTH_PX = 2400
        const val MAX_PIXELS = 12_000_000L

        /** Bitmap size for a page of [pw] x [ph] points drawn [targetWidthPx] wide, within the size limits. */
        fun fit(pw: Int, ph: Int, targetWidthPx: Int): Pair<Int, Int> {
            val pageW = pw.coerceAtLeast(1)
            val pageH = ph.coerceAtLeast(1)
            var w = targetWidthPx.coerceIn(100, MAX_WIDTH_PX)
            var h = (w.toLong() * pageH / pageW).toInt().coerceAtLeast(1)
            if (w.toLong() * h > MAX_PIXELS) {
                val shrink = Math.sqrt(MAX_PIXELS.toDouble() / (w.toLong() * h))
                w = (w * shrink).roundToInt().coerceAtLeast(1)
                h = (h * shrink).roundToInt().coerceAtLeast(1)
            }
            return w to h
        }

        @Volatile private var core: PdfiumCore? = null
        @Volatile private var coreFailed = false

        /** PDFium, started once. Null if its native library cannot load on this phone. */
        @Synchronized
        private fun pdfium(context: Context): PdfiumCore? {
            if (core == null && !coreFailed) {
                try { core = PdfiumCore(context.applicationContext) } catch (t: Throwable) { coreFailed = true }
            }
            return core
        }

        private fun looksLikePassword(e: Throwable): Boolean =
            e is PdfPasswordException || e.message?.contains("password", ignoreCase = true) == true

        /**
         * Opens [uri], with [password] if the file is locked. Providers that cannot give a seekable file descriptor are
         * handled by copying the file to the app cache first. Throws [OpenException] with a message that is safe to show,
         * or [PasswordRequired] when a password is needed.
         */
        fun open(context: Context, uri: Uri, password: String? = null): PdfSession {
            val engine = pdfium(context)
            var failure: Throwable? = null

            // 1. PDFium on the file itself, then on a cached copy (some providers hand out descriptors that cannot seek).
            for (attempt in 0..1) {
                val fd = try { descriptor(context, uri, copy = attempt == 1) } catch (e: OpenException) { throw e } catch (e: Exception) { failure = e; continue }
                if (engine != null) {
                    try {
                        val d = engine.newDocument(fd, password?.takeIf { it.isNotEmpty() })
                        if (d.getPageCount() == 0) { d.close(); fd.close(); throw OpenException("This PDF has no pages.") }
                        return PdfSession(fd, null, d)
                    } catch (e: OpenException) {
                        throw e
                    } catch (e: Throwable) {
                        try { fd.close() } catch (_: Exception) {}
                        if (looksLikePassword(e)) throw PasswordRequired(wrong = !password.isNullOrEmpty())
                        failure = e
                    }
                }
            }

            // 2. Android's own renderer, as a last resort (also used when PDFium could not start).
            for (attempt in 0..1) {
                try {
                    val fd = descriptor(context, uri, copy = attempt == 1)
                    try {
                        val r = PdfRenderer(fd)
                        if (r.pageCount == 0) { r.close(); fd.close(); throw OpenException("This PDF has no pages.") }
                        return PdfSession(fd, r, null)
                    } catch (e: OpenException) {
                        throw e
                    } catch (e: SecurityException) {
                        try { fd.close() } catch (_: Exception) {}
                        throw PasswordRequired(wrong = !password.isNullOrEmpty())
                    } catch (e: Throwable) {
                        try { fd.close() } catch (_: Exception) {}
                        failure = failure ?: e
                    }
                } catch (e: OpenException) {
                    throw e
                } catch (e: Exception) {
                    failure = failure ?: e
                }
            }
            throw OpenException("This file is not a valid PDF, or it is damaged.")
        }

        private fun descriptor(context: Context, uri: Uri, copy: Boolean): ParcelFileDescriptor {
            if (!copy) {
                try {
                    return context.contentResolver.openFileDescriptor(uri, "r") ?: throw OpenException("Could not open the file.")
                } catch (e: OpenException) { throw e } catch (e: IOException) { /* not seekable: use the copy */ } catch (e: SecurityException) { throw OpenException("The app is not allowed to read this file.") }
            }
            val file = File(context.cacheDir, "open.pdf")
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                ?: throw OpenException("Could not open the file.")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}
