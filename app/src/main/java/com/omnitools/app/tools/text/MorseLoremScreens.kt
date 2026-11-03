package com.omnitools.app.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.NumberField
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.Actions

@Composable
fun MorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var toMorse by rememberSaveable { mutableStateOf(true) }
    var input by rememberSaveable { mutableStateOf("") }

    val encoded = if (toMorse && input.isNotBlank()) Morse.encode(input) else null
    val output = if (toMorse) encoded?.morse else if (input.isNotBlank()) Morse.decode(input) else null

    ScreenScaffold(title = "Morse Code", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = toMorse, onClick = { toMorse = true; input = "" }, label = { Text("Text → Morse") })
                FilterChip(selected = !toMorse, onClick = { toMorse = false; input = "" }, label = { Text("Morse → Text") })
            }
            TextInput(if (toMorse) "Text" else "Morse (dots and dashes)", input, { input = it.take(2000) }, singleLine = false, minLines = 3)
            if (encoded != null && encoded.skipped.isNotEmpty()) {
                ErrorText("No Morse code for: " + encoded.skipped.joinToString(" ") + " (skipped)")
            }
            if (!output.isNullOrEmpty()) {
                ResultCard(if (toMorse) "Morse code" else "Text", output)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { Actions.copy(context, "Result", output) }, modifier = Modifier.weight(1f)) { Text("Copy") }
                    OutlinedButton(onClick = { Actions.shareText(context, output) }, modifier = Modifier.weight(1f)) { Text("Share") }
                }
            }
            Text(
                if (toMorse) "Letters are separated by a space and words by  /  . Letters, digits and common punctuation are supported."
                else "Separate letters with a space and words with  /  (or three spaces). Unknown codes show as ?.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class LoremKind(val label: String, val max: Int) {
    PARAGRAPHS("Paragraphs", Lorem.MAX_PARAGRAPHS),
    SENTENCES("Sentences", 200),
    WORDS("Words", Lorem.MAX_WORDS),
}

@Composable
fun LoremScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var kind by rememberSaveable { mutableStateOf(LoremKind.PARAGRAPHS) }
    var count by rememberSaveable { mutableStateOf("3") }
    var text by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun generate() {
        val n = count.toIntOrNull()
        if (n == null) {
            error = "Enter a number."
            return
        }
        try {
            text = when (kind) {
                LoremKind.PARAGRAPHS -> Lorem.paragraphs(n)
                LoremKind.SENTENCES -> Lorem.sentences(n)
                LoremKind.WORDS -> Lorem.words(n)
            }
            error = null
        } catch (e: IllegalArgumentException) {
            error = e.message
        }
    }

    ScreenScaffold(title = "Lorem Ipsum Generator", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoremKind.values().forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
                }
            }
            NumberField("How many (up to ${kind.max})", count, { count = it.filter(Char::isDigit).take(5) })
            Button(onClick = ::generate, modifier = Modifier.fillMaxWidth()) { Text("Generate") }
            ErrorText(error)
            if (text.isNotEmpty()) {
                ResultCard("Placeholder text", text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { Actions.copy(context, "Lorem ipsum", text) }, modifier = Modifier.weight(1f)) { Text("Copy") }
                    OutlinedButton(onClick = { Actions.shareText(context, text) }, modifier = Modifier.weight(1f)) { Text("Share") }
                }
            }
        }
    }
}
