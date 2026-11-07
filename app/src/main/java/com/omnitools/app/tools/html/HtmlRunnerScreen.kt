package com.omnitools.app.tools.html

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ScreenScaffold

private const val EXAMPLE_HTML = "<h1>Hello!</h1>\n<p id=\"out\">Tap the button.</p>\n<button onclick=\"hello()\">Click me</button>"
private const val EXAMPLE_CSS = "body { font-family: sans-serif; padding: 16px; }\nh1 { color: #4f8cff; }\nbutton { padding: 10px 16px; font-size: 16px; }"
private const val EXAMPLE_JS = "let n = 0;\nfunction hello() {\n  n++;\n  document.getElementById('out').textContent = 'Clicked ' + n + ' time(s)';\n  console.log('clicked', n);\n}"

@Composable
fun HtmlRunnerScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(RunnerMode.WEB) }
    var tab by remember { mutableStateOf(0) }
    var html by remember { mutableStateOf("") }
    var css by remember { mutableStateOf("") }
    var js by remember { mutableStateOf("") }
    var stdin by remember { mutableStateOf("") }
    val codes = remember { mutableStateMapOf<RunnerMode, String>() }
    var runId by remember { mutableStateOf(0) }
    var document by remember { mutableStateOf("") }
    var runtimeRun by remember { mutableStateOf(false) }
    // Editor tabs first, then Preview, then Updates.
    val editorTabs = when (mode) {
        RunnerMode.WEB -> listOf("HTML", "CSS", "JavaScript")
        RunnerMode.PYTHON -> listOf("Code", "Input")
        else -> listOf("Code")
    }
    val previewTab = editorTabs.size
    val updatesTab = previewTab + 1

    fun run() {
        document = RunnerDocs.build(mode, html, css, js, codes[mode].orEmpty(), stdin)
        runtimeRun = mode.runtime
        runId++
        tab = previewTab
    }

    ScreenScaffold(
        title = "Code Runner",
        onBack = onBack,
        actions = { IconButton(onClick = ::run) { Icon(Icons.Filled.PlayArrow, contentDescription = "Run") } },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RunnerMode.values().forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m; tab = 0 }, label = { Text(m.label) })
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (editorTabs + "Preview" + "Updates").forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { if (i == previewTab) run() else tab = i }, label = { Text(label) })
                }
            }
            when {
                tab == previewTab -> HtmlPreviewPanel(document, runId, Modifier.weight(1f).fillMaxWidth(), runtime = runtimeRun)
                tab == updatesTab -> UpdatesPanel(Modifier.weight(1f).fillMaxWidth())
                else -> {
                    val value: String
                    val setValue: (String) -> Unit
                    val label: String
                    if (mode == RunnerMode.WEB) {
                        when (tab) {
                            0 -> { value = html; setValue = { html = it }; label = "HTML (a fragment or a whole page)" }
                            1 -> { value = css; setValue = { css = it }; label = "CSS" }
                            else -> { value = js; setValue = { js = it }; label = "JavaScript" }
                        }
                    } else if (mode == RunnerMode.PYTHON && tab == 1) {
                        value = stdin; setValue = { stdin = it.take(20_000) }; label = "Input for input(), one line per call"
                    } else {
                        value = codes[mode].orEmpty()
                        setValue = { codes[mode] = it.take(200_000) }
                        label = mode.label
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = setValue,
                        label = { Text(label) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            if (mode == RunnerMode.WEB) { html = EXAMPLE_HTML; css = EXAMPLE_CSS; js = EXAMPLE_JS } else codes[mode] = mode.example
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) { Text("Fill in an example") }
                    Text(
                        "Runs on this device in a locked-down page: no internet, no access to your files or to this app. " +
                            "Tap \u25B6 to run. HTML, JavaScript, TypeScript, Python, SQL and Lua run offline. Java, C and other compiled languages are not supported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }
}

/** The update log, with Copy and Share so it can be sent to someone. */
@Composable
private fun UpdatesPanel(modifier: Modifier = Modifier) {
    val text = remember { RunnerChangelog.asText() }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CopyShareRow(text, "OmniTools update log")
        RunnerChangelog.entries.forEach { e ->
            Text("${e.version}  \u00B7  ${e.date}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(e.title, style = MaterialTheme.typography.titleMedium)
            e.items.forEach { Text("\u2022 $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Text(RunnerChangelog.LIMITS, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
