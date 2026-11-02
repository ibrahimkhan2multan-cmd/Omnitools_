package com.omnitools.app.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitools.app.data.Prefs
import com.omnitools.app.ui.components.ScreenScaffold

@Composable
fun CalculatorScreen(prefs: Prefs, onBack: () -> Unit) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    // The angle unit starts from the Settings default; tapping DEG/RAD here overrides it for this visit.
    val defaultDegrees by prefs.calcDegrees.collectAsStateWithLifecycle(initialValue = true)
    var degreesOverride by remember { mutableStateOf<Boolean?>(null) }
    val degrees = degreesOverride ?: defaultDegrees
    var scientific by remember { mutableStateOf(false) }

    fun append(s: String) {
        // Typing after a finished calculation starts from the result for operators, or fresh for digits.
        if (result.isNotEmpty() && expression.isEmpty()) {
            expression = if (s.firstOrNull()?.let { it.isDigit() || it == '(' || it == '.' || it.isLetter() || it == 'π' || it == '√' } == true) "" else result
            result = ""
        }
        expression += s
        error = null
    }

    fun equals() {
        if (expression.isBlank()) return
        try {
            val value = ExpressionEvaluator.evaluate(expression, degrees)
            result = ExpressionEvaluator.format(value)
            error = null
            expression = ""
        } catch (e: ExpressionEvaluator.EvalException) {
            error = e.message
        }
    }

    ScreenScaffold(title = "Calculator", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                Text(
                    expression.ifEmpty { " " },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    error ?: result.ifEmpty { "0" },
                    fontSize = if (error != null) 18.sp else 44.sp,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = scientific, onClick = { scientific = !scientific }, label = { Text("Scientific") })
                if (scientific) {
                    FilterChip(selected = degrees, onClick = { degreesOverride = true }, label = { Text("DEG") })
                    FilterChip(selected = !degrees, onClick = { degreesOverride = false }, label = { Text("RAD") })
                }
            }

            if (scientific) {
                KeyRow(listOf("sin(", "cos(", "tan(", "ln(", "log(")) { append(it) }
                KeyRow(listOf("√(", "^", "!", "π", "e")) { append(it) }
            }
            KeyRow(listOf("C", "(", ")", "%", "÷")) {
                when (it) {
                    "C" -> { expression = ""; result = ""; error = null }
                    else -> append(it)
                }
            }
            KeyRow(listOf("7", "8", "9", "×")) { append(it) }
            KeyRow(listOf("4", "5", "6", "-")) { append(it) }
            KeyRow(listOf("1", "2", "3", "+")) { append(it) }
            KeyRow(listOf("0", ".", "⌫", "=")) {
                when (it) {
                    "⌫" -> if (expression.isNotEmpty()) { expression = expression.dropLast(1); error = null }
                    "=" -> equals()
                    else -> append(it)
                }
            }
        }
    }
}

@Composable
private fun KeyRow(keys: List<String>, onKey: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { key ->
            FilledTonalButton(
                onClick = { onKey(key) },
                modifier = Modifier.weight(1f).height(58.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text(key, fontSize = 20.sp) }
        }
    }
}
