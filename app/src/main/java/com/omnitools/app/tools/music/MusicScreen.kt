package com.omnitools.app.tools.music

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeat
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitools.app.ui.components.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class Picked(val tracks: List<Track>, val skipped: Int, val notRemembered: Int)

/** Reads the picked files: keeps access to them after a restart and drops anything that is not audio. */
private fun readPicked(context: Context, uris: List<Uri>): Picked {
    val tracks = ArrayList<Track>()
    var skipped = 0
    var notRemembered = 0
    for (u in uris) {
        // Android only lets an app remember a limited number of picked files (and some folders refuse): count the ones it won't.
        try { context.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { notRemembered++ }
        val name = MusicController.queryName(context, u)
        val mime = try { context.contentResolver.getType(u) } catch (_: Exception) { null }
        if (MusicFormat.looksLikeAudio(mime, name)) tracks.add(Track(u.toString(), MusicFormat.title(name))) else skipped++
    }
    return Picked(tracks, skipped, notRemembered)
}

@Composable
fun MusicScreen(prefs: com.omnitools.app.data.Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val design by prefs.musicDesign.collectAsStateWithLifecycle(initialValue = MusicDesign())
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var customize by rememberSaveable { mutableStateOf(false) }
    val state = MusicController.state
    val duration = MusicController.durationMs
    var position by remember { mutableIntStateOf(MusicController.position()) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var notice by remember { mutableStateOf<String?>(null) }
    var section by rememberSaveable { mutableIntStateOf(0) }           // 0 = Player, 1 = Playlists
    var openId by rememberSaveable { mutableStateOf<String?>(null) }   // playlist being viewed
    var pickTarget by remember { mutableStateOf<String?>(null) }       // null = the queue, otherwise a playlist id
    var nameDialog by remember { mutableStateOf<String?>(null) }       // "new", "queue" or "rename"
    var confirmDelete by remember { mutableStateOf(false) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { MusicLibrary.init(context) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askNotificationsOnce() {
        if (Build.VERSION.SDK_INT >= 33 && !askedNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            try { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) } catch (_: Exception) {}
        }
    }

    LaunchedEffect(state, MusicController.index) {
        while (state == MusicState.PLAYING || state == MusicState.PAUSED) {
            if (!dragging) position = MusicController.position()
            delay(250)
        }
        position = 0
        dragging = false   // never leave the seek bar stuck mid-drag if playback ended
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        // Cancelling gives an empty list: nothing changes and the music keeps playing.
        if (uris.isNotEmpty()) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPicked(context, uris) }
            val target = pickTarget
            if (picked.tracks.isNotEmpty()) {
                if (target == null) { askNotificationsOnce(); MusicController.addToQueue(context, picked.tracks) }
                else MusicLibrary.addTracks(target, picked.tracks)
            }
            notice = when {
                picked.tracks.isEmpty() -> "None of the selected files are audio files."
                picked.skipped > 0 -> "${picked.skipped} file${if (picked.skipped == 1) "" else "s"} skipped: not audio."
                target != null && picked.notRemembered > 0 ->
                    "${picked.notRemembered} song${if (picked.notRemembered == 1) "" else "s"} may not be remembered after you restart the app (Android limits how many files an app can keep)."
                else -> null
            }
        }
    }
    fun pick(target: String?) {
        pickTarget = target
        try { picker.launch(arrayOf("audio/*", "application/ogg")) } catch (e: Exception) { notice = "No file picker is available on this phone." }
    }
    fun playTracks(tracks: List<Track>, start: Int, shuffle: Boolean) {
        askNotificationsOnce()
        notice = null
        MusicController.playQueue(context, tracks, start, shuffle)
        section = 0
    }

    val playlist = MusicLibrary.find(openId)
    // A playlist that no longer exists (deleted) sends the user back to the list.
    LaunchedEffect(openId, playlist, MusicLibrary.loaded) { if (openId != null && playlist == null && MusicLibrary.loaded) openId = null }

    val playerBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayerCard(design, state, duration, position, dragging, dragFraction,
                onDrag = { dragging = true; dragFraction = it },
                onDragDone = {
                    MusicController.seekToFraction(dragFraction)
                    position = MusicFormat.seekTarget(dragFraction, duration)
                    dragging = false
                })
            Button(onClick = { pick(null) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (MusicController.queue.isEmpty()) "Choose songs" else "Add songs to queue")
            }
            TextButton(onClick = { customize = !customize }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Palette, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (customize) "Hide player design" else "Customize player design")
            }
            if (customize) DesignPanel(design, prefs)
        }
    }

    val listPane: @Composable (Modifier) -> Unit = { mod ->
        LazyColumn(
            modifier = mod,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("Player") })
                    FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("Playlists (${MusicLibrary.playlists.size})") })
                }
            }
            val message = notice ?: MusicController.error
            if (message != null) item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            if (section == 0) {
                if (!landscape) item { playerBlock() }
                if (MusicController.queue.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Queue · ${MusicController.queue.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { nameDialog = "queue" }) { Text("Save as playlist") }
                            TextButton(onClick = { MusicController.clear() }) { Text("Clear") }
                        }
                    }
                    itemsIndexed(MusicController.queue, key = { i, t -> "q$i${t.uri}" }) { i, t ->
                        TrackRow(i + 1, t.name, highlighted = i == MusicController.index, onClick = { MusicController.playIndex(i) }) {
                            IconButton(onClick = { MusicController.removeFromQueue(i) }) { Icon(Icons.Rounded.Close, contentDescription = "Remove from queue") }
                        }
                    }
                }
                item {
                    Text(
                        "Plays files from your phone only. Nothing is downloaded or uploaded. Music keeps playing when you leave this screen or the app; use the notification or Stop to end it.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            } else if (playlist == null) {
                item {
                    Button(onClick = { nameDialog = "new" }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New playlist")
                    }
                }
                if (MusicLibrary.playlists.isEmpty()) {
                    item {
                        Text(
                            "No playlists yet. Create one, add songs from your phone, then play it any time. Playlists are saved on this device only.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                itemsIndexed(MusicLibrary.playlists, key = { _, p -> p.id }) { _, p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = com.omnitools.app.ui.components.cardBorder(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { openId = p.id },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            com.omnitools.app.ui.components.IconBadge(Icons.Rounded.LibraryMusic, size = 40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall)
                                Text("${p.tracks.size} song${if (p.tracks.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { playTracks(p.tracks, 0, false) }, enabled = p.tracks.isNotEmpty()) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${p.name}")
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { openId = null }) { Text("‹ All playlists") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { nameDialog = "rename" }) { Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist") }
                    }
                }
                item {
                    Text(playlist.name, style = MaterialTheme.typography.headlineSmall)
                    Text("${playlist.tracks.size} song${if (playlist.tracks.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { playTracks(playlist.tracks, 0, false) }, enabled = playlist.tracks.isNotEmpty(), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Play")
                        }
                        OutlinedButton(
                            onClick = { playTracks(playlist.tracks, playlist.tracks.indices.random(), true) },
                            enabled = playlist.tracks.isNotEmpty(), modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Rounded.Shuffle, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Shuffle") }
                    }
                }
                item {
                    OutlinedButton(onClick = { pick(playlist.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add songs")
                    }
                }
                itemsIndexed(playlist.tracks, key = { i, t -> "p$i${t.uri}" }) { i, t ->
                    TrackRow(i + 1, t.name, highlighted = false, onClick = { playTracks(playlist.tracks, i, false) }) {
                        IconButton(onClick = { MusicLibrary.moveTrack(playlist.id, i, -1) }, enabled = i > 0) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up") }
                        IconButton(onClick = { MusicLibrary.moveTrack(playlist.id, i, 1) }, enabled = i < playlist.tracks.lastIndex) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down") }
                        IconButton(onClick = { MusicLibrary.removeTrack(playlist.id, i) }) { Icon(Icons.Rounded.Close, contentDescription = "Remove from playlist") }
                    }
                }
                if (playlist.tracks.isEmpty()) {
                    item { Text("This playlist is empty. Tap Add songs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    ScreenScaffold(title = "Music Player", onBack = onBack) { padding ->
        if (landscape) {
            Row(Modifier.padding(padding).fillMaxWidth()) {
                Column(Modifier.weight(0.46f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) { playerBlock() }
                listPane(Modifier.weight(0.54f))
            }
        } else {
            listPane(Modifier.padding(padding).fillMaxWidth())
        }
    }

    nameDialog?.let { kind ->
        NameDialog(
            title = when (kind) { "rename" -> "Rename playlist"; "queue" -> "Save queue as playlist"; else -> "New playlist" },
            initial = if (kind == "rename") playlist?.name.orEmpty() else "",
            onDismiss = { nameDialog = null },
            onConfirm = { name ->
                when (kind) {
                    "rename" -> playlist?.let { MusicLibrary.rename(it.id, name) }
                    "queue" -> { MusicLibrary.create(name, MusicController.queue); section = 1 }
                    else -> { val p = MusicLibrary.create(name); openId = p.id }
                }
                nameDialog = null
            },
        )
    }
    if (confirmDelete && playlist != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${playlist.name}\"?") },
            text = { Text("The playlist is removed. Your music files are not touched.") },
            confirmButton = { TextButton(onClick = { MusicLibrary.delete(playlist.id); openId = null; confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it.take(60) }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TrackRow(number: Int, name: String, highlighted: Boolean, onClick: () -> Unit, actions: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (highlighted) Icon(Icons.Rounded.MusicNote, contentDescription = "Now playing", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        else Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(MusicFormat.titleNoExtension(name), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        actions()
    }
}

@Composable
private fun PlayerCard(
    design: MusicDesign, state: MusicState, duration: Int, position: Int, dragging: Boolean, dragFraction: Float,
    onDrag: (Float) -> Unit, onDragDone: () -> Unit,
) {
    val ready = state == MusicState.PLAYING || state == MusicState.PAUSED
    val hasSong = MusicController.queue.isNotEmpty()
    val c = playerColors(design)
    Card(
        colors = CardDefaults.cardColors(containerColor = c.container),
        border = if (c.brush == null && design.style != PlayerStyle.VINYL) com.omnitools.app.ui.components.cardBorder() else null,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().then(if (c.brush != null) Modifier.background(c.brush) else Modifier).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Artwork(design, playing = state == MusicState.PLAYING, loading = state == MusicState.LOADING, size = if (design.style == PlayerStyle.VINYL) 150.dp else 110.dp)
            val name = MusicController.fileName
            Text(
                if (name == null) "No song selected" else MusicFormat.titleNoExtension(name),   // shown in full, never cut with "..."
                style = if (design.style == PlayerStyle.MINIMAL) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = c.fg, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { contentDescription = "Song: ${name ?: "none"}" },
            )
            val count = MusicController.queue.size
            Text(
                when (state) {
                    MusicState.EMPTY -> "Pick MP3 or other audio files saved on this phone."
                    MusicState.LOADING -> "Loading…"
                    MusicState.PLAYING -> "Playing" + if (count > 1) " · song ${MusicController.index + 1} of $count" else ""
                    MusicState.PAUSED -> "Paused" + if (count > 1) " · song ${MusicController.index + 1} of $count" else ""
                    MusicState.STOPPED -> "Stopped · press play to start again"
                    MusicState.ERROR -> "Can't play this file"
                },
                style = MaterialTheme.typography.bodySmall, color = c.sub, textAlign = TextAlign.Center,
            )
            val shown = if (dragging) dragFraction else MusicFormat.fraction(position, duration)
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = if (ready) shown else 0f, onValueChange = onDrag, onValueChangeFinished = onDragDone,
                    enabled = ready && duration > 0, modifier = Modifier.semantics { contentDescription = "Seek bar" },
                    colors = SliderDefaults.colors(
                        thumbColor = if (c.brush != null) Color.White else c.accentA,
                        activeTrackColor = if (c.brush != null) Color.White else c.accentA,
                        inactiveTrackColor = c.sub.copy(alpha = 0.3f),
                        disabledThumbColor = c.sub.copy(alpha = 0.4f),
                        disabledActiveTrackColor = c.sub.copy(alpha = 0.3f),
                        disabledInactiveTrackColor = c.sub.copy(alpha = 0.2f),
                    ),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val shownPos = if (dragging) MusicFormat.seekTarget(dragFraction, duration) else position
                    Text(MusicFormat.time(if (ready) shownPos else 0), style = MaterialTheme.typography.labelMedium, color = c.sub)
                    Text(MusicFormat.time(if (ready) duration else 0), style = MaterialTheme.typography.labelMedium, color = c.sub)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { MusicController.previous() }, enabled = hasSong) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous song", tint = c.fg) }
                FilledIconButton(
                    onClick = { MusicController.togglePlayPause() },
                    enabled = hasSong, modifier = Modifier.size(68.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.buttonBg, contentColor = c.buttonFg, disabledContainerColor = c.sub.copy(alpha = 0.25f)),
                ) {
                    val playing = MusicController.showsPause
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (playing) "Pause" else "Play", modifier = Modifier.size(36.dp))
                }
                FilledTonalIconButton(
                    onClick = { MusicController.stop() }, enabled = ready, modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = c.sub.copy(alpha = 0.2f), contentColor = c.fg, disabledContainerColor = c.sub.copy(alpha = 0.1f)),
                ) { Icon(Icons.Rounded.Stop, contentDescription = "Stop") }
                IconButton(onClick = { MusicController.next() }, enabled = hasSong) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next song", tint = c.fg) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { MusicController.skip(-10_000) }, enabled = ready) { Icon(Icons.Rounded.Replay10, contentDescription = "Back 10 seconds", tint = c.fg) }
                IconButton(onClick = { MusicController.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = if (MusicController.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (MusicController.shuffle) (if (c.brush != null) Color.White else c.accentA) else c.sub)
                }
                IconButton(onClick = { MusicController.cycleRepeat() }) {
                    val mode = MusicController.repeat
                    Icon(
                        if (mode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = when (mode) { RepeatMode.OFF -> "Repeat off"; RepeatMode.ALL -> "Repeat all"; RepeatMode.ONE -> "Repeat one" },
                        tint = if (mode == RepeatMode.OFF) c.sub else (if (c.brush != null) Color.White else c.accentA),
                    )
                }
                IconButton(onClick = { MusicController.skip(10_000) }, enabled = ready) { Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds", tint = c.fg) }
            }
        }
    }
}

/** Lets the user pick how the player looks: style, accent colour and whether the mini player shows on every screen. */
@Composable
private fun DesignPanel(design: MusicDesign, prefs: com.omnitools.app.data.Prefs) {
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = com.omnitools.app.ui.components.cardBorder(),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Style", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerStyle.values().forEach { st ->
                    FilterChip(selected = design.style == st, onClick = { scope.launch { prefs.setMusicStyle(st) } }, label = { Text(st.label) })
                }
            }
            Text("Colour", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MusicAccents.names.forEachIndexed { i, label ->
                    val pair = accentPair(design.copy(accent = i))
                    val selected = design.accent == i
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(40.dp)
                                .background(Brush.linearGradient(listOf(pair.first, pair.second)), CircleShape)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable(role = androidx.compose.ui.semantics.Role.RadioButton) { scope.launch { prefs.setMusicAccent(i) } }
                                .semantics { contentDescription = "$label colour" + if (selected) ", selected" else "" },
                        )
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mini player on every screen", style = MaterialTheme.typography.titleSmall)
                    Text("A slim bar with play, pause, skip and close, in portrait and landscape.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = design.miniPlayer, onCheckedChange = { scope.launch { prefs.setMusicMini(it) } })
            }
        }
    }
}
