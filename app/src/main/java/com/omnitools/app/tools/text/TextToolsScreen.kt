package com.omnitools.app.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.util.Actions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }

    fun run(action: () -> String) {
        try {
            output = action()
            message = null
            isError = false
        } catch (e: TextTools.TextToolException) {
            message = e.message
            isError = true
        } catch (e: Exception) {
            message = "Something went wrong: ${e.message}"
            isError = true
        }
    }

    val stats = remember(input) { TextTools.stats(input) }

    ScreenScaffold(title = "Text Tools", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Input text") },
                minLines = 5,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${stats.words} words · ${stats.characters} characters (${stats.charactersNoSpaces} without spaces) · " +
                    "${stats.sentences} sentences · ${stats.paragraphs} paragraphs · ${stats.lines} lines",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Transform")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip("UPPERCASE") { run { TextTools.upper(input) } }
                Chip("lowercase") { run { TextTools.lower(input) } }
                Chip("Title Case") { run { TextTools.titleCase(input) } }
                Chip("Trim whitespace") { run { TextTools.removeExtraWhitespace(input) } }
                Chip("Remove duplicate lines") { run { TextTools.removeDuplicateLines(input) } }
                Chip("Sort A→Z") { run { TextTools.sortLines(input) } }
                Chip("Sort Z→A") { run { TextTools.sortLines(input, descending = true) } }
                Chip("Reverse") { run { TextTools.reverseText(input) } }
                Chip("Number lines") { run { TextTools.numberLines(input) } }
            }

            SectionTitle("Encode & format")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip("Base64 encode") { run { TextTools.base64Encode(input) } }
                Chip("Base64 decode") { run { TextTools.base64Decode(input) } }
                Chip("URL encode") { run { TextTools.urlEncode(input) } }
                Chip("URL decode") { run { TextTools.urlDecode(input) } }
                Chip("JSON format") { run { TextTools.jsonFormat(input) } }
                Chip("JSON minify") { run { TextTools.jsonMinify(input) } }
            }

            SectionTitle("Find & replace")
            OutlinedTextField(find, { find = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(replace, { replace = it }, label = { Text("Replace with") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ignoreCase, onCheckedChange = { ignoreCase = it })
                Text("Ignore case")
            }
            OutlinedButton(
                onClick = {
                    try {
                        val (text, count) = TextTools.findReplace(input, find, replace, ignoreCase)
                        output = text
                        message = "Replaced $count occurrence${if (count == 1) "" else "s"}."
                        isError = false
                    } catch (e: TextTools.TextToolException) {
                        message = e.message
                        isError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Replace all") }

            message?.let {
                Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }

            if (output.isNotEmpty()) {
                SectionTitle("Result")
                OutlinedTextField(
                    value = output,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 4,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { Actions.copy(context, "Result", output) }, modifier = Modifier.weight(1f)) { Text("Copy") }
                    OutlinedButton(onClick = { Actions.shareText(context, output) }, modifier = Modifier.weight(1f)) { Text("Share") }
                    OutlinedButton(onClick = { input = output }, modifier = Modifier.weight(1f)) { Text("Use as input") }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}
