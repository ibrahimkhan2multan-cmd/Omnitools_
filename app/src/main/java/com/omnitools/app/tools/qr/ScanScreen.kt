package com.omnitools.app.tools.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitools.app.data.Prefs
import com.omnitools.app.data.ScanEntry
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.Actions
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun ScanScreen(prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history by prefs.scanHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        askedOnce = true
    }

    var result by remember { mutableStateOf<ScanEntry?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var confirmOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = if (showHistory) "Scan history" else "Scanner",
        onBack = { if (showHistory) showHistory = false else onBack() },
        actions = {
            if (showHistory) {
                if (history.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history")
                    }
                }
            } else {
                IconButton(onClick = { showHistory = true }) {
                    Icon(Icons.Filled.History, contentDescription = "Scan history")
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                showHistory -> HistoryList(
                    history = history,
                    onPick = { result = it; showHistory = false },
                    onDelete = { entry -> scope.launch { prefs.deleteScan(entry.timestamp) } },
                )

                !hasPermission -> PermissionPrompt(
                    askedOnce = askedOnce,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )

                else -> {
                    CameraScanner(
                        active = result == null,
                        onDetected = { format, value ->
                            if (result == null) {
                                val entry = ScanEntry(System.currentTimeMillis(), format, value)
                                result = entry
                                scope.launch { prefs.addScan(entry) }
                            }
                        },
                        onError = { cameraError = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (result == null) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(240.dp)
                                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                        )
                        Text(
                            cameraError ?: "Point the camera at a QR code or barcode",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                        )
                    }
                }
            }

            val current = result
            if (current != null && !showHistory) {
                ResultCard(
                    entry = current,
                    onCopy = { Actions.copy(context, "Scan result", current.text) },
                    onShare = { Actions.shareText(context, current.text) },
                    onOpen = { confirmOpen = true },
                    onDismiss = { result = null },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    val current = result
    if (confirmOpen && current != null) {
        val analysis = UrlSafety.analyze(current.text)
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = { Text("Open this link?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Destination website: ${analysis.host ?: "unknown"}", fontWeight = FontWeight.SemiBold)
                    Text(current.text, style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    analysis.warnings.forEach {
                        Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpen = false
                    Actions.openWebUrl(context, current.text)
                }) { Text(if (analysis.warnings.isEmpty()) "Open" else "Open anyway") }
            },
            dismissButton = { TextButton(onClick = { confirmOpen = false }) { Text("Cancel") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear scan history?") },
            text = { Text("All saved scan results on this device will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch { prefs.clearScans() }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PermissionPrompt(askedOnce: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera access needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "OmniTools uses the camera only while this scanner is open, to read codes on your device. " +
                "No pictures are saved or sent anywhere.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onRequest) { Text(if (askedOnce) "Try again" else "Allow camera") }
        if (askedOnce) {
            Text(
                "If the system no longer shows the permission dialog, enable Camera in the app settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) { Text("Open app settings") }
        }
        Text(
            "You can still use every other tool, and the QR generator works without the camera.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun ResultCard(
    entry: ScanEntry,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val analysis = UrlSafety.analyze(entry.text)
    Card(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.format, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(entry.text, style = MaterialTheme.typography.bodyLarge, maxLines = 8, overflow = TextOverflow.Ellipsis)
            if (analysis.isUri) {
                analysis.host?.let { Text("Website: $it", style = MaterialTheme.typography.bodySmall) }
                analysis.warnings.forEach {
                    Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Copy")
                }
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Share")
                }
                if (analysis.canOpen) {
                    OutlinedButton(onClick = onOpen) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Open")
                    }
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
        }
    }
}

@Composable
private fun HistoryList(history: List<ScanEntry>, onPick: (ScanEntry) -> Unit, onDelete: (ScanEntry) -> Unit) {
    if (history.isEmpty()) {
        EmptyState("No scans yet. Scanned codes will appear here and stay on this device.")
        return
    }
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    LazyColumn(Modifier.fillMaxSize()) {
        items(history, key = { it.timestamp }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPick(entry) }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${entry.format} · ${format.format(Date(entry.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(entry) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete this scan")
                }
            }
        }
    }
}
