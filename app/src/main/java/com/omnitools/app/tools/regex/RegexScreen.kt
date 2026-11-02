package com.omnitools.app.tools.regex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegexScreen(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(false) }
    var multiline by remember { mutableStateOf(false) }
    var dotAll by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RegexTester.Result?>(null) }

    // Re-run shortly after typing stops, on a background thread, with the tester's hard time limit.
    LaunchedEffect(pattern, input, ignoreCase, multiline, dotAll) {
        if (pattern.isEmpty()) {
            result = null
            return@LaunchedEffect
        }
        delay(300)
        result = withContext(Dispatchers.Default) { RegexTester.run(pattern, input, ignoreCase, multiline, dotAll) }
    }

    ScreenScaffold(title = "Regex Tester", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextInput("Pattern (Java regex syntax)", pattern, { pattern = it })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text("Ignore case") })
                FilterChip(selected = multiline, onClick = { multiline = !multiline }, label = { Text("Multiline ^ $") })
                FilterChip(selected = dotAll, onClick = { dotAll = !dotAll }, label = { Text("Dot matches newline") })
            }
            TextInput("Text to test", input, { input = it }, singleLine = false, minLines = 5)

            when (val r = result) {
                null -> Text(
                    "Matches appear here. Work is limited to ${RegexTester.MAX_INPUT} characters and 1 second.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is RegexTester.Result.Error -> ErrorText(r.message)
                is RegexTester.Result.Ok -> {
                    Text(
                        if (r.matches.isEmpty()) "No matches." else "${r.matches.size}${if (r.truncated) "+" else ""} match${if (r.matches.size == 1) "" else "es"}" +
                            if (r.truncated) " (showing the first ${RegexTester.MAX_MATCHES})" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    r.matches.forEachIndexed { i, m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("#${i + 1}  at ${m.start}–${m.end}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (m.text.isEmpty()) "(empty match)" else m.text,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                m.groups.forEachIndexed { gi, g ->
                                    Text(
                                        "group ${gi + 1}: ${g ?: "(did not participate)"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
