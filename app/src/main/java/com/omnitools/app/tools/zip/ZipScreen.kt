package com.omnitools.app.tools.zip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.FileInfo
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val SHOWN_ENTRIES = 200
private const val MAX_FILES_TO_ZIP = 200

@Composable
fun ZipScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    ScreenScaffold(title = "ZIP Manager", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Open & extract") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Create ZIP") })
            }
            if (tab == 0) ExtractTab() else CreateTab()
        }
    }
}

@Composable
private fun ExtractTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cacheCopy = remember { File(context.cacheDir, "zip_open.zip") }
    var name by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<ZipEngine.Entry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    // The ZIP is copied to the app's private cache so entries can be read at random; remove it when leaving.
    DisposableEffect(Unit) { onDispose { cacheCopy.delete() } }

    val openPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                error = null
                success = null
                try {
                    val (displayName, list) = withContext(Dispatchers.IO) {
                        val details = FileInfo.query(context, uri)
                        context.contentResolver.openInputStream(uri)?.use { input -> cacheCopy.outputStream().use { input.copyTo(it) } }
                            ?: throw java.io.IOException("Could not open the file.")
                        (details.name ?: "archive.zip") to ZipEngine.list(cacheCopy)
                    }
                    name = displayName
                    entries = list
                } catch (e: ZipEngine.ZipLimitException) {
                    error = e.message
                } catch (e: java.util.zip.ZipException) {
                    name = null
                    entries = emptyList()
                    error = if (e.message?.contains("path", ignoreCase = true) == true) {
                        "This ZIP contains unsafe file paths, so Android refused to open it. Nothing was extracted."
                    } else {
                        "This file is not a valid ZIP, or it is damaged."
                    }
                } catch (e: Exception) {
                    name = null
                    entries = emptyList()
                    error = "This file is not a valid ZIP, or it is damaged."
                }
                busy = false
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree: Uri? ->
        if (tree != null) {
            scope.launch {
                busy = true
                error = null
                success = null
                progress = 0 to entries.size
                try {
                    val result = withContext(Dispatchers.IO) {
                        ZipEngine.extract(cacheCopy, SafSink(context.contentResolver, tree), onProgress = { d, t -> progress = d to t })
                    }
                    success = "Extracted ${result.files} file${if (result.files == 1) "" else "s"} (${Format.bytes(result.bytes)})" +
                        (if (result.skippedUnsafe > 0) ". Skipped ${result.skippedUnsafe} unsafe entr${if (result.skippedUnsafe == 1) "y" else "ies"}." else ".")
                } catch (e: ZipEngine.ZipLimitException) {
                    error = e.message
                } catch (e: java.util.zip.ZipException) {
                    error = "This ZIP could not be read: ${e.message ?: "it may be damaged"}"
                } catch (e: java.io.IOException) {
                    error = "Could not write to that folder: ${e.message ?: "unknown error"}"
                } catch (e: Exception) {
                    error = "Extraction failed: ${e.message ?: "unknown error"}"
                }
                progress = null
                busy = false
            }
        }
    }

    Button(onClick = { openPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(if (name == null) "Choose a ZIP file" else "Choose another ZIP")
    }
    if (name == null && !busy) {
        Text(
            "Open a ZIP to see what is inside, then extract it into a folder you choose. Entries with unsafe paths " +
                "(such as ../ tricks) are never written. Password-protected ZIPs are not supported.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ErrorText(error)

    if (name != null) {
        val files = entries.count { !it.isDirectory }
        val unsafe = entries.count { !it.safe }
        val totalSize = entries.filter { !it.isDirectory && it.size >= 0 }.sumOf { it.size }
        Text("$name", style = MaterialTheme.typography.titleSmall)
        Text(
            "$files file${if (files == 1) "" else "s"} · ${Format.bytes(totalSize)} when extracted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (unsafe > 0) {
            Text("$unsafe entr${if (unsafe == 1) "y has an" else "ies have"} unsafe path${if (unsafe == 1) "" else "s"} and will be skipped.", color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { folderPicker.launch(null) }, enabled = !busy && files + entries.count { it.isDirectory } > 0, modifier = Modifier.fillMaxWidth()) {
            Text("Extract all to a folder…")
        }
        progress?.let { (d, t) ->
            LinearProgressIndicator(progress = { if (t == 0) 0f else d.toFloat() / t }, modifier = Modifier.fillMaxWidth())
        }
        success?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleSmall) }

        SectionTitle("Contents")
        entries.take(SHOWN_ENTRIES).forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(if (e.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(e.name, modifier = Modifier.weight(1f), color = if (e.safe) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                if (!e.isDirectory && e.size >= 0) Text(Format.bytes(e.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (entries.size > SHOWN_ENTRIES) {
            Text("…and ${entries.size - SHOWN_ENTRIES} more. Extract all includes them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var names by remember { mutableStateOf<Map<Uri, String>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        if (picked.isNotEmpty()) {
            scope.launch {
                val room = MAX_FILES_TO_ZIP - files.size
                val added = picked.filterNot { it in files }.take(room.coerceAtLeast(0))
                val newNames = withContext(Dispatchers.IO) { added.associateWith { FileInfo.query(context, it).name ?: "file" } }
                files = files + added
                names = names + newNames
                error = if (picked.size > room) "At most $MAX_FILES_TO_ZIP files fit in one ZIP; the extra ones were not added." else null
                success = null
            }
        }
    }

    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { dest: Uri? ->
        if (dest != null) {
            val snapshot = files
            val labels = names
            scope.launch {
                busy = true
                error = null
                success = null
                progress = 0 to snapshot.size
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        val inputs = snapshot.map { uri ->
                            ZipEngine.Input(labels[uri] ?: "file") {
                                context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Could not read ${labels[uri] ?: "a file"}.")
                            }
                        }
                        val out = context.contentResolver.openOutputStream(dest, "w") ?: throw java.io.IOException("Could not write the ZIP.")
                        ZipEngine.create(inputs, out) { d, t -> progress = d to t }
                        FileInfo.query(context, dest).sizeBytes
                    }
                    success = "ZIP saved with ${snapshot.size} file${if (snapshot.size == 1) "" else "s"}" + (bytes?.let { " (${Format.bytes(it)})" } ?: "") + "."
                } catch (e: java.io.IOException) {
                    error = e.message ?: "Could not create the ZIP."
                } catch (e: Exception) {
                    error = "Could not create the ZIP: ${e.message ?: "unknown error"}"
                }
                progress = null
                busy = false
            }
        }
    }

    Button(onClick = { addPicker.launch(arrayOf("*/*")) }, enabled = !busy && files.size < MAX_FILES_TO_ZIP, modifier = Modifier.fillMaxWidth()) {
        Text(if (files.isEmpty()) "Choose files to zip" else "Add more files")
    }
    if (files.isEmpty()) {
        Text(
            "Pick the files you want, then save one ZIP. Your files are not changed. The system picker chooses files, " +
                "not whole folders, so zipping a folder means selecting the files inside it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    files.forEach { uri ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(names[uri] ?: "file", modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            IconButton(onClick = { files = files - uri }, enabled = !busy) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
        }
    }
    if (files.isNotEmpty()) {
        Button(onClick = { savePicker.launch("archive.zip") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Create ZIP (${files.size} file${if (files.size == 1) "" else "s"})")
        }
        OutlinedButton(onClick = { files = emptyList(); success = null; error = null }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Clear list") }
    }
    progress?.let { (d, t) ->
        LinearProgressIndicator(progress = { if (t == 0) 0f else d.toFloat() / t }, modifier = Modifier.fillMaxWidth())
    }
    ErrorText(error)
    success?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleSmall) }
}
