package com.omnitools.app.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitools.app.data.Prefs
import com.omnitools.app.tools.html.RunnerChangelog
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.cardBorder

/** What's new in each build, with Copy and Share so it can be sent to someone. Opened from the Home header. */
@Composable
fun UpdatesScreen(prefs: Prefs, onBack: () -> Unit) {
    val text = remember { RunnerChangelog.asText() }
    // Opening the log clears the "new" dot on the Home button.
    LaunchedEffect(Unit) { prefs.setSeenUpdate(RunnerChangelog.latestVersion) }

    ScreenScaffold(title = "Update log", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CopyShareRow(text, "OmniTools update log")
            RunnerChangelog.entries.forEach { e ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = cardBorder(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${e.version}  ·  ${e.date}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(e.title, style = MaterialTheme.typography.titleMedium)
                        e.items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            Text(RunnerChangelog.LIMITS, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
