package com.omnitools.app.tools.qr

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.Actions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarcodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(BarcodeEncoder.Type.CODE_128) }
    var text by rememberSaveable { mutableStateOf("") }

    // Recomputed whenever the type or text changes; nothing is stored.
    var error: String? = null
    var bitmap: Bitmap? = null
    if (text.isNotEmpty()) {
        error = BarcodeEncoder.validate(type, if (type == BarcodeEncoder.Type.CODE_39) text.uppercase() else text)
        if (error == null) {
            bitmap = remember(type, text) {
                try { BarcodeEncoder.bitmap(type, text) } catch (e: BarcodeEncoder.BarcodeException) { error = e.message; null }
            }
        }
    }

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
                Toast.makeText(context, if (ok) "Barcode saved" else "Could not save the image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ScreenScaffold(title = "Barcode Generator", onBack = onBack) { padding ->
        ToolPage(padding) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BarcodeEncoder.Type.values().forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                }
            }
            Text(type.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextInput("Content", text, { text = it.take(100) })
            ErrorText(error)
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Generated ${type.label} barcode",
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { saveLauncher.launch("omnitools-barcode.png") }, modifier = Modifier.weight(1f)) { Text("Save") }
                    OutlinedButton(onClick = { Actions.shareImage(context, bmp, "omnitools-barcode.png") }, modifier = Modifier.weight(1f)) { Text("Share") }
                }
                Text(
                    "Made on this device. Check that a scanner can read it before printing in bulk.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
