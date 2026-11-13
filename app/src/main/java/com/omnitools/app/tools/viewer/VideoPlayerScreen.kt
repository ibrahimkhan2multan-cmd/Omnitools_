package com.omnitools.app.tools.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.omnitools.app.tools.music.MusicController
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.accent

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

@Composable
fun VideoPlayerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var videos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var current by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(1f) }
    var error by remember { mutableStateOf<String?>(null) }
    val player = remember { ExoPlayer.Builder(context).build() }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            videos = uris
            current = 0
            error = null
            try { MusicController.pause() } catch (_: Throwable) {}
            player.setMediaItems(uris.map { MediaItem.fromUri(it) })
            player.prepare()
            player.playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { current = player.currentMediaItemIndex; error = null }
            override fun onPlayerError(e: PlaybackException) {
                error = "This phone cannot play this video (${e.errorCodeName}). Its format or codec may not be supported."
                // Go on to the next video if there is one, instead of getting stuck.
                if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    // A video should not keep playing once the screen is hidden.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    DisposableEffect(Unit) {
        onDispose { context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    ScreenScaffold(
        title = "Video Player",
        onBack = onBack,
        actions = {
            if (videos.isNotEmpty()) {
                IconButton(onClick = {
                    context.findActivity()?.requestedOrientation =
                        if (landscape) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }) { Icon(if (landscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, contentDescription = "Rotate screen") }
                IconButton(onClick = { picker.launch(arrayOf("video/*")) }) { Icon(Icons.Rounded.VideoLibrary, contentDescription = "Open other videos") }
            }
        },
    ) { padding ->
        if (videos.isEmpty()) {
            com.omnitools.app.ui.components.FeatureEmptyState(
                icon = Icons.Rounded.PlayCircle,
                title = "Play videos from your phone",
                body = "MP4, MKV, WebM, 3GP, TS and more. Works offline, with a playlist, speed control and seeking. Pick one or many videos.",
                actionLabel = "Choose videos",
                onAction = { picker.launch(arrayOf("video/*")) },
                modifier = Modifier.padding(padding),
                accent = com.omnitools.app.data.Category.IMAGES.accent(),
            )
        } else {
            val playerView: @Composable (Modifier) -> Unit = { mod ->
                AndroidView(
                    modifier = mod.background(Color.Black),
                    factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                )
            }
            if (landscape) {
                playerView(Modifier.padding(padding).fillMaxSize())
            } else {
                Column(Modifier.padding(padding).fillMaxSize()) {
                    playerView(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ViewerLogic.SPEEDS.forEach { s ->
                            FilterChip(selected = speed == s, onClick = { speed = s; player.setPlaybackSpeed(s) }, label = { Text(ViewerLogic.speedLabel(s)) })
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp)) }
                    LazyColumn(Modifier.fillMaxSize()) {
                        itemsIndexed(videos) { i, uri ->
                            val name = remember(uri) { PhotoLoader.displayName(context, uri) }
                            ListItem(
                                headlineContent = { Text(name, maxLines = 1) },
                                leadingContent = { Icon(if (i == current) Icons.Rounded.PlayCircle else Icons.Rounded.VideoLibrary, null, tint = if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth().clickable { player.seekTo(i, 0); player.play(); current = i },
                            )
                        }
                    }
                }
            }
        }
    }
}
