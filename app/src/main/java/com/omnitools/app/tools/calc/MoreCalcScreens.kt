package com.omnitools.app.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
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
import com.omnitools.app.util.Format
import java.util.Locale

/** Runs [block]; returns its value, or null plus the message when the input is invalid. */
private inline fun <T> attempt(block: () -> T): Pair<T?, String?> = try {
    block() to null
} catch (e: IllegalArgumentException) {
    null to e.message
}

@Composable
fun LoanScreen(onBack: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var term by rememberSaveable { mutableStateOf("") }
    var years by rememberSaveable { mutableStateOf(true) }

    val a = amount.toDoubleOrNull()
    val r = rate.toDoubleOrNull()
    val t = term.toDoubleOrNull()
    val months = t?.let { (if (years) it * 12 else it).let { m -> Math.round(m).toInt() } }
    val ready = a != null && r != null && months != null
    val (result, error) = if (ready) attempt { LoanCalc.emi(a!!, r!!, months!!) } else null to null
    val rows = if (result != null) LoanCalc.schedule(a!!, r!!, months!!).take(12) else emptyList()

    ScreenScaffold(title = "Loan & EMI Calculator", onBack = onBack) { padding ->
        ToolPage(padding) {
            NumberField("Loan amount", amount, { amount = it })
            NumberField("Yearly interest rate (%)", rate, { rate = it })
            NumberField(if (years) "Term (years)" else "Term (months)", term, { term = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = years, onClick = { years = true }, label = { Text("Years") })
                FilterChip(selected = !years, onClick = { years = false }, label = { Text("Months") })
            }
            ErrorText(error)
            if (result != null) {
                ResultCard("Monthly payment", Format.money(result.monthlyPayment))
                ResultCard("Total interest", Format.money(result.totalInterest))
                ResultCard("Total you pay back", Format.money(result.totalPayment))
                Text("First ${rows.size} payments", style = MaterialTheme.typography.titleSmall)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("#${row.month}", Modifier.weight(0.6f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Interest ${Format.money(row.interest)}", Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall)
                        Text("Balance ${Format.money(row.balance)}", Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(
                "Assumes a fixed rate and equal monthly payments. Real loans may add fees or change the rate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BmiScreen(onBack: () -> Unit) {
    var metric by rememberSaveable { mutableStateOf(true) }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var inches by rememberSaveable { mutableStateOf("") }

    val w = weight.toDoubleOrNull()
    val h = height.toDoubleOrNull()
    val i = if (inches.isEmpty()) 0.0 else inches.toDoubleOrNull()
    val ready = w != null && h != null && i != null
    val (bmi, error) = if (ready) attempt {
        val kg = if (metric) w!! else BmiCalc.poundsToKg(w!!)
        val m = if (metric) h!! / 100.0 else BmiCalc.feetInchesToM(h!!, i!!)
        Triple(BmiCalc.bmi(kg, m), BmiCalc.healthyRange(m), m)
    } else null to null

    ScreenScaffold(title = "BMI Calculator", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = metric, onClick = { metric = true }, label = { Text("kg & cm") })
                FilterChip(selected = !metric, onClick = { metric = false }, label = { Text("lb & ft/in") })
            }
            NumberField(if (metric) "Weight (kg)" else "Weight (lb)", weight, { weight = it })
            if (metric) {
                NumberField("Height (cm)", height, { height = it })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Feet", height, { height = it }, Modifier.weight(1f))
                    NumberField("Inches", inches, { inches = it }, Modifier.weight(1f))
                }
            }
            ErrorText(error)
            if (bmi != null) {
                val (value, range, m) = bmi
                ResultCard("Your BMI", String.format(Locale.US, "%.1f", value) + " — " + BmiCalc.category(value))
                val (lo, hi) = range
                ResultCard(
                    "Weight range for BMI 18.5 to 24.9 at this height",
                    if (metric) String.format(Locale.US, "%.1f to %.1f kg", lo, hi)
                    else String.format(Locale.US, "%.0f to %.0f lb", lo / 0.45359237, hi / 0.45359237),
                )
            }
            Text(
                "BMI is a rough screening number for adults. It ignores muscle, age and body shape, and it is not a diagnosis. " +
                    "Ask a health professional about your own health.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TipScreen(onBack: () -> Unit) {
    var bill by rememberSaveable { mutableStateOf("") }
    var tip by rememberSaveable { mutableStateOf("15") }
    var people by rememberSaveable { mutableStateOf("1") }
    var roundUp by rememberSaveable { mutableStateOf(false) }

    val b = bill.toDoubleOrNull()
    val t = tip.toDoubleOrNull()
    val p = people.toIntOrNull()
    val (result, error) = if (b != null && t != null && p != null) attempt { TipCalc.split(b, t, p, roundUp) } else null to null

    ScreenScaffold(title = "Tip & Bill Split", onBack = onBack) { padding ->
        ToolPage(padding) {
            NumberField("Bill amount", bill, { bill = it })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("0", "5", "10", "15", "18", "20").forEach { v ->
                    FilterChip(selected = tip == v, onClick = { tip = v }, label = { Text("$v%") })
                }
            }
            NumberField("Tip (%)", tip, { tip = it })
            NumberField("People", people, { people = it.filter(Char::isDigit) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = roundUp, onCheckedChange = { roundUp = it })
                Text("Round each share up to a whole amount")
            }
            ErrorText(error)
            if (result != null) {
                ResultCard("Each person pays", Format.money(result.perPerson))
                ResultCard("Tip", Format.money(result.tip))
                ResultCard("Total", Format.money(result.total))
            }
        }
    }
}

@Composable
fun RomanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    val trimmed = input.trim()
    val toRoman = trimmed.isNotEmpty() && trimmed.all { it.isDigit() }
    val (output, error) = when {
        trimmed.isEmpty() -> null to null
        toRoman -> attempt { RomanNumerals.toRoman(trimmed.toIntOrNull() ?: throw IllegalArgumentException("Roman numerals cover 1 to 3999.")) }
        else -> attempt { RomanNumerals.fromRoman(trimmed).toString() }
    }
    ScreenScaffold(title = "Roman Numerals", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Number (1–3999) or Roman numeral", input, { input = it.take(20) })
            ErrorText(error)
            if (output != null) {
                ResultCard(if (toRoman) "Roman numeral" else "Number", output)
                OutlinedButton(onClick = { Actions.copy(context, "Result", output) }, modifier = Modifier.fillMaxWidth()) { Text("Copy") }
            }
            Text(
                "Type digits to get the Roman numeral, or letters (I V X L C D M) to get the number.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NumberWordsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    val (output, error) = if (input.isBlank()) null to null else attempt { NumberWords.fromString(input) }
    ScreenScaffold(title = "Number to Words", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Number", input, { input = it.take(40) })
            ErrorText(error)
            if (output != null) {
                ResultCard("In words", output)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { Actions.copy(context, "Words", output) }, modifier = Modifier.weight(1f)) { Text("Copy") }
                    OutlinedButton(onClick = { Actions.shareText(context, output) }, modifier = Modifier.weight(1f)) { Text("Share") }
                }
            }
            Text(
                "English, American style, up to 15 digits before the decimal point. Decimal digits are read one by one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
