package com.omnitools.app.tools.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.omnitools.app.ui.components.rememberText
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.Choice
import com.omnitools.app.ui.components.ChipRow
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun JsonScreen(onBack: () -> Unit) {
    var input by rememberText()
    var mode by rememberSaveable { mutableStateOf(0) } // 0 pretty, 1 compact
    var sort by rememberSaveable { mutableStateOf(false) }
    val computed = remember<Pair<String?, String?>>(input, mode, sort) {
        if (input.isBlank()) null to null
        else try { Json.format(input, if (mode == 0) 2 else 0, sort) to null } catch (e: Json.JsonException) { null to e.message }
    }
    val output = computed.first
    val error = computed.second
    ScreenScaffold(title = "JSON Formatter", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Paste JSON", input, { input = it.take(300_000) }, singleLine = false, minLines = 6)
            ChipRow {
                Choice(mode == 0, "Pretty") { mode = 0 }
                Choice(mode == 1, "Compact") { mode = 1 }
                Choice(sort, "Sort keys") { sort = !sort }
            }
            ErrorText(error)
            val shownJson = output
            if (shownJson != null) {
                ResultCard("Valid JSON", if (shownJson.length > 20000) shownJson.take(20000) + "\n… (shortened here; Copy gives everything)" else shownJson)
                CopyShareRow(shownJson, "JSON")
            } else if (input.isBlank()) Note("Checks that your JSON is strictly valid and shows exactly where it is not.")
        }
    }
}

@Composable
fun DiffScreen(onBack: () -> Unit) {
    var a by rememberText()
    var b by rememberText()
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var ignoreSpace by rememberSaveable { mutableStateOf(false) }
    val computed = remember<Pair<TextDiff.Result?, String?>>(a, b, ignoreCase, ignoreSpace) {
        if (a.isEmpty() && b.isEmpty()) null to null
        else try { TextDiff.diff(a, b, ignoreCase, ignoreSpace) to null } catch (e: IllegalArgumentException) { null to e.message }
    }
    val result = computed.first
    val error = computed.second
    ScreenScaffold(title = "Text Compare", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Original text", a, { a = it.take(200_000) }, singleLine = false, minLines = 4)
            TextInput("Changed text", b, { b = it.take(200_000) }, singleLine = false, minLines = 4)
            ChipRow {
                Choice(ignoreCase, "Ignore case") { ignoreCase = !ignoreCase }
                Choice(ignoreSpace, "Ignore spaces") { ignoreSpace = !ignoreSpace }
            }
            ErrorText(error)
            result?.let { r ->
                if (r.added == 0 && r.removed == 0) Text("The texts are the same.", style = MaterialTheme.typography.titleMedium)
                else Text("${r.added} added, ${r.removed} removed, ${r.same} unchanged", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    r.lines.take(600).forEach { line ->
                        val (bg, mark) = when (line.type) {
                            TextDiff.Type.ADDED -> Color(0x3322C55E) to "+"
                            TextDiff.Type.REMOVED -> Color(0x33EF4444) to "-"
                            TextDiff.Type.SAME -> Color.Transparent to " "
                        }
                        Text(
                            "$mark ${line.text}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (r.lines.size > 600) Note("Showing the first 600 lines.")
            }
        }
    }
}

@Composable
fun JwtScreen(onBack: () -> Unit) {
    var input by rememberText()
    var d: Jwt.Decoded? = null
    var error: String? = null
    if (input.isNotBlank()) { try { d = Jwt.decode(input) } catch (e: IllegalArgumentException) { error = e.message } }
    ScreenScaffold(title = "JWT Decoder", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Paste a token (xxxxx.yyyyy.zzzzz)", input, { input = it.take(20000) }, singleLine = false, minLines = 3)
            ErrorText(error)
            d?.let {
                ResultCard("Header", it.header)
                ResultCard("Payload", it.payload)
                it.times.forEach { (k, v) -> ResultCard(k, v) }
                ResultCard("Signature", "${it.signatureBytes} bytes. NOT verified.")
            }
            Note("Decoding happens on your phone. A token's contents are readable by anyone who has it, so treat tokens like passwords. The signature cannot be checked here because that needs the secret key.")
        }
    }
}

@Composable
fun CsvJsonScreen(onBack: () -> Unit) {
    var input by rememberText()
    var toJson by rememberSaveable { mutableStateOf(true) }
    var delim by rememberSaveable { mutableStateOf(0) }
    val d = charArrayOf(',', ';', '\t')[delim]
    val computed = remember<Pair<String?, String?>>(input, toJson, delim) {
        if (input.isBlank()) null to null
        else try { (if (toJson) CsvJson.csvToJson(input, d) else CsvJson.jsonToCsv(input, d)) to null }
        catch (e: IllegalArgumentException) { null to e.message } catch (e: Json.JsonException) { null to e.message }
    }
    val output = computed.first
    val error = computed.second
    ScreenScaffold(title = "CSV ⇄ JSON", onBack = onBack) { padding ->
        ToolPage(padding) {
            ChipRow {
                Choice(toJson, "CSV to JSON") { toJson = true }
                Choice(!toJson, "JSON to CSV") { toJson = false }
            }
            ChipRow {
                Choice(delim == 0, "Comma") { delim = 0 }
                Choice(delim == 1, "Semicolon") { delim = 1 }
                Choice(delim == 2, "Tab") { delim = 2 }
            }
            TextInput(if (toJson) "CSV (first row = column names)" else "JSON array of objects", input, { input = it.take(300_000) }, singleLine = false, minLines = 6)
            ErrorText(error)
            output?.let {
                ResultCard("Result", if (it.length > 20000) it.take(20000) + "\n… (shortened here; Copy gives everything)" else it)
                CopyShareRow(it, "Converted")
            }
        }
    }
}

@Composable
fun ChmodScreen(onBack: () -> Unit) {
    var octal by rememberSaveable { mutableStateOf("755") }
    val result = try { Chmod.fromOctal(octal) } catch (e: IllegalArgumentException) { null }
    val bits = Chmod.toBits(octal.filter { it in '0'..'7' }.takeLast(3).ifEmpty { "0" })
    ScreenScaffold(title = "Chmod Calculator", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Octal permissions (e.g. 755 or 4755)", octal, { octal = it.filter { c -> c in '0'..'7' }.take(4) })
            if (result == null) ErrorText("Use three or four octal digits, like 755 or 4755.")
            result?.let { r ->
                ResultCard("Symbolic", r.symbolic)
                r.lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                CopyShareRow("chmod ${r.octal} file", "chmod command")
            }
            SectionTitle("Or tick the permissions")
            val labels = listOf("Owner", "Group", "Others")
            val names = listOf("Read", "Write", "Execute")
            for (row in 0..2) {
                Text(labels[row], style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..2) {
                        Checkbox(checked = bits[row * 3 + col], onCheckedChange = { on ->
                            val nb = bits.toMutableList(); nb[row * 3 + col] = on
                            octal = Chmod.fromBits(nb)
                        })
                        Text(names[col], style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
