package com.omnitools.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import com.omnitools.app.ui.components.LinkRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import com.omnitools.app.ui.feedback.Cue
import com.omnitools.app.ui.feedback.Feedback
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.omnitools.app.ui.feedback.SoundTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitools.app.data.Prefs
import com.omnitools.app.tools.notes.NotesRepository
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import kotlinx.coroutines.launch

private enum class Confirm(val title: String, val body: String, val done: String) {
    RECENTS("Clear recently used?", "This only clears the list on the home screen.", "Recently used cleared"),
    FAVORITES("Remove all favorites?", "Your starred tools will be un-starred.", "Favorites cleared"),
    SCANS("Clear scan history?", "All saved QR and barcode scan results on this device will be deleted.", "Scan history cleared"),
    NOTES("Delete ALL notes?", "Every note in the Notepad will be permanently deleted. This cannot be undone.", "Notes deleted"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: Prefs,
    themeMode: String,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val saveScans by prefs.saveScans.collectAsStateWithLifecycle(initialValue = true)
    val showRecents by prefs.showRecents.collectAsStateWithLifecycle(initialValue = true)
    val calcDegrees by prefs.calcDegrees.collectAsStateWithLifecycle(initialValue = true)
    val sounds by prefs.sounds.collectAsStateWithLifecycle(initialValue = true)
    val haptics by prefs.haptics.collectAsStateWithLifecycle(initialValue = true)
    val soundTheme by prefs.soundTheme.collectAsStateWithLifecycle(initialValue = SoundTheme.CLASSIC)
    val soundVolume by prefs.soundVolume.collectAsStateWithLifecycle(initialValue = 0.8f)

    ScreenScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            SectionTitle("Appearance")
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("dark" to "Dark", "light" to "Light", "system" to "Follow phone").forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { scope.launch { prefs.setThemeMode(mode) } },
                        label = { Text(label) },
                    )
                }
            }

            SectionTitle("Calculator")
            Text(
                "Angle unit used by sin, cos and tan when you open the calculator.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = calcDegrees, onClick = { scope.launch { prefs.setCalcDegrees(true) } }, label = { Text("Degrees") })
                FilterChip(selected = !calcDegrees, onClick = { scope.launch { prefs.setCalcDegrees(false) } }, label = { Text("Radians") })
            }

            SectionTitle("Sound & touch")
            SwitchRow(
                title = "Sounds",
                subtitle = "Soft click when you tap, and tones when code runs or something is copied.",
                checked = sounds,
                onChange = { scope.launch { prefs.setSounds(it) }; if (it) { Feedback.soundOn = true; Feedback.play(Cue.TOGGLE) } },
            )
            if (sounds) {
                Text("Sound theme", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundTheme.values().forEach { t ->
                        FilterChip(
                            selected = soundTheme == t,
                            onClick = {
                                scope.launch { prefs.setSoundTheme(t) }
                                Feedback.theme = t       // applied at once so the preview already uses it
                                Feedback.play(Cue.SUCCESS)
                            },
                            label = { Text(t.label) },
                        )
                    }
                }
                Text(soundTheme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Volume", style = MaterialTheme.typography.titleSmall)
                androidx.compose.material3.Slider(
                    value = soundVolume,
                    onValueChange = { Feedback.volume = it; scope.launch { prefs.setSoundVolume(it) } },
                    onValueChangeFinished = { Feedback.play(Cue.CLICK) },
                    modifier = Modifier.semantics { contentDescription = "Sound volume" },
                )
                Text("Try the sounds", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Click" to Cue.CLICK, "Switch" to Cue.TOGGLE, "Run" to Cue.RUN, "Done" to Cue.SUCCESS, "Error" to Cue.ERROR, "Back" to Cue.BACK).forEach { (label, cue) ->
                        OutlinedButton(onClick = { Feedback.play(cue) }) { Text(label) }
                    }
                }
            }
            SwitchRow(
                title = "Vibration",
                subtitle = "A tiny tick on taps.",
                checked = haptics,
                onChange = { scope.launch { prefs.setHaptics(it) } },
            )

            SectionTitle("History & privacy")
            SwitchRow(
                title = "Save scan history",
                subtitle = "Keep a list of scanned codes on this device. Turning this off does not delete existing history.",
                checked = saveScans,
                onChange = { scope.launch { prefs.setSaveScans(it) } },
            )
            SwitchRow(
                title = "Show recently used tools",
                subtitle = "Show the \"Recently used\" section on the home screen.",
                checked = showRecents,
                onChange = { scope.launch { prefs.setShowRecents(it) } },
            )

            SectionTitle("Your data")
            Text(
                "Everything OmniTools saves stays on this device. You can delete it here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Confirm.values().forEach { c ->
                OutlinedButton(
                    onClick = { confirm = c },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        when (c) {
                            Confirm.RECENTS -> "Clear recently used"
                            Confirm.FAVORITES -> "Clear favorites"
                            Confirm.SCANS -> "Clear scan history"
                            Confirm.NOTES -> "Delete all notes"
                        }
                    )
                }
            }

            SectionTitle("About")
            LinkRow(
                icon = Icons.Filled.Info,
                title = "About & privacy",
                subtitle = "Version, permissions and privacy policy",
                onClick = onOpenAbout,
            )
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
                            Confirm.FAVORITES -> prefs.clearFavorites()
                            Confirm.SCANS -> prefs.clearScans()
                            Confirm.NOTES -> NotesRepository(context.applicationContext).deleteAll()
                        }
                        Toast.makeText(context, c.done, Toast.LENGTH_SHORT).show()
                    }
                    confirm = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
