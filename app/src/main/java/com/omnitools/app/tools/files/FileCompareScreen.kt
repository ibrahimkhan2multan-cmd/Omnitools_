package com.omnitools.app.tools.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.FileInfo
import com.omnitools.app.util.Format
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FileDigest {
    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun sha256(input: java.io.InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        return hex(md.digest())
    }
}

private class Picked(val name: String, val size: Long?, val mime: String?, val modified: Long?, val sha: String)

@Composable
fun FileCompareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var a by remember { mutableStateOf<Picked?>(null) }
    var b by remember { mutableStateOf<Picked?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(uri: Uri, into: (Picked?) -> Unit) {
        busy = true; error = null
        scope.launch {
            val p = withContext(Dispatchers.IO) {
                try {
                    val d = FileInfo.query(context, uri)
                    val sha = context.contentResolver.openInputStream(uri)?.use { FileDigest.sha256(it) } ?: return@withContext null
                    Picked(d.name ?: "(unnamed)", d.sizeBytes, d.mimeType, d.lastModified, sha)
                } catch (e: Exception) { null }
            }
            if (p == null) error = "That file could not be read."
            into(p)
            busy = false
        }
    }
    val pickA = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) load(it) { p -> a = p } }
    val pickB = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) load(it) { p -> b = p } }
    val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    @Composable
    fun Details(title: String, p: Picked) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        ResultCard("Name", p.name)
        ResultCard("Size", p.size?.let { "${Format.bytes(it)} ($it bytes)" } ?: "Unknown")
        ResultCard("Type", p.mime ?: "Unknown")
        ResultCard("Modified", p.modified?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "Unknown")
        ResultCard("SHA-256", p.sha)
    }

    ScreenScaffold(title = "File Info & Compare", onBack = onBack) { padding ->
        ToolPage(padding) {
            Button(onClick = { pickA.launch(arrayOf("*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (a == null) "Choose first file" else "Change first file") }
            OutlinedButton(onClick = { pickB.launch(arrayOf("*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (b == null) "Choose second file (optional)" else "Change second file") }
            if (busy) CircularProgressIndicator()
            ErrorText(error)
            val x = a; val y = b
            if (x != null && y != null) {
                val same = x.sha == y.sha
                Text(
                    if (same) "The two files are identical." else "The two files are different.",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (same) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            x?.let { Details(if (y != null) "First file" else "File", it) }
            y?.let { Details("Second file", it) }
            if (x != null) CopyShareRow("${x.name}\nSHA-256: ${x.sha}", "Checksum")
            if (x == null) Text("Choose a file to see its details and SHA-256 checksum. Add a second file to see whether they match exactly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
