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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import android.util.LruCache
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
    var sizes by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var zoom by remember { mutableStateOf(1f) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var jumpText by remember { mutableStateOf("") }
    var needPassword by remember { mutableStateOf<Pair<Uri, Boolean>?>(null) } // the locked file, and whether the last try was wrong
    var passwordText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val cache = remember { PageCache() }

    // Always release the file when the screen closes or another document replaces this one.
    DisposableEffect(session) { onDispose { session?.close() } }

    fun openFile(picked: Uri, password: String?) {
        scope.launch {
            loading = true
            error = null
            try {
                val opened = withContext(Dispatchers.IO) { PdfSession.open(context, picked, password) }
                val info = withContext(Dispatchers.IO) { FileInfo.query(context, picked) }
                // Page sizes up front, so the list has the right height for every page before it is drawn.
                val measured = withContext(Dispatchers.IO) {
                    List(opened.pageCount) { i -> if (opened.pageCount > 3000) A4 else try { opened.pageSize(i) } catch (e: Exception) { A4 } }
                }
                cache.evictAll()
                uri = picked
                details = info
                zoom = 1f
                sizes = measured
                session = opened
                needPassword = null
                passwordText = ""
                listState.scrollToItem(0)
            } catch (e: PdfSession.PasswordRequired) {
                needPassword = picked to e.wrong
            } catch (e: PdfSession.OpenException) {
                error = e.message
            } catch (e: Throwable) {
                error = "This PDF could not be opened."
            }
            loading = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) { passwordText = ""; openFile(picked, null) }
    }

    needPassword?.let { (locked, wrong) ->
        AlertDialog(
            onDismissRequest = { needPassword = null },
            title = { Text("Password needed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (wrong) "That password is not correct. Try again." else "This PDF is locked. Enter its password to open it.")
                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { passwordText = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { openFile(locked, passwordText) }),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { openFile(locked, passwordText) }, enabled = passwordText.isNotEmpty()) { Text("Open") } },
            dismissButton = { TextButton(onClick = { needPassword = null; passwordText = "" }) { Text("Cancel") } },
        )
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
                // Some phones label PDFs as plain binary files, so those are offered too.
                Button(onClick = { picker.launch(arrayOf("application/pdf", "application/x-pdf", "application/octet-stream")) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (session == null) "Choose a PDF" else "Choose another PDF")
                }
                ErrorText(error)
            }

            val s = session
            if (s == null) {
                if (loading) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                Text(
                    "Open a PDF to read it: scroll through every page, zoom in, jump to a page. Pictures, scanned pages and password-protected files are supported. PDFs stay on your device.\n\n" +
                        "Not supported yet: searching the text and filling forms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
                return@Column
            }

            val page by remember { derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, s.pageCount - 1) } }

            Text(
                "${details?.name ?: "PDF"} · ${s.pageCount} page${if (s.pageCount == 1) "" else "s"}" +
                    (details?.sizeBytes?.let { " · ${Format.bytes(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = { scope.launch { listState.animateScrollToItem((page - 1).coerceAtLeast(0)) } }, enabled = page > 0) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous page") }
                Text("${page + 1} / ${s.pageCount}", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { scope.launch { listState.animateScrollToItem((page + 1).coerceAtMost(s.pageCount - 1)) } }, enabled = page < s.pageCount - 1) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next page")
                }
                OutlinedTextField(
                    value = jumpText,
                    onValueChange = { jumpText = it.filter(Char::isDigit).take(5) },
                    placeholder = { Text("Go to") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        jumpText.toIntOrNull()?.let { n -> scope.launch { listState.scrollToItem((n - 1).coerceIn(0, s.pageCount - 1)) } }
                        jumpText = ""
                    }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out") }
                IconButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(4f) }, enabled = zoom < 4f) { Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in") }
            }

            // All pages in one scrolling list. Only the pages on screen are drawn, and recently drawn ones are kept.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
                val density = LocalDensity.current
                val viewportPx = with(density) { maxWidth.roundToPx() }
                val widthPx = (viewportPx * zoom).toInt()
                Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                    LazyColumn(state = listState, modifier = Modifier.width(maxWidth * zoom), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.pageCount, key = { it }) { i ->
                            val (pw, ph) = sizes.getOrElse(i) { A4 }
                            PdfPageItem(s, i, widthPx, pw.toFloat() / ph.coerceAtLeast(1), cache)
                        }
                    }
                }
            }
        }
    }
}

private val A4 = 595 to 842

/** Recently drawn pages, limited by memory rather than by count. */
private class PageCache : LruCache<String, Bitmap>(64 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

@Composable
private fun PdfPageItem(s: PdfSession, index: Int, widthPx: Int, aspect: Float, cache: PageCache) {
    val key = "$index@$widthPx"
    val result by produceState<Any?>(cache.get(key), s, key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    s.render(index, widthPx).also { cache.put(key, it) }
                } catch (e: OutOfMemoryError) {
                    "Not enough memory to show this page at this zoom. Zoom out."
                } catch (e: Exception) {
                    "This page could not be drawn."
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(aspect.coerceIn(0.05f, 20f)).background(Color.White), contentAlignment = Alignment.Center) {
        when (val r = result) {
            is Bitmap -> Image(bitmap = r.asImageBitmap(), contentDescription = "Page ${index + 1}", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            is String -> Text(r, color = Color.Black, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            else -> CircularProgressIndicator()
        }
    }
}
