package com.omnitools.app.tools.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.FileDetails
import com.omnitools.app.util.FileInfo
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfReaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uri by remember { mutableStateOf<Uri?>(null) }
    var details by remember { mutableStateOf<FileDetails?>(null) }
    var session by remember { mutableStateOf<PdfSession?>(null) }
    var page by remember { mutableStateOf(0) }
    var zoom by remember { mutableStateOf(1f) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var jumpText by remember { mutableStateOf("") }

    // Always release the file when the screen closes or another document replaces this one.
    DisposableEffect(session) { onDispose { session?.close() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            scope.launch {
                loading = true
                error = null
                try {
                    val opened = withContext(Dispatchers.IO) { PdfSession.open(context, picked) }
                    uri = picked
                    details = withContext(Dispatchers.IO) { FileInfo.query(context, picked) }
                    page = 0
                    zoom = 1f
                    pageBitmap = null
                    session = opened
                } catch (e: PdfSession.OpenException) {
                    error = e.message
                }
                loading = false
            }
        }
    }

    ScreenScaffold(
        title = "PDF Reader",
        onBack = onBack,
        actions = {
            val u = uri
            if (u != null) {
                IconButton(onClick = {
                    val view = Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/pdf")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(Intent.createChooser(view, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        error = "No other app could open this PDF."
                    }
                }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in another app") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("application/pdf")) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (session == null) "Choose a PDF" else "Choose another PDF")
                }
                ErrorText(error)
            }

            val s = session
            if (s == null) {
                if (loading) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                Text(
                    "Open a PDF to read it, move between pages and zoom. PDFs stay on your device.\n\n" +
                        "Not supported by the built-in PDF engine: searching the text, filling forms, and password-protected files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                return@Column
            }

            // Document details
            Text(
                "${details?.name ?: "PDF"} · ${s.pageCount} page${if (s.pageCount == 1) "" else "s"}" +
                    (details?.sizeBytes?.let { " · ${Format.bytes(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Navigation and zoom
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = { if (page > 0) page-- }, enabled = page > 0) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous page") }
                Text("${page + 1} / ${s.pageCount}", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { if (page < s.pageCount - 1) page++ }, enabled = page < s.pageCount - 1) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next page")
                }
                OutlinedTextField(
                    value = jumpText,
                    onValueChange = { jumpText = it.filter(Char::isDigit).take(5) },
                    placeholder = { Text("Go to") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        jumpText.toIntOrNull()?.let { page = (it - 1).coerceIn(0, s.pageCount - 1) }
                        jumpText = ""
                    }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out") }
                IconButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(4f) }, enabled = zoom < 4f) { Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in") }
            }

            // Page view: the area is bounded, so vertical and horizontal scrolling can live inside it safely.
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val density = LocalDensity.current
                val viewportPx = with(density) { maxWidth.roundToPx() }
                val requestedWidth = (viewportPx * zoom).toInt()

                LaunchedEffect(s, page, requestedWidth) {
                    loading = true
                    try {
                        pageBitmap = withContext(Dispatchers.IO) { s.render(page, requestedWidth) }
                        error = null
                    } catch (e: OutOfMemoryError) {
                        error = "Not enough memory to show this page at this zoom. Zoom out."
                    } catch (e: Exception) {
                        error = "This page could not be drawn."
                    }
                    loading = false
                }

                val bmp = pageBitmap
                if (bmp != null) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page ${page + 1} of ${s.pageCount}",
                            modifier = Modifier.size(
                                width = with(density) { bmp.width.toDp() },
                                height = with(density) { bmp.height.toDp() },
                            ),
                        )
                    }
                }
                if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}
