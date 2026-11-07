package com.omnitools.app.tools.image

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.Choice
import com.omnitools.app.ui.components.ChipRow
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.BitmapLoader
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

/** Shared "choose a picture" state: loads a downscaled copy off the main thread. */
@Composable
private fun rememberPicture(maxSide: Int): PictureState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = remember { PictureState() }
    st.launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            st.busy = true; st.error = null
            scope.launch {
                val b = withContext(Dispatchers.IO) { BitmapLoader.decode(context, uri, maxSide) }
                st.bitmap = b; st.uri = uri
                if (b == null) st.error = "That file could not be opened as a picture."
                st.busy = false
            }
        }
    }
    return st
}

class PictureState {
    var bitmap by mutableStateOf<Bitmap?>(null)
    var uri by mutableStateOf<Uri?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null
    fun pick() { launcher?.launch(arrayOf("image/*")) }
}

@Composable
private fun PickButton(st: PictureState) {
    Button(onClick = { st.pick() }, enabled = !st.busy, modifier = Modifier.fillMaxWidth()) {
        Text(if (st.bitmap == null) "Choose a picture" else "Choose another picture")
    }
    if (st.busy) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    ErrorText(st.error)
}

@Composable
fun ColorFromImageScreen(onBack: () -> Unit) {
    val st = rememberPicture(1600)
    var picked by remember { mutableStateOf<Int?>(null) }
    ScreenScaffold(title = "Color Picker", onBack = onBack) { padding ->
        ToolPage(padding) {
            PickButton(st)
            val bmp = st.bitmap
            if (bmp == null) Note("Open a picture, then tap anywhere on it to read the exact colour at that spot.")
            else {
                Image(
                    bitmap = bmp.asImageBitmap(), contentDescription = "Chosen picture", contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height).clip(RoundedCornerShape(12.dp))
                        .pointerInput(bmp) {
                            detectTapGestures { o ->
                                val x = (o.x / size.width * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                                val y = (o.y / size.height * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                                picked = bmp.getPixel(x, y) and 0xFFFFFF
                            }
                        },
                )
                picked?.let { c ->
                    Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF000000.toInt() or c)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                    ResultCard("Hex", Palette.hex(c))
                    ResultCard("RGB", "${(c shr 16) and 255}, ${(c shr 8) and 255}, ${c and 255}")
                    CopyShareRow(Palette.hex(c), "Colour")
                }
            }
        }
    }
}

@Composable
fun PaletteScreen(onBack: () -> Unit) {
    val st = rememberPicture(1200)
    var count by remember { mutableStateOf(6) }
    val bmp = st.bitmap
    val swatches = remember(bmp, count) {
        if (bmp == null) emptyList() else {
            val scale = 120f / maxOf(bmp.width, bmp.height)
            val small = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true) else bmp
            val px = IntArray(small.width * small.height)
            small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
            Palette.extract(px, count)
        }
    }
    ScreenScaffold(title = "Color Palette", onBack = onBack) { padding ->
        ToolPage(padding) {
            PickButton(st)
            if (bmp == null) Note("Open a picture to get its main colours, from the most to the least used.")
            else {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = "Chosen picture", modifier = Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height).clip(RoundedCornerShape(12.dp)))
                ChipRow { listOf(4, 6, 8, 10).forEach { n -> Choice(count == n, "$n colours") { count = n } } }
                swatches.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF000000.toInt() or s.rgb)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)))
                        Column {
                            Text(Palette.hex(s.rgb), style = MaterialTheme.typography.titleMedium)
                            Text("${(s.share * 100).toInt().coerceAtLeast(1)}% of the picture", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (swatches.isNotEmpty()) CopyShareRow(swatches.joinToString("\n") { Palette.hex(it.rgb) }, "Palette")
            }
        }
    }
}

private val exifTags = listOf(
    "Camera maker" to android.media.ExifInterface.TAG_MAKE, "Camera model" to android.media.ExifInterface.TAG_MODEL,
    "Taken on" to android.media.ExifInterface.TAG_DATETIME_ORIGINAL, "Edited on" to android.media.ExifInterface.TAG_DATETIME,
    "Software" to android.media.ExifInterface.TAG_SOFTWARE, "Exposure time" to android.media.ExifInterface.TAG_EXPOSURE_TIME,
    "Aperture" to android.media.ExifInterface.TAG_F_NUMBER, "ISO" to android.media.ExifInterface.TAG_ISO_SPEED_RATINGS,
    "Focal length" to android.media.ExifInterface.TAG_FOCAL_LENGTH, "Flash" to android.media.ExifInterface.TAG_FLASH,
    "Width" to android.media.ExifInterface.TAG_IMAGE_WIDTH, "Height" to android.media.ExifInterface.TAG_IMAGE_LENGTH,
    "Orientation" to android.media.ExifInterface.TAG_ORIENTATION,
)

@Composable
fun ExifScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var rows by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var hasGps by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            uri = u; error = null
            try {
                context.contentResolver.openInputStream(u)?.use { s ->
                    val e = android.media.ExifInterface(s)
                    rows = exifTags.mapNotNull { (label, tag) -> e.getAttribute(tag)?.takeIf { it.isNotBlank() }?.let { label to it } }
                    hasGps = e.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE) != null
                } ?: run { error = "Could not open the file." }
            } catch (ex: Exception) { rows = emptyList(); hasGps = false; error = "This file has no readable photo details." }
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { dest ->
        val src = uri
        if (dest != null && src != null) scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    val b = BitmapLoader.decode(context, src, 4096) ?: return@withContext false
                    val bytes = ImageOps.encode(b, ImageOps.Format.JPEG, 92)
                    context.contentResolver.openOutputStream(dest, "w")?.use { it.write(bytes) } != null
                } catch (e: Exception) { false } catch (e: OutOfMemoryError) { false }
            }
            busy = false
            Toast.makeText(context, if (ok) "Clean copy saved" else "Could not save the copy", Toast.LENGTH_SHORT).show()
        }
    }
    ScreenScaffold(title = "Photo Details (EXIF)", onBack = onBack) { padding ->
        ToolPage(padding) {
            Button(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Choose a photo") }
            ErrorText(error)
            if (uri != null && error == null) {
                if (rows.isEmpty()) Text("No camera details found in this photo.", style = MaterialTheme.typography.titleSmall)
                rows.forEach { (k, v) -> ResultCard(k, v) }
                Text(
                    if (hasGps) "This photo contains the GPS location where it was taken." else "No GPS location was found in this photo.",
                    color = if (hasGps) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { save.launch("photo-clean.jpg") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save a copy without photo details") }
            }
            Note("The clean copy is re-saved as a JPEG, which leaves out location, camera and date details. Your original file is not changed.")
        }
    }
}

@Composable
fun CompressToSizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = rememberPicture(3000)
    var target by remember { mutableStateOf("200") }
    var webp by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ByteArray?>(null) }
    var plan by remember { mutableStateOf<SizeFit.Plan?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val format = if (webp) ImageOps.Format.WEBP else ImageOps.Format.JPEG
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mime)) { dest ->
        val bytes = result
        if (dest != null && bytes != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { try { context.contentResolver.openOutputStream(dest, "w")?.use { it.write(bytes) } != null } catch (e: Exception) { false } }
            Toast.makeText(context, if (ok) "Image saved" else "Could not save the image", Toast.LENGTH_SHORT).show()
        }
    }
    ScreenScaffold(title = "Compress to Size", onBack = onBack) { padding ->
        ToolPage(padding) {
            PickButton(st)
            val bmp = st.bitmap
            if (bmp == null) Note("Pick a picture and a size limit, such as 200 KB for an upload form. The tool finds the best quality that fits.")
            else {
                TextInput("Target size in KB", target, { target = it.filter(Char::isDigit).take(6) }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                ChipRow { Choice(!webp, "JPEG") { webp = false; result = null }; Choice(webp, "WebP") { webp = true; result = null } }
                Button(
                    enabled = !busy && (target.toIntOrNull() ?: 0) > 0,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val limit = (target.toIntOrNull() ?: 0) * 1024
                        busy = true; message = null; result = null; plan = null
                        scope.launch {
                            try {
                                val p = withContext(Dispatchers.Default) { SizeFit.fit(limit) { q, s -> ImageOps.encode(bmp, format, q, s).size } }
                                if (p == null) message = "It could not be made that small. Try a larger limit."
                                else {
                                    result = withContext(Dispatchers.Default) { ImageOps.encode(bmp, format, p.quality, p.scalePercent) }
                                    plan = p
                                }
                            } catch (e: OutOfMemoryError) {
                                message = "Not enough memory for this picture. Try a smaller picture."
                            } catch (e: Exception) {
                                message = "That did not work: ${e.message ?: "unknown error"}"
                            }
                            busy = false
                        }
                    },
                ) { Text("Compress") }
                if (busy) CircularProgressIndicator()
                ErrorText(message)
                val p = plan; val r = result
                if (p != null && r != null) {
                    ResultCard("New size", Format.bytes(r.size.toLong()))
                    ResultCard("Quality used", "${p.quality}%" + if (p.scalePercent < 100) "  ·  shrunk to ${p.scalePercent}% of the original dimensions" else "")
                    Button(onClick = { save.launch("compressed.${format.extension}") }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                }
            }
        }
    }
}
