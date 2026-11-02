package com.omnitools.app.tools.image

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.util.Actions
import com.omnitools.app.util.BitmapLoader
import com.omnitools.app.util.FileDetails
import com.omnitools.app.util.FileInfo
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf<Uri?>(null) }
    var details by remember { mutableStateOf<FileDetails?>(null) }
    var originalInfo by remember { mutableStateOf<BitmapLoader.Info?>(null) }
    var working by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var cropL by remember { mutableStateOf(0f) }
    var cropT by remember { mutableStateOf(0f) }
    var cropR by remember { mutableStateOf(0f) }
    var cropB by remember { mutableStateOf(0f) }
    var scalePct by remember { mutableStateOf(100) }
    var format by remember { mutableStateOf(ImageOps.Format.JPEG) }
    var quality by remember { mutableStateOf(85) }
    var estimateBytes by remember { mutableStateOf<Int?>(null) }

    fun resetControls() {
        cropL = 0f; cropT = 0f; cropR = 0f; cropB = 0f
        scalePct = 100
        estimateBytes = null
    }

    fun load(uri: Uri) {
        scope.launch {
            busy = true
            error = null
            val info = withContext(Dispatchers.IO) { BitmapLoader.info(context, uri) }
            val bmp = if (info == null) null else withContext(Dispatchers.IO) { BitmapLoader.decode(context, uri, ImageOps.MAX_WORKING_SIDE) }
            if (info == null || bmp == null) {
                error = "That file could not be opened as a picture. Supported: JPEG, PNG, WebP, GIF (first frame), BMP, HEIF on newer phones."
            } else {
                source = uri
                details = FileInfo.query(context, uri)
                originalInfo = info
                working = bmp
                resetControls()
            }
            busy = false
        }
    }

    fun transform(op: (Bitmap) -> Bitmap) {
        val current = working ?: return
        scope.launch {
            busy = true
            error = null
            try {
                working = withContext(Dispatchers.Default) { op(current) }
            } catch (e: OutOfMemoryError) {
                error = "Not enough memory for that operation on this picture."
            } catch (e: Exception) {
                error = "That edit failed: ${e.message ?: "unknown error"}"
            }
            busy = false
        }
    }

    // Real encode in the background so the size shown is the size that will be saved.
    LaunchedEffect(working, format, quality, scalePct) {
        val bmp = working ?: return@LaunchedEffect
        delay(250)
        estimateBytes = try {
            withContext(Dispatchers.Default) { ImageOps.encode(bmp, format, quality, scalePct).size }
        } catch (e: Exception) {
            null
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) load(uri) }

    fun save(uri: Uri?) {
        val bmp = working
        if (uri == null || bmp == null) return
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    val bytes = ImageOps.encode(bmp, format, quality, scalePct)
                    context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) } != null
                } catch (e: Exception) {
                    false
                }
            }
            busy = false
            Toast.makeText(context, if (ok) "Image saved" else "Could not save the image", Toast.LENGTH_SHORT).show()
        }
    }
    val saveJpeg = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ImageOps.Format.JPEG.mime)) { save(it) }
    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ImageOps.Format.PNG.mime)) { save(it) }
    val saveWebp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ImageOps.Format.WEBP.mime)) { save(it) }

    val bmp = working
    ScreenScaffold(title = "Image Tools", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (bmp == null) "Choose a picture" else "Choose another picture")
            }
            ErrorText(error)

            if (bmp == null) {
                if (busy) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                Text(
                    "Open a picture to see its details, rotate, flip, crop, resize, convert between JPEG, PNG and WebP, and compress it. " +
                        "The original file is never changed; the result is saved as a new file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Box(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview of the picture being edited",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                )
                if (busy) CircularProgressIndicator()
            }

            val (outW, outH) = ImageMath.scaledSize(bmp.width, bmp.height, scalePct)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val o = originalInfo
                    Text(details?.name ?: "Picture", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (o != null) {
                        Text(
                            "Original: ${o.width} × ${o.height} px · ${o.mime ?: "unknown type"} · ${details?.sizeBytes?.let { Format.bytes(it) } ?: "size unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (maxOf(o.width, o.height) > ImageOps.MAX_WORKING_SIDE) {
                            Text(
                                "Large picture: editing uses a copy reduced to ${bmp.width} × ${bmp.height} px so it fits in memory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "Result: $outW × $outH px · ${format.label}" + (estimateBytes?.let { " · about ${Format.bytes(it.toLong())}" } ?: " · calculating size…"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            SectionTitle("Rotate & flip")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { transform { ImageOps.rotate(it, -90f) } }, enabled = !busy) { Text("Rotate left") }
                OutlinedButton(onClick = { transform { ImageOps.rotate(it, 90f) } }, enabled = !busy) { Text("Rotate right") }
                OutlinedButton(onClick = { transform { ImageOps.flip(it, horizontal = true) } }, enabled = !busy) { Text("Flip horizontal") }
                OutlinedButton(onClick = { transform { ImageOps.flip(it, horizontal = false) } }, enabled = !busy) { Text("Flip vertical") }
            }

            SectionTitle("Crop")
            Text("Remove a percentage from each edge.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CropSlider("Left", cropL) { cropL = it }
            CropSlider("Top", cropT) { cropT = it }
            CropSlider("Right", cropR) { cropR = it }
            CropSlider("Bottom", cropB) { cropB = it }
            val anyCrop = cropL > 0 || cropT > 0 || cropR > 0 || cropB > 0
            OutlinedButton(
                onClick = {
                    val l = cropL; val t = cropT; val r = cropR; val b = cropB
                    transform { ImageOps.crop(it, ImageMath.cropBox(it.width, it.height, l, t, r, b)) }
                    cropL = 0f; cropT = 0f; cropR = 0f; cropB = 0f
                },
                enabled = anyCrop && !busy,
            ) { Text("Apply crop") }

            SectionTitle("Resize")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 75, 50, 25, 10).forEach { p ->
                    FilterChip(selected = scalePct == p, onClick = { scalePct = p }, label = { Text("$p%") })
                }
            }
            Text("Applied when you save, so you can still change it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionTitle("Format & compression")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageOps.Format.values().forEach { f ->
                    FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label) })
                }
            }
            if (format.lossy) {
                Text("Quality: $quality", style = MaterialTheme.typography.labelLarge)
                Slider(value = quality.toFloat(), onValueChange = { quality = it.roundToInt() }, valueRange = 10f..100f)
                Text("Lower quality gives a smaller file. The size above is the real encoded size.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("PNG is lossless, so there is no quality setting; the file may be larger than a JPEG.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!format.keepsTransparency && bmp.hasAlpha()) {
                Text("This picture has transparent areas. JPEG cannot keep them, so they will become white. Choose PNG or WebP to keep transparency.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            val suggested = ImageMath.outputName(details?.name, format.extension)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        when (format) {
                            ImageOps.Format.JPEG -> saveJpeg.launch(suggested)
                            ImageOps.Format.PNG -> savePng.launch(suggested)
                            ImageOps.Format.WEBP -> saveWebp.launch(suggested)
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("Save as…") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val bytes = withContext(Dispatchers.Default) { ImageOps.encode(bmp, format, quality, scalePct) }
                                Actions.shareBytes(context, bytes, suggested, format.mime)
                            } catch (e: Exception) {
                                error = "Could not share: ${e.message ?: "unknown error"}"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("Share") }
            }
            OutlinedButton(
                onClick = { source?.let { load(it) } },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset to original") }
        }
    }
}

@Composable
private fun CropSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${value.roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onChange, valueRange = 0f..45f)
    }
}
