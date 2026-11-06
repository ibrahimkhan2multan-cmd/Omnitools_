package com.omnitools.app.tools.qr

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage

@Composable
fun QrInspectorScreen(onBack: () -> Unit) {
    var raw by rememberSaveable { mutableStateOf("") }
    val info = if (raw.isBlank()) null else QrInspector.inspect(raw.trim())
    ScreenScaffold(title = "QR Content Inspector", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Paste text read from a QR code", raw, { raw = it.take(10_000) }, singleLine = false, minLines = 3)
            info?.let {
                ResultCard("Type", it.type)
                it.fields.forEach { (k, v) -> ResultCard(k, v) }
                it.warnings.forEach { w -> Text("Warning: $w", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                CopyShareRow(raw.trim(), "QR content")
            }
            Text("Shows what a code contains before you act on it. Nothing is opened or sent anywhere.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
