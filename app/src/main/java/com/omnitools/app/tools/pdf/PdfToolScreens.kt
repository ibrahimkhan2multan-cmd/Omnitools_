package com.omnitools.app.tools.pdf

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.Choice
import com.omnitools.app.ui.components.ChipRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.OmniCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RASTER_NOTE = "Pages are copied as pictures, so text in the new file cannot be selected or searched. Your original files are not changed."

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

private class PdfPick(val uri: Uri, val name: String, val pages: Int)

private fun inspect(context: android.content.Context, uri: Uri): PdfPick? = try {
    PdfSession.open(context, uri).use { PdfPick(uri, FileInfo.query(context, uri).name ?: "PDF", it.pageCount) }
} catch (e: Exception) { null }

@Composable
fun MergePdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PdfPick>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0) }
    val total = items.sumOf { it.pages }
    val add = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if (picked.isNotEmpty()) scope.launch {
            busy = true; error = null
            val found = withContext(Dispatchers.IO) { picked.filter { u -> items.none { it.uri == u } }.map { it to inspect(context, it) } }
            val bad = found.count { it.second == null }
            items = items + found.mapNotNull { it.second }
            if (bad > 0) error = "$bad file${if (bad == 1) " was" else "s were"} not added because they are not readable PDFs."
            busy = false
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { dest ->
        if (dest != null) scope.launch {
            busy = true; error = null; progress = 0
            try {
                val n = withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(dest, "w") ?: throw PdfSession.OpenException("Could not open the destination file.")
                    out.use { PdfRebuild.write(context, items.map { PdfRebuild.Part(it.uri, null) }, it) { p -> progress = p } }
                }
                Toast.makeText(context, "Saved $n pages", Toast.LENGTH_SHORT).show()
            } catch (e: PdfSession.OpenException) { error = e.message
            } catch (e: Exception) { error = "Could not merge: ${e.message ?: "unknown error"}"
            } catch (e: OutOfMemoryError) { error = "Not enough memory for these files." }
            busy = false
        }
    }
    ScreenScaffold(title = "Merge PDFs", onBack = onBack) { padding ->
        ToolPage(padding) {
            Button(onClick = { add.launch(arrayOf("application/pdf")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (items.isEmpty()) "Choose PDFs" else "Add more PDFs") }
            ErrorText(error)
            items.forEachIndexed { i, it ->
                OmniCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text("${i + 1}. ${it.name}", style = MaterialTheme.typography.titleSmall)
                            Text("${it.pages} page${if (it.pages == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(enabled = i > 0 && !busy, onClick = { items = items.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) } }) { Text("Up") }
                        TextButton(enabled = i < items.lastIndex && !busy, onClick = { items = items.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) } }) { Text("Down") }
                        TextButton(enabled = !busy, onClick = { items = items.filterIndexed { k, _ -> k != i } }) { Text("Remove") }
                    }
                }
            }
            if (items.size >= 2) {
                Text("$total pages in total", style = MaterialTheme.typography.titleSmall)
                if (total > PdfRebuild.MAX_PAGES) ErrorText("Too many pages: the limit is ${PdfRebuild.MAX_PAGES}.")
                Button(onClick = { save.launch("merged.pdf") }, enabled = !busy && total <= PdfRebuild.MAX_PAGES, modifier = Modifier.fillMaxWidth()) { Text("Merge and save") }
            }
            if (busy) { CircularProgressIndicator(); if (progress > 0) Text("Page $progress of $total") }
            Note(RASTER_NOTE)
        }
    }
}

@Composable
fun ExtractPagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pick by remember { mutableStateOf<PdfPick?>(null) }
    var range by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) scope.launch {
            busy = true; error = null
            val p = withContext(Dispatchers.IO) { inspect(context, u) }
            if (p == null) error = "That is not a readable PDF." else { pick = p; range = "" }
            busy = false
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { dest ->
        val p = pick
        if (dest != null && p != null) scope.launch {
            busy = true; error = null
            try {
                val pages = PdfRange.parse(range, p.pages)
                val n = withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(dest, "w") ?: throw PdfSession.OpenException("Could not open the destination file.")
                    out.use { PdfRebuild.write(context, listOf(PdfRebuild.Part(p.uri, pages)), it) }
                }
                Toast.makeText(context, "Saved $n pages", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) { error = e.message
            } catch (e: PdfSession.OpenException) { error = e.message
            } catch (e: Exception) { error = "Could not save: ${e.message ?: "unknown error"}"
            } catch (e: OutOfMemoryError) { error = "Not enough memory for this file." }
            busy = false
        }
    }
    ScreenScaffold(title = "Extract PDF Pages", onBack = onBack) { padding ->
        ToolPage(padding) {
            Button(onClick = { open.launch(arrayOf("application/pdf")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (pick == null) "Choose a PDF" else "Choose another PDF") }
            ErrorText(error)
            pick?.let { p ->
                Text("${p.name}: ${p.pages} page${if (p.pages == 1) "" else "s"}", style = MaterialTheme.typography.titleSmall)
                TextInput("Pages to keep (for example 1-3, 5, 8-)", range, { range = it.filter { c -> c.isDigit() || c in ",- " }.take(100) })
                Button(onClick = {
                    try { PdfRange.parse(range, p.pages); error = null; save.launch("extracted.pdf") } catch (e: IllegalArgumentException) { error = e.message }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save selected pages") }
            }
            if (busy) CircularProgressIndicator()
            Note("Use it to split a PDF, delete pages (keep the others) or reorder (for example 3, 1, 2). $RASTER_NOTE")
        }
    }
}

@Composable
fun PdfToImagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pick by remember { mutableStateOf<PdfPick?>(null) }
    var range by remember { mutableStateOf("") }
    var jpeg by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) scope.launch {
            busy = true; error = null
            val p = withContext(Dispatchers.IO) { inspect(context, u) }
            if (p == null) error = "That is not a readable PDF." else { pick = p; range = "" }
            busy = false
        }
    }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        val p = pick
        if (tree != null && p != null) scope.launch {
            busy = true; error = null; done = 0
            try {
                val pages = if (range.isBlank()) (0 until p.pages).toList() else PdfRange.parse(range, p.pages)
                require(pages.size <= 100) { "At most 100 pages at a time. Choose a smaller range." }
                val n = withContext(Dispatchers.IO) { PdfRebuild.exportImages(context, p.uri, tree, pages, jpeg) { d -> done = d } }
                Toast.makeText(context, "Saved $n image${if (n == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) { error = e.message
            } catch (e: PdfSession.OpenException) { error = e.message
            } catch (e: Exception) { error = "Could not save: ${e.message ?: "unknown error"}"
            } catch (e: OutOfMemoryError) { error = "Not enough memory for this file." }
            busy = false
        }
    }
    ScreenScaffold(title = "PDF to Images", onBack = onBack) { padding ->
        ToolPage(padding) {
            Button(onClick = { open.launch(arrayOf("application/pdf")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (pick == null) "Choose a PDF" else "Choose another PDF") }
            ErrorText(error)
            pick?.let { p ->
                Text("${p.name}: ${p.pages} page${if (p.pages == 1) "" else "s"}", style = MaterialTheme.typography.titleSmall)
                TextInput("Pages (leave empty for all)", range, { range = it.filter { c -> c.isDigit() || c in ",- " }.take(100) })
                ChipRow { Choice(!jpeg, "PNG (sharp)") { jpeg = false }; Choice(jpeg, "JPEG (smaller)") { jpeg = true } }
                Button(onClick = { folder.launch(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose folder and save") }
            }
            if (busy) { CircularProgressIndicator(); if (done > 0) Text("Saved $done") }
            Note("Each page becomes its own picture in the folder you choose, named page-1, page-2 and so on. Your original PDF is not changed.")
        }
    }
}
