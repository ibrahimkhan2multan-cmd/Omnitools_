package com.omnitools.app.tools.qr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Turns a list of lines into a printable PDF page grid of QR codes. */
object QrSheet {
    const val MAX_CODES = 60
    const val MAX_LINE = 300
    const val COLUMNS = 3
    const val ROWS = 4
    const val PER_PAGE = COLUMNS * ROWS

    /** One code per non-empty line. Throws when there is nothing to print or too much. */
    fun parse(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.isNotEmpty()) { "Type one item per line, for example a link or a name." }
        require(lines.size <= MAX_CODES) { "At most $MAX_CODES codes per sheet (you have ${lines.size})." }
        require(lines.all { it.length <= MAX_LINE }) { "Each line can be up to $MAX_LINE characters." }
        return lines
    }

    fun pageCount(codes: Int) = (codes + PER_PAGE - 1) / PER_PAGE

    fun write(items: List<String>, out: java.io.OutputStream) {
        val doc = PdfDocument()
        val pageW = 595; val pageH = 842; val margin = 36f
        val cellW = (pageW - 2 * margin) / COLUMNS; val cellH = (pageH - 2 * margin) / ROWS
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9f; textAlign = Paint.Align.CENTER }
        val frame = Paint().apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 0.5f }
        try {
            for (pageIndex in 0 until pageCount(items.size)) {
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex + 1).create())
                val c: Canvas = page.canvas
                c.drawColor(Color.WHITE)
                for (k in 0 until PER_PAGE) {
                    val idx = pageIndex * PER_PAGE + k
                    if (idx >= items.size) break
                    val left = margin + (k % COLUMNS) * cellW; val top = margin + (k / COLUMNS) * cellH
                    val side = minOf(cellW, cellH) - 40f
                    val bmp = QrEncoder.bitmap(items[idx], 400)
                    val x = left + (cellW - side) / 2
                    c.drawBitmap(bmp, null, RectF(x, top + 8f, x + side, top + 8f + side), null)
                    bmp.recycle()
                    c.drawRect(left + 4f, top + 4f, left + cellW - 4f, top + cellH - 4f, frame)
                    val text = if (items[idx].length > 34) items[idx].take(33) + "…" else items[idx]
                    c.drawText(text, left + cellW / 2, top + side + 26f, label)
                }
                doc.finishPage(page)
            }
            doc.writeTo(out)
        } finally { doc.close() }
    }
}

@Composable
fun QrSheetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { dest: Uri? ->
        if (dest != null) {
            val items = try { QrSheet.parse(text) } catch (e: IllegalArgumentException) { error = e.message; return@rememberLauncherForActivityResult }
            busy = true; error = null
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try { context.contentResolver.openOutputStream(dest, "w")?.use { QrSheet.write(items, it) } != null }
                    catch (e: Exception) { false }
                }
                busy = false
                Toast.makeText(context, if (ok) "Sheet saved (${QrSheet.pageCount(items.size)} page${if (QrSheet.pageCount(items.size) == 1) "" else "s"})" else "Could not save the sheet", Toast.LENGTH_SHORT).show()
            }
        }
    }
    ScreenScaffold(title = "QR Code Sheet", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("One item per line (links, names, codes)", text, { text = it.take(30_000) }, singleLine = false, minLines = 8)
            ErrorText(error)
            Button(onClick = {
                try { QrSheet.parse(text); error = null; save.launch("qr-sheet.pdf") } catch (e: IllegalArgumentException) { error = e.message }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save as PDF") }
            if (busy) CircularProgressIndicator()
            Text("Makes a printable A4 sheet with ${QrSheet.PER_PAGE} codes per page, each labelled. Handy for labels, tickets, asset tags or Wi-Fi cards. Works offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
