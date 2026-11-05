package com.omnitools.app.tools.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
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
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.OmniCard
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_FILES = 200_000
private const val SHOWN = 100

@Composable
fun FolderAnalyzerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var summary by remember { mutableStateOf<FileStats.Summary?>(null) }
    var folderNote by remember { mutableStateOf<String?>(null) }
    var counting by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree: Uri? ->
        if (tree != null) {
            summary = null; error = null; counting = 0; folderNote = null
            job = scope.launch {
                try {
                    val walk = withContext(Dispatchers.IO) {
                        SafWalker.walk(context.contentResolver, tree, MAX_FILES, { !isActive }, { counting = it })
                    }
                    summary = FileStats.summarize(walk.entries)
                    folderNote = if (walk.truncated) "Stopped after $MAX_FILES files; the totals below are partial." else null
                } catch (e: Exception) {
                    error = "Could not read that folder: ${e.message ?: "unknown error"}"
                }
                job = null
            }
        }
    }

    ScreenScaffold(title = "Folder Analyzer", onBack = onBack) { padding ->
        ToolPage(padding) {
            Text(
                "Pick a folder to see how much space it uses, which types take the most room and the biggest files. Nothing is changed or uploaded.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { picker.launch(null) }, enabled = job == null, modifier = Modifier.fillMaxWidth()) { Text("Choose a folder") }
            if (job != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Scanning… $counting files found", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { job?.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            ErrorText(error)
            folderNote?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            summary?.let { s ->
                if (s.files == 0) {
                    Text("That folder has no files.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    ResultCard("Total", "${Format.bytes(s.bytes)} in ${s.files} file${if (s.files == 1) "" else "s"}")
                    SectionTitle("By type")
                    s.byType.forEach { t ->
                        OmniCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(t.label, style = MaterialTheme.typography.titleSmall)
                                    Text(Format.bytes(t.bytes), style = MaterialTheme.typography.titleSmall)
                                }
                                LinearProgressIndicator(progress = { if (s.bytes == 0L) 0f else t.bytes.toFloat() / s.bytes }, modifier = Modifier.fillMaxWidth())
                                Text("${t.files} file${if (t.files == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    SectionTitle("Biggest files")
                    s.largest.forEach { f ->
                        OmniCard {
                            Column(Modifier.padding(14.dp)) {
                                Text(f.name, style = MaterialTheme.typography.titleSmall)
                                Text(f.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Format.bytes(f.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateFinderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tree by remember { mutableStateOf<Uri?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var stage by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var groups by remember { mutableStateOf<List<Duplicates.Group>?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked: Uri? ->
        if (picked != null) {
            tree = picked; groups = null; selected = emptySet(); error = null; result = null; progress = null
            job = scope.launch {
                try {
                    stage = "Listing files…"
                    val walk = withContext(Dispatchers.IO) {
                        SafWalker.walk(context.contentResolver, picked, MAX_FILES, { !isActive }, { stage = "Listing files… $it" })
                    }
                    stage = "Comparing ${walk.entries.size} files…"
                    val found = withContext(Dispatchers.IO) {
                        Duplicates.find(
                            walk.entries,
                            hash = { e, limit -> SafWalker.hash(context.contentResolver, picked, e, limit) },
                            onProgress = { d, t -> progress = if (t == 0) null else d.toFloat() / t },
                            shouldStop = { !isActive },
                        )
                    }
                    groups = found
                } catch (e: Exception) {
                    error = "Could not scan that folder: ${e.message ?: "unknown error"}"
                }
                progress = null
                job = null
            }
        }
    }

    ScreenScaffold(title = "Duplicate Finder", onBack = onBack) { padding ->
        ToolPage(padding) {
            Text(
                "Finds files with identical contents (compared byte by byte with a checksum, not just by name) inside a folder you pick. " +
                    "Nothing is deleted unless you tick files and confirm. At least one copy of each file is always kept.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { picker.launch(null) }, enabled = job == null, modifier = Modifier.fillMaxWidth()) { Text("Choose a folder to scan") }
            if (job != null) {
                progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stage, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { job?.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
            ErrorText(error)
            result?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            groups?.let { gs ->
                if (gs.isEmpty()) {
                    Text("No duplicates found.", style = MaterialTheme.typography.titleMedium)
                } else {
                    ResultCard("Duplicates found", "${gs.size} group${if (gs.size == 1) "" else "s"} · ${Format.bytes(gs.sumOf { it.wasted })} could be freed")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { selected = Duplicates.suggestKeepFirst(gs) }, modifier = Modifier.weight(1f)) { Text("Select extras") }
                        OutlinedButton(onClick = { selected = emptySet() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                    }
                    val safe = Duplicates.safeToDelete(gs, selected)
                    Button(
                        onClick = { confirm = true },
                        enabled = safe.isNotEmpty() && job == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Delete ${safe.size} selected file${if (safe.size == 1) "" else "s"}") }
                    gs.take(SHOWN).forEach { g ->
                        OmniCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${g.files.size} copies · ${Format.bytes(g.size)} each", style = MaterialTheme.typography.titleSmall)
                                g.files.forEach { f ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable { selected = if (f.id in selected) selected - f.id else selected + f.id },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(checked = f.id in selected, onCheckedChange = { selected = if (it) selected + f.id else selected - f.id })
                                        Text(f.path, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    if (gs.size > SHOWN) Text("…and ${gs.size - SHOWN} more groups. Delete the shown ones, then scan again.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    val current = groups
    val picked = tree
    if (confirm && current != null && picked != null) {
        val toDelete = Duplicates.safeToDelete(current, selected)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete ${toDelete.size} file${if (toDelete.size == 1) "" else "s"}?") },
            text = { Text("These files will be permanently deleted from your phone. This cannot be undone. One copy of each is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch {
                        val deletedIds = withContext(Dispatchers.IO) { toDelete.filter { SafWalker.delete(context.contentResolver, picked, it) }.toSet() }
                        val deleted = deletedIds.size
                        val failed = toDelete.size - deleted
                        result = "Deleted $deleted file${if (deleted == 1) "" else "s"}" + if (failed > 0) ". $failed could not be deleted." else "."
                        val remaining = current.map { g -> Duplicates.Group(g.size, g.files.filter { it.id !in deletedIds }) }
                        groups = remaining.filter { it.files.size > 1 }
                        selected = emptySet()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}
