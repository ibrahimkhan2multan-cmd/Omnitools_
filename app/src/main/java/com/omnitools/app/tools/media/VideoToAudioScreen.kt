package com.omnitools.app.tools.media

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.tools.music.MusicController
import com.omnitools.app.tools.music.MusicFormat
import com.omnitools.app.tools.music.Track
import com.omnitools.app.ui.components.IconBadge
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.cardBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun libraryDir(context: android.content.Context) = File(context.filesDir, "extracted_audio")

private fun listLibrary(context: android.content.Context): List<File> =
    libraryDir(context).listFiles { f -> f.isFile && !f.name.startsWith(".") }?.sortedByDescending { it.lastModified() } ?: emptyList()

private fun mimeFor(f: File): String = Container.values().firstOrNull { it.ext == f.extension.lowercase() }?.mime ?: "audio/*"

@Composable
fun VideoToAudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf<String?>(null) }
    var probe by remember { mutableStateOf<Probe?>(null) }
    var wantWav by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf<List<File>>(emptyList()) }
    var toDelete by remember { mutableStateOf<File?>(null) }
    var toSave by remember { mutableStateOf<File?>(null) }
    val cancelFlag = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    LaunchedEffect(Unit) { library = withContext(Dispatchers.IO) { listLibrary(context) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        // Cancelling gives null: nothing changes.
        if (uri != null) scope.launch {
            message = null; probe = null; videoUri = uri
            videoName = withContext(Dispatchers.IO) { MusicController.queryName(context, uri) }
            try {
                val p = withContext(Dispatchers.IO) { AudioExtractor.probe(context, uri) }
                probe = p
                if (p.audioMime == null) { message = "This video has no sound track, so there is nothing to extract."; isError = true }
            } catch (e: Exception) {
                videoUri = null
                message = e.message ?: "This file could not be read as a video."; isError = true
            }
        }
    }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/*")) { dest: Uri? ->
        val src = toSave
        toSave = null
        if (dest != null && src != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(dest)?.use { o -> src.inputStream().use { it.copyTo(o) } } != null
                } catch (e: Exception) { false }
            }
            message = if (ok) "Saved a copy to your phone." else "Could not save the copy there. Try another folder."
            isError = !ok
        }
    }

    fun refresh() = scope.launch { library = withContext(Dispatchers.IO) { listLibrary(context) } }

    fun extract() {
        val uri = videoUri ?: return
        val taken = library.map { it.name }.toSet()
        cancelFlag.set(false)
        running = true; progress = 0f; message = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                AudioExtractor.extract(
                    context, uri, libraryDir(context),
                    nameFor = { c -> AudioPlan.outputName(videoName, c, taken) },
                    wantWav = wantWav,
                    onProgress = { progress = it },
                    isCancelled = { cancelFlag.get() || !this.isActive },
                )
            }
            running = false
            when (result) {
                is ExtractResult.Done -> {
                    message = "Done: ${result.file.name} (${AudioPlan.size(result.file.length())}). Saved inside OmniTools." + (result.note?.let { " $it" } ?: "")
                    isError = false
                }
                is ExtractResult.Failed -> { message = result.message; isError = true }
                ExtractResult.Cancelled -> { message = "Cancelled."; isError = false }
            }
            library = withContext(Dispatchers.IO) { listLibrary(context) }
        }
    }
    // The cancel button and leaving the screen both stop the work.
    fun cancel() { cancelFlag.set(true) }

    ScreenScaffold(title = "Video to Audio", onBack = { cancel(); onBack() }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = cardBorder(),
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.VideoFile, size = 44)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(videoName ?: "No video selected", style = MaterialTheme.typography.titleSmall)
                                val p = probe
                                if (p != null && p.audioMime != null) {
                                    Text(
                                        listOfNotNull(
                                            "Sound: ${AudioPlan.codecName(p.audioMime)}",
                                            if (p.sampleRate > 0) "${p.sampleRate / 1000.0} kHz" else null,
                                            AudioPlan.channelsLabel(p.channels).ifEmpty { null },
                                            if (p.durationUs > 0) MusicFormat.time((p.durationUs / 1000).toInt()) else null,
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else if (videoUri == null) {
                                    Text("Pick a video saved on your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        OutlinedButton(onClick = { picker.launch(arrayOf("video/*", "audio/*")) }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
                            Text(if (videoUri == null) "Choose video" else "Choose another video")
                        }
                        val p = probe
                        if (p?.audioMime != null) {
                            val original = AudioPlan.choose(p.audioMime, Build.VERSION.SDK_INT, false)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (original != Container.WAV) {
                                    FilterChip(selected = !wantWav, onClick = { wantWav = false }, label = { Text("Original (.${original.ext})") }, enabled = !running)
                                }
                                FilterChip(selected = wantWav || original == Container.WAV, onClick = { wantWav = true }, label = { Text("WAV (.wav)") }, enabled = !running && original != Container.WAV)
                            }
                            Text(
                                if (original == Container.WAV)
                                    "The sound in this video (${AudioPlan.codecName(p.audioMime)}) cannot be copied as is, so it is converted to WAV. WAV files are large (about 10 MB per minute)."
                                else if (wantWav) "WAV is uncompressed and plays anywhere, but is large (about 10 MB per minute)."
                                else "Original keeps the exact sound from the video with no quality loss, and is fast.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (running) {
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                Text("Extracting… ${(progress * 100).toInt()}%  Keep this screen open.", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                            } else {
                                Button(onClick = { extract() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                                    Icon(Icons.Filled.AudioFile, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Extract audio")
                                }
                            }
                        }
                    }
                }
            }
            message?.let { m ->
                item { Text(m, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            }
            item {
                Text(
                    "Saved in OmniTools" + if (library.isNotEmpty()) " · ${library.size} file${if (library.size == 1) "" else "s"}, ${AudioPlan.size(library.sumOf { it.length() })}" else "",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (library.isEmpty()) {
                item {
                    Text(
                        "Extracted audio is kept here, inside the app, until you delete it. You can play it in the Music Player or save a copy anywhere on your phone.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(library, key = { it.absolutePath }) { f ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = cardBorder(),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp)) {
                        Text(f.name, style = MaterialTheme.typography.titleSmall)
                        Text(AudioPlan.size(f.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            IconButton(onClick = {
                                MusicController.playQueue(context, listOf(Track(Uri.fromFile(f).toString(), f.name)))
                                message = "Playing in the Music Player. Use the mini player at the bottom to control it."; isError = false
                            }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${f.name}") }
                            IconButton(onClick = {
                                MusicController.addToQueue(context, listOf(Track(Uri.fromFile(f).toString(), f.name)))
                                message = "Added to the Music Player queue."; isError = false
                            }) { Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add ${f.name} to the music queue") }
                            IconButton(onClick = { toSave = f; saver.launch(f.name) }) { Icon(Icons.Filled.SaveAlt, contentDescription = "Save a copy of ${f.name} to your phone") }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { toDelete = f }) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${f.name}") }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { f ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${f.name}\"?") },
            text = { Text("This removes the extracted audio from OmniTools. Copies you saved elsewhere are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    scope.launch { withContext(Dispatchers.IO) { f.delete() }; refresh() }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
