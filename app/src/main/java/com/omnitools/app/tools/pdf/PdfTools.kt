package com.omnitools.app.tools.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import java.io.OutputStream

/** Page ranges such as "1-3, 5, 8-" (1-based, inclusive; a trailing dash means "to the end"). */
object PdfRange {
    fun parse(text: String, pageCount: Int): List<Int> {
        require(pageCount > 0) { "The PDF has no pages." }
        val parts = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        require(parts.isNotEmpty()) { "Type the pages you want, like 1-3, 5." }
        val out = LinkedHashSet<Int>()
        for (p in parts) {
            val bits = p.split('-').map { it.trim() }
            val from: Int; val to: Int
            when {
                bits.size == 1 -> { from = num(bits[0], p); to = from }
                bits.size == 2 -> {
                    from = if (bits[0].isEmpty()) 1 else num(bits[0], p)
                    to = if (bits[1].isEmpty()) pageCount else num(bits[1], p)
                }
                else -> throw IllegalArgumentException("\"$p\" is not a valid page range.")
            }
            require(from >= 1 && to <= pageCount) { "Pages must be between 1 and $pageCount (you typed \"$p\")." }
            require(from <= to) { "\"$p\" goes backwards. Write it as $to-$from." }
            for (i in from..to) out.add(i - 1)
        }
        return out.toList()
    }

    private fun num(s: String, whole: String): Int = s.toIntOrNull() ?: throw IllegalArgumentException("\"$whole\" is not a valid page range.")
}

/** Rebuilds PDFs by drawing each source page as a picture. Text in the result is no longer selectable. */
object PdfRebuild {
    const val MAX_PAGES = 60
    const val RENDER_WIDTH = 1000

    /** [pages] null means every page. */
    class Part(val uri: Uri, val pages: List<Int>?)

    fun write(context: Context, parts: List<Part>, out: OutputStream, onProgress: (Int) -> Unit = {}): Int {
        val doc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        var written = 0
        try {
            for (part in parts) {
                PdfSession.open(context, part.uri).use { s ->
                    val pages = part.pages ?: (0 until s.pageCount).toList()
                    for (index in pages) {
                        if (written >= MAX_PAGES) throw PdfSession.OpenException("At most $MAX_PAGES pages fit in one result.")
                        val (w, h) = s.pageSize(index)
                        val bmp = s.render(index, RENDER_WIDTH)
                        val page = doc.startPage(PdfDocument.PageInfo.Builder(w.coerceAtLeast(1), h.coerceAtLeast(1), written + 1).create())
                        page.canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
                        doc.finishPage(page)
                        bmp.recycle()
                        written++
                        onProgress(written)
                    }
                }
            }
            if (written == 0) throw PdfSession.OpenException("There were no pages to save.")
            doc.writeTo(out)
        } finally { doc.close() }
        return written
    }

    /** Saves page images into [treeUri] as page-1.png, page-2.png ... Returns how many were written. */
    fun exportImages(context: Context, pdf: Uri, treeUri: Uri, pages: List<Int>, jpeg: Boolean, onProgress: (Int) -> Unit = {}): Int {
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        var done = 0
        PdfSession.open(context, pdf).use { s ->
            for (index in pages) {
                val bmp = s.render(index, 1600)
                val name = "page-${index + 1}"
                val doc = DocumentsContract.createDocument(context.contentResolver, parent, if (jpeg) "image/jpeg" else "image/png", name)
                    ?: throw PdfSession.OpenException("Could not create a file in that folder.")
                context.contentResolver.openOutputStream(doc, "w")?.use {
                    val ok = bmp.compress(if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 92, it)
                    if (!ok) throw PdfSession.OpenException("A page could not be saved.")
                } ?: throw PdfSession.OpenException("Could not write to that folder.")
                bmp.recycle()
                done++; onProgress(done)
            }
        }
        return done
    }
}
