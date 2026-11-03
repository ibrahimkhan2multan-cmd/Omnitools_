package com.omnitools.app.tools.html

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitools.app.ui.components.ScreenScaffold

private const val EXAMPLE_HTML = "<h1>Hello!</h1>\n<p id=\"out\">Tap the button.</p>\n<button onclick=\"hello()\">Click me</button>"
private const val EXAMPLE_CSS = "body { font-family: sans-serif; padding: 16px; }\nh1 { color: #4f8cff; }\nbutton { padding: 10px 16px; font-size: 16px; }"
private const val EXAMPLE_JS = "let n = 0;\nfunction hello() {\n  n++;\n  document.getElementById('out').textContent = 'Clicked ' + n + ' time(s)';\n  console.log('clicked', n);\n}"

@Composable
fun HtmlRunnerScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) } // 0 HTML, 1 CSS, 2 JS, 3 Preview
    var html by remember { mutableStateOf("") }
    var css by remember { mutableStateOf("") }
    var js by remember { mutableStateOf("") }
    var runId by remember { mutableStateOf(0) }
    var document by remember { mutableStateOf("") }

    fun run() {
        document = HtmlDocument.build(html, css, js)
        runId++
        tab = 3
    }

    ScreenScaffold(
        title = "HTML / JS Runner",
        onBack = onBack,
        actions = { IconButton(onClick = ::run) { Icon(Icons.Filled.PlayArrow, contentDescription = "Run") } },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HTML", "CSS", "JavaScript", "Preview").forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { if (i == 3) run() else tab = i }, label = { Text(label) })
                }
            }
            if (tab == 3) {
                HtmlPreviewPanel(document, runId, Modifier.weight(1f).fillMaxWidth())
            } else {
                val (value, setValue) = when (tab) {
                    0 -> html to { v: String -> html = v }
                    1 -> css to { v: String -> css = v }
                    else -> js to { v: String -> js = v }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = setValue,
                    label = { Text(listOf("HTML (a fragment or a whole page)", "CSS", "JavaScript")[tab]) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                )
                OutlinedButton(
                    onClick = { html = EXAMPLE_HTML; css = EXAMPLE_CSS; js = EXAMPLE_JS },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) { Text("Fill in an example") }
                Text(
                    "Runs on this device in a locked-down page: no internet, no access to your files or to this app. " +
                        "Tap ▶ to run.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
    }
}
