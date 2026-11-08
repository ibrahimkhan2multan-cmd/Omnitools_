package com.omnitools.app.tools.music

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import kotlinx.coroutines.delay

@Composable
fun MusicScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state = MusicController.state
    val duration = MusicController.durationMs
    var position by remember { mutableIntStateOf(MusicController.position()) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var pickError by remember { mutableStateOf<String?>(null) }

    // Re-read the position a few times a second while a song is loaded.
    LaunchedEffect(state) {
        while (state == MusicState.PLAYING || state == MusicState.PAUSED) {
            if (!dragging) position = MusicController.position()
            delay(250)
        }
        if (state != MusicState.PLAYING && state != MusicState.PAUSED) position = 0
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        // Cancelling the picker gives null: nothing changes and the current song keeps playing.
        if (uri != null) {
            pickError = null
            MusicController.open(context, uri)
        }
    }
    fun pick() {
        try { picker.launch(arrayOf("audio/*", "application/ogg")) } catch (e: Exception) { pickError = "No file picker is available on this phone." }
    }

    ScreenScaffold(title = "Music Player", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = com.omnitools.app.ui.components.cardBorder(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Disc(spinning = state == MusicState.PLAYING, loading = state == MusicState.LOADING)
                    val name = MusicController.fileName
                    Text(
                        if (name == null) "No song selected" else MusicFormat.titleNoExtension(name),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { contentDescription = "Song: ${name ?: "none"}" },
                    )
                    Text(
                        when (state) {
                            MusicState.EMPTY -> "Pick an MP3 or other audio file saved on this phone."
                            MusicState.LOADING -> "Loading…"
                            MusicState.PLAYING -> "Playing · ${name.orEmpty()}"
                            MusicState.PAUSED -> "Paused · ${name.orEmpty()}"
                            MusicState.STOPPED -> "Stopped · ${name.orEmpty()}"
                            MusicState.ERROR -> "Can't play this file"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val ready = state == MusicState.PLAYING || state == MusicState.PAUSED
                    val shown = if (dragging) dragFraction else MusicFormat.fraction(position, duration)
                    Column(Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (ready) shown else 0f,
                            onValueChange = { dragging = true; dragFraction = it },
                            onValueChangeFinished = {
                                MusicController.seekToFraction(dragFraction)
                                position = MusicFormat.seekTarget(dragFraction, duration)
                                dragging = false
                            },
                            enabled = ready && duration > 0,
                            modifier = Modifier.semantics { contentDescription = "Seek bar" },
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            val shownPos = if (dragging) MusicFormat.seekTarget(dragFraction, duration) else position
                            Text(MusicFormat.time(if (ready) shownPos else 0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MusicFormat.time(if (ready) duration else 0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { MusicController.skip(-10_000) }, enabled = ready) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Back 10 seconds")
                        }
                        FilledIconButton(
                            onClick = { MusicController.togglePlayPause() },
                            enabled = state != MusicState.EMPTY && state != MusicState.LOADING,
                            modifier = Modifier.size(68.dp),
                        ) {
                            val playing = state == MusicState.PLAYING
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { MusicController.stop() },
                            enabled = state == MusicState.PLAYING || state == MusicState.PAUSED,
                            modifier = Modifier.size(52.dp),
                        ) { Icon(Icons.Filled.Stop, contentDescription = "Stop") }
                        IconButton(onClick = { MusicController.skip(10_000) }, enabled = ready) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds")
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { MusicController.setRepeat(!MusicController.repeat) },
                        colors = if (MusicController.repeat) IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors(),
                    ) { Icon(Icons.Filled.Repeat, contentDescription = if (MusicController.repeat) "Repeat on" else "Repeat off") }
                }
            }

            val message = pickError ?: MusicController.error
            if (message != null) {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }

            Button(onClick = ::pick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (MusicController.fileName == null) "Choose audio file" else "Choose another file")
            }
            Text(
                "Plays files from your phone only. Nothing is downloaded or uploaded. The music keeps playing while you use other tools, and stops when you close the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A round "record" that gently pulses while music plays. */
@Composable
private fun Disc(spinning: Boolean, loading: Boolean) {
    val pulse = rememberInfiniteTransition(label = "disc").animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "disc pulse",
    )
    Box(
        Modifier.size(120.dp).scale(if (spinning) pulse.value else 1f).background(
            androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
            CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
        else Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(56.dp))
    }
}
