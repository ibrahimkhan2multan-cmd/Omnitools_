package com.omnitools.app.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.NumberField
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold

private enum class PercentMode(val label: String) {
    OF("X% of Y"),
    WHAT("X is what % of Y"),
    CHANGE("% change"),
    DISCOUNT("Discount & tax"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PercentScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(PercentMode.OF) }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }

    fun fmt(v: Double) = ExpressionEvaluator.format(Math.round(v * 1e6) / 1e6)

    // Computed from the current inputs on every recomposition: nothing is stored or faked.
    var error: String? = null
    val results = mutableListOf<Pair<String, String>>()
    val x = a.toDoubleOrNull()
    val y = b.toDoubleOrNull()
    val z = c.toDoubleOrNull()
    if ((a.isNotEmpty() && x == null) || (b.isNotEmpty() && y == null) || (c.isNotEmpty() && z == null)) {
        error = "Enter valid numbers."
    } else {
        try {
            when (mode) {
                PercentMode.OF -> if (x != null && y != null) results += "Result" to fmt(PercentCalc.percentOf(x, y))
                PercentMode.WHAT -> if (x != null && y != null) results += "Percentage" to fmt(PercentCalc.whatPercent(x, y)) + "%"
                PercentMode.CHANGE -> if (x != null && y != null) {
                    val ch = PercentCalc.change(x, y)
                    results += (if (ch >= 0) "Increase" else "Decrease") to fmt(Math.abs(ch)) + "%"
                }
                PercentMode.DISCOUNT -> if (x != null && y != null) {
                    val d = PercentCalc.discount(x, y, z ?: 0.0)
                    results += "You save" to fmt(d.saved)
                    results += "Price after discount" to fmt(d.afterDiscount)
                    if (z != null) results += "Tax" to fmt(d.tax)
                    results += "Total to pay" to fmt(d.total)
                }
            }
        } catch (e: IllegalArgumentException) {
            error = e.message
        }
    }

    val labels = when (mode) {
        PercentMode.OF -> Triple("Percent (X)", "Value (Y)", null)
        PercentMode.WHAT -> Triple("Part (X)", "Whole (Y)", null)
        PercentMode.CHANGE -> Triple("Old value", "New value", null)
        PercentMode.DISCOUNT -> Triple("Original price", "Discount %", "Tax % (optional)")
    }

    ScreenScaffold(title = "Percentage & Discount", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PercentMode.values().forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                }
            }
            NumberField(labels.first, a, { a = it })
            NumberField(labels.second, b, { b = it })
            labels.third?.let { NumberField(it, c, { c = it }) }
            ErrorText(error)
            if (error == null) results.forEach { (caption, value) -> ResultCard(caption, value) }
        }
    }
}
