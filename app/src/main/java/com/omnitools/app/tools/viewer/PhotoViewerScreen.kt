package com.omnitools.app.tools.viewer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.accent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoViewerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) photos = uris
    }
    val pager = rememberPagerState(pageCount = { photos.size })
    var zoomed by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Photo Viewer",
        onBack = onBack,
        actions = {
            if (photos.isNotEmpty()) {
                IconButton(onClick = {
                    val uri = photos.getOrNull(pager.currentPage) ?: return@IconButton
                    try {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = context.contentResolver.getType(uri) ?: "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "Share picture"))
                    } catch (_: Throwable) {}
                }) { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                IconButton(onClick = { picker.launch(arrayOf("image/*")) }) { Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Open other pictures") }
            }
        },
    ) { padding ->
        if (photos.isEmpty()) {
            com.omnitools.app.ui.components.FeatureEmptyState(
                icon = Icons.Rounded.PhotoLibrary,
                title = "Open pictures from your phone",
                body = "JPEG, PNG, WebP, GIF, BMP and, on phones that support them, HEIC and AVIF. Pick one or many, then swipe between them. Pinch or double-tap to zoom.",
                actionLabel = "Choose pictures",
                onAction = { picker.launch(arrayOf("image/*")) },
                modifier = Modifier.padding(padding),
                accent = com.omnitools.app.data.Category.IMAGES.accent(),
            )
        } else {
            Column(Modifier.padding(padding).fillMaxSize()) {
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = !zoomed,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = { photos[it].toString() + it },
                ) { page ->
                    PhotoPage(photos[page], active = page == pager.currentPage, onZoomChanged = { zoomed = it })
                }
                val name = remember(photos, pager.currentPage) { photos.getOrNull(pager.currentPage)?.let { PhotoLoader.displayName(context, it) } ?: "" }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(ViewerLogic.position(pager.currentPage, photos.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PhotoPage(uri: Uri, active: Boolean, onZoomChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val loaded by produceState<PhotoLoader.Loaded?>(null, uri) {
        value = withContext(Dispatchers.IO) { PhotoLoader.load(context, uri) }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Leaving a picture resets its zoom, so coming back shows it whole.
    LaunchedEffect(active) { if (!active) { scale = 1f; offset = Offset.Zero } }
    LaunchedEffect(scale > 1.01f, active) { if (active) onZoomChanged(scale > 1.01f) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val l = loaded
        when {
            l == null -> CircularProgressIndicator()
            l.bitmap == null -> Text(l.error ?: "Cannot open this picture.", textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
            else -> Image(
                bitmap = l.bitmap.asImageBitmap(),
                contentDescription = "Picture",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scale = ViewerLogic.doubleTapScale(scale)
                            if (scale <= 1f) offset = Offset.Zero
                        })
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.size > 1 || scale > 1.01f) {
                                    scale = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                                    val mx = ViewerLogic.maxPan(size.width.toFloat(), scale)
                                    val my = ViewerLogic.maxPan(size.height.toFloat(), scale)
                                    offset = if (scale <= 1.01f) Offset.Zero else {
                                        val p = offset + event.calculatePan()
                                        Offset(p.x.coerceIn(-mx, mx), p.y.coerceIn(-my, my))
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            )
        }
    }
}
