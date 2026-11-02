package com.omnitools.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitools.app.data.Prefs
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import kotlinx.coroutines.launch

private enum class Confirm(val title: String, val body: String) {
    RECENTS("Clear recently used?", "This only clears the list on the home screen."),
    SCANS("Clear scan history?", "All saved QR and barcode scan results on this device will be deleted."),
}

@Composable
fun SettingsScreen(
    prefs: Prefs,
    themeMode: String,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Confirm?>(null) }

    ScreenScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            SectionTitle("Theme")
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("system" to "System", "dark" to "Dark", "light" to "Light").forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { scope.launch { prefs.setThemeMode(mode) } },
                        label = { Text(label) },
                    )
                }
            }

            SectionTitle("Your data")
            Text(
                "Everything OmniTools saves stays on this device. You can clear it here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            OutlinedButton(
                onClick = { confirm = Confirm.RECENTS },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            ) { Text("Clear recently used") }
            OutlinedButton(
                onClick = { confirm = Confirm.SCANS },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            ) { Text("Clear scan history") }

            SectionTitle("About")
            OutlinedButton(
                onClick = onOpenAbout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            ) { Text("About & privacy") }
        }
    }

    confirm?.let { c ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(c.title) },
            text = { Text(c.body) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (c) {
                            Confirm.RECENTS -> prefs.clearRecents()
                            Confirm.SCANS -> prefs.clearScans()
                        }
                    }
                    confirm = null
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}
