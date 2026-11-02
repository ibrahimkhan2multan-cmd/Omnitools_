package com.omnitools.app.tools.qr

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.Actions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class QrMode(val label: String) { TEXT("Text"), LINK("Link"), WIFI("Wi-Fi"), CONTACT("Contact") }

@Composable
fun QrGenerateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(QrMode.TEXT) }
    var text by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf(QrPayloads.WifiSecurity.WPA) }
    var hidden by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var payload by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bmp = bitmap
        if (uri != null && bmp != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                Toast.makeText(context, if (ok) "QR image saved" else "Could not save the image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun buildPayload(): String = when (mode) {
        QrMode.TEXT -> text
        QrMode.LINK -> {
            val a = UrlSafety.analyze(link)
            require(a.isUri && a.canOpen) { "Enter a full web link starting with http:// or https://" }
            link.trim()
        }
        QrMode.WIFI -> QrPayloads.wifi(ssid, password, security, hidden)
        QrMode.CONTACT -> QrPayloads.contact(name, phone, email, org)
    }

    ScreenScaffold(title = "QR Code Generator", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QrMode.values().forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m; error = null }, label = { Text(m.label) })
                }
            }

            when (mode) {
                QrMode.TEXT -> Field("Text", text, { text = it }, singleLine = false)
                QrMode.LINK -> Field("Web link", link, { link = it }, keyboard = KeyboardType.Uri)
                QrMode.WIFI -> {
                    Field("Network name (SSID)", ssid, { ssid = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QrPayloads.WifiSecurity.values().forEach { s ->
                            FilterChip(selected = security == s, onClick = { security = s }, label = { Text(s.label) })
                        }
                    }
                    if (security != QrPayloads.WifiSecurity.NONE) Field("Password", password, { password = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hidden, onCheckedChange = { hidden = it })
                        Text("Hidden network")
                    }
                }
                QrMode.CONTACT -> {
                    Field("Name", name, { name = it })
                    Field("Phone", phone, { phone = it }, keyboard = KeyboardType.Phone)
                    Field("Email", email, { email = it }, keyboard = KeyboardType.Email)
                    Field("Organization (optional)", org, { org = it })
                }
            }

            Button(
                onClick = {
                    error = null
                    scope.launch {
                        try {
                            val p = buildPayload()
                            val bmp = withContext(Dispatchers.Default) { QrEncoder.bitmap(p) }
                            payload = p
                            bitmap = bmp
                        } catch (e: IllegalArgumentException) {
                            bitmap = null
                            error = e.message
                        } catch (e: QrEncoder.QrException) {
                            bitmap = null
                            error = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Generate QR code") }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Generated QR code",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(260.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { saveLauncher.launch("omnitools-qr.png") }, modifier = Modifier.weight(1f)) { Text("Save") }
                    OutlinedButton(onClick = { Actions.shareImage(context, bmp) }, modifier = Modifier.weight(1f)) { Text("Share") }
                    OutlinedButton(onClick = { Actions.copy(context, "QR content", payload) }, modifier = Modifier.weight(1f)) { Text("Copy text") }
                }
                Text(
                    "Generated on this device. Nothing is uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
