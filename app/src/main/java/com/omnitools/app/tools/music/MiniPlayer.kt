package com.omnitools.app.tools.music

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A slim player docked at the bottom of every screen while music is loaded, in portrait and landscape,
 * so the music can be paused, skipped or stopped without opening the notification shade.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(design: MusicDesign, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val state = MusicController.state
    val colors = playerColors(design)
    var position by remember { mutableIntStateOf(0) }
    LaunchedEffect(state, MusicController.index) {
        while (state == MusicState.PLAYING || state == MusicState.PAUSED) {
            position = MusicController.position()
            delay(500)
        }
        position = 0
    }
    val bg = if (design.style == PlayerStyle.CLASSIC || design.style == PlayerStyle.MINIMAL) MaterialTheme.colorScheme.surfaceVariant else colors.container
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = bg,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.then(if (colors.brush != null) Modifier.background(colors.brush) else Modifier).navigationBarsPadding(),
        ) {
            LinearProgressIndicator(
                progress = { MusicFormat.fraction(position, MusicController.durationMs) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = if (colors.brush != null) Color.White else colors.accentA,
                trackColor = Color.Transparent,
            )
            val fg = if (design.style == PlayerStyle.CLASSIC || design.style == PlayerStyle.MINIMAL) MaterialTheme.colorScheme.onSurface else colors.fg
            val sub = if (design.style == PlayerStyle.CLASSIC || design.style == PlayerStyle.MINIMAL) MaterialTheme.colorScheme.onSurfaceVariant else colors.sub
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (design.style != PlayerStyle.MINIMAL) {
                    Artwork(design, playing = state == MusicState.PLAYING, loading = state == MusicState.LOADING, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 8.dp)) {
                    Text(
                        MusicFormat.titleNoExtension(MusicController.fileName),
                        style = MaterialTheme.typography.titleSmall, color = fg, maxLines = 1,
                        modifier = Modifier.basicMarquee(),   // long names scroll instead of being cut with "..."
                    )
                    val n = MusicController.queue.size
                    Text(
                        when (state) {
                            MusicState.PLAYING -> "Playing"
                            MusicState.PAUSED -> "Paused"
                            MusicState.LOADING -> "Loading…"
                            MusicState.ERROR -> "Can't play"
                            else -> "Stopped"
                        } + if (n > 1) " · ${MusicController.index + 1}/$n" else "",
                        style = MaterialTheme.typography.labelSmall, color = sub,
                    )
                }
                IconButton(onClick = { MusicController.previous() }) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous song", tint = fg) }
                IconButton(onClick = { MusicController.togglePlayPause() }) {
                    val playing = state == MusicState.PLAYING
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (playing) "Pause" else "Play", tint = fg, modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = { MusicController.next() }) { Icon(Icons.Filled.SkipNext, contentDescription = "Next song", tint = fg) }
                IconButton(onClick = { MusicController.clear() }) { Icon(Icons.Filled.Close, contentDescription = "Stop and close player", tint = sub) }
            }
        }
    }
}
