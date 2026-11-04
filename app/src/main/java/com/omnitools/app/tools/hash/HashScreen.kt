package com.omnitools.app.tools.hash

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.util.Actions
import com.omnitools.app.util.FileDetails
import com.omnitools.app.util.FileInfo
import com.omnitools.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun HashScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    ScreenScaffold(title = "Hash & File Info", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("File") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Text") })
            }
            if (tab == 0) FileTab() else TextTab()
            Text(
                "MD5 and SHA-1 are fine for spotting accidental changes but are broken against deliberate tampering; prefer SHA-256 or SHA-512 for verification.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileTab() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var details by remember { mutableStateOf<FileDetails?>(null) }
    var hashes by remember { mutableStateOf<Map<HashTool.Algo, String>?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var expected by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            uri = picked
            details = FileInfo.query(context, picked)
            hashes = null
            error = null
        }
    }

    // Hashing runs on a background thread and is cancelled automatically if the file changes or the screen closes.
    var startHash by remember { mutableStateOf(0) }
    LaunchedEffect(startHash) {
        val target = uri ?: return@LaunchedEffect
        if (startHash == 0) return@LaunchedEffect
        progress = 0f
        error = null
        val size = details?.sizeBytes
        try {
            hashes = withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openInputStream(target) ?: throw java.io.FileNotFoundException("Could not open the file.")
                stream.use {
                    HashTool.digestAll(it, onProgress = { read -> if (size != null && size > 0) progress = (read.toFloat() / size).coerceIn(0f, 1f) })
                }
            }
        } catch (e: java.io.IOException) {
            error = "Could not read the file: ${e.message ?: "unknown error"}"
        } catch (e: SecurityException) {
            error = "Android did not allow reading this file."
        }
        progress = null
    }

    Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (uri == null) "Choose a file" else "Choose another file") }

    details?.let { d ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoLine("Name", d.name ?: "Unknown")
                InfoLine("Extension", d.extension ?: "None")
                InfoLine("Size", d.sizeBytes?.let { "${Format.bytes(it)} ($it bytes)" } ?: "Unknown")
                InfoLine("Type", d.mimeType ?: "Unknown")
                InfoLine(
                    "Modified",
                    d.lastModified?.takeIf { it > 0 }?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
                        ?: "Not provided by this file source",
                )
            }
        }
        Button(onClick = { startHash++ }, enabled = progress == null, modifier = Modifier.fillMaxWidth()) { Text("Calculate checksums") }
    }

    progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
    ErrorText(error)

    hashes?.let { map ->
        TextInput("Compare with a checksum (optional)", expected, { expected = it })
        val trimmed = expected.filterNot { it.isWhitespace() }
        if (trimmed.isNotEmpty()) {
            val hit = map.entries.firstOrNull { HashTool.matches(trimmed, it.value) }
            Text(
                if (hit != null) "Match: this file's ${hit.key.label} equals the checksum you entered." else "No match: the checksum you entered does not equal any of the values below.",
                color = if (hit != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        map.forEach { (algo, value) -> HashRow(algo.label, value) }
    }
}

@Composable
private fun TextTab() {
    var text by remember { mutableStateOf("") }
    TextInput("Text to hash (UTF-8)", text, { text = it }, singleLine = false, minLines = 4)
    if (text.isNotEmpty()) HashTool.Algo.values().forEach { HashRow(it.label, HashTool.digestText(it, text)) }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HashRow(label: String, value: String) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(onClick = { Actions.copy(context, label, value) }) { Text("Copy") }
        }
    }
}
