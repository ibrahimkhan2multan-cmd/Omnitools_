package com.omnitools.app.tools.pdf

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.util.BitmapLoader
import com.omnitools.app.util.FileInfo
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImagesToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var options by remember { mutableStateOf(PdfBuilder.Options()) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if (picked.isNotEmpty()) {
            val room = PdfLayout.MAX_PAGES - items.size
            items = items + picked.filterNot { it in items }.take(room.coerceAtLeast(0))
            error = if (picked.size > room) "Only ${PdfLayout.MAX_PAGES} pictures fit in one PDF; the extra ones were not added." else null
            success = null
        }
    }

    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { dest ->
        if (dest != null) {
            val snapshot = items
            val opts = options
            error = null
            success = null
            progress = 0 to snapshot.size
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val out = context.contentResolver.openOutputStream(dest, "w")
                            ?: throw PdfBuilder.BuildException("Could not open the destination file.")
                        out.use { PdfBuilder.write(context, snapshot, opts, it) { done, total -> progress = done to total } }
                    }
                    success = "Saved ${result.pages} page${if (result.pages == 1) "" else "s"} (${Format.bytes(result.bytesWritten)})" +
                        if (result.skipped > 0) ". ${result.skipped} picture${if (result.skipped == 1) "" else "s"} could not be read and ${if (result.skipped == 1) "was" else "were"} skipped." else "."
                } catch (e: PdfBuilder.BuildException) {
                    error = e.message
                } catch (e: java.io.IOException) {
                    error = "Could not write the PDF: ${e.message ?: "unknown error"}"
                }
                progress = null
            }
        }
    }

    ScreenScaffold(title = "Images to PDF", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { addPicker.launch(arrayOf("image/*")) },
                enabled = progress == null && items.size < PdfLayout.MAX_PAGES,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (items.isEmpty()) "Choose pictures" else "Add more pictures") }

            if (items.isEmpty()) {
                Text(
                    "Pick one or more pictures, put them in order, choose a page size and save one PDF. " +
                        "Your pictures are not changed and nothing is uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items.forEachIndexed { index, uri ->
                PictureRow(
                    uri = uri,
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < items.lastIndex,
                    enabled = progress == null,
                    onUp = { items = items.toMutableList().also { val t = it.removeAt(index); it.add(index - 1, t) } },
                    onDown = { items = items.toMutableList().also { val t = it.removeAt(index); it.add(index + 1, t) } },
                    onRemove = { items = items.toMutableList().also { it.removeAt(index) } },
                )
            }

            if (items.isNotEmpty()) {
                SectionTitle("Page")
                Text("Page size", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfLayout.PageSize.values().forEach { s ->
                        FilterChip(selected = options.size == s, onClick = { options = options.copy(size = s) }, label = { Text(s.label) })
                    }
                }
                if (options.size != PdfLayout.PageSize.FIT) {
                    Text("Orientation", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfLayout.Orientation.values().forEach { o ->
                            FilterChip(selected = options.orientation == o, onClick = { options = options.copy(orientation = o) }, label = { Text(o.label) })
                        }
                    }
                }
                Text("Margin", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfLayout.Margin.values().forEach { m ->
                        FilterChip(selected = options.margin == m, onClick = { options = options.copy(margin = m) }, label = { Text(m.label) })
                    }
                }
                Text("Picture quality", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfLayout.Quality.values().forEach { q ->
                        FilterChip(selected = options.quality == q, onClick = { options = options.copy(quality = q) }, label = { Text(q.label) })
                    }
                }
                Text(
                    "Each picture is reduced to at most ${options.quality.maxSidePx} px on its longest side. " +
                        "Android's built-in PDF writer stores pictures uncompressed-ish, so PDFs from photos can be large; " +
                        "\"Small file\" helps, and so does a smaller picture count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { savePicker.launch("images.pdf") },
                    enabled = progress == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Create PDF (${items.size} picture${if (items.size == 1) "" else "s"})") }
                OutlinedButton(
                    onClick = { items = emptyList(); success = null; error = null },
                    enabled = progress == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Clear list") }
            }

            progress?.let { (done, total) ->
                LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                Text("Processing picture $done of $total…", style = MaterialTheme.typography.bodySmall)
            }
            ErrorText(error)
            success?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleSmall) }
        }
    }
}

@Composable
private fun PictureRow(
    uri: Uri,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    enabled: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    // Small thumbnail, decoded off the main thread; null while loading or if the file cannot be read.
    val thumb by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { BitmapLoader.decode(context, uri, 160) }
    }
    val name by produceState<String?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { FileInfo.query(context, uri).name }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                thumb?.let {
                    Image(it.asImageBitmap(), contentDescription = "Thumbnail of picture $position", contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
                }
            }
            Text(
                "$position. ${name ?: "Picture"}",
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onUp, enabled = enabled && canMoveUp) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up") }
            IconButton(onClick = onDown, enabled = enabled && canMoveDown) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down") }
            IconButton(onClick = onRemove, enabled = enabled) { Icon(Icons.Filled.Close, contentDescription = "Remove picture") }
        }
    }
}
