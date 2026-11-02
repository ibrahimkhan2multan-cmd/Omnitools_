package com.omnitools.app.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import com.omnitools.app.ui.components.TextInput
import java.time.LocalDate

private enum class DateMode(val label: String) { DIFF("Date difference"), AGE("Age"), ADD("Add / subtract") }

@Composable
fun DateScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(DateMode.DIFF) }
    var d1 by remember { mutableStateOf("") }
    var d2 by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("0") }
    var months by remember { mutableStateOf("0") }
    var days by remember { mutableStateOf("0") }
    val today = remember { LocalDate.now() }

    var error: String? = null
    val results = mutableListOf<Pair<String, String>>()
    try {
        when (mode) {
            DateMode.DIFF -> if (d1.isNotBlank() && d2.isNotBlank()) {
                val diff = DateCalc.difference(DateCalc.parse(d1), DateCalc.parse(d2))
                results += "Difference" to "${diff.years} years, ${diff.months} months, ${diff.days} days"
                results += "Total days" to diff.totalDays.toString()
                results += "Weeks" to "${diff.weeks} weeks and ${diff.remainderDays} days"
            }
            DateMode.AGE -> if (d1.isNotBlank()) {
                val age = DateCalc.age(DateCalc.parse(d1), today)
                results += "Age" to "${age.years} years, ${age.months} months, ${age.days} days"
                results += "Days lived" to age.totalDays.toString()
                results += "Next birthday in" to "${age.daysToNextBirthday} days"
            }
            DateMode.ADD -> if (d1.isNotBlank()) {
                val y = years.toLongOrNull() ?: throw IllegalArgumentException("Years must be a whole number.")
                val m = months.toLongOrNull() ?: throw IllegalArgumentException("Months must be a whole number.")
                val dd = days.toLongOrNull() ?: throw IllegalArgumentException("Days must be a whole number.")
                val r = DateCalc.add(DateCalc.parse(d1), y, m, dd)
                results += "Result" to r.toString()
                results += "Weekday" to r.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
    } catch (e: IllegalArgumentException) {
        error = e.message
    } catch (e: java.time.DateTimeException) {
        error = "That date is out of range."
    }

    ScreenScaffold(title = "Date & Age Calculator", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateMode.values().forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                }
            }
            TextInput(
                label = when (mode) { DateMode.DIFF -> "From date (yyyy-MM-dd)"; DateMode.AGE -> "Date of birth (yyyy-MM-dd)"; DateMode.ADD -> "Start date (yyyy-MM-dd)" },
                value = d1, onChange = { d1 = it },
            )
            if (mode == DateMode.DIFF) TextInput("To date (yyyy-MM-dd)", d2, { d2 = it })
            if (mode == DateMode.ADD) {
                Text("Use negative numbers to go back in time.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                NumberField("Years", years, { years = it })
                NumberField("Months", months, { months = it })
                NumberField("Days", days, { days = it })
            }
            OutlinedButton(onClick = {
                if (mode == DateMode.DIFF && d1.isNotBlank()) d2 = today.toString() else d1 = today.toString()
            }) { Text(if (mode == DateMode.DIFF && d1.isNotBlank()) "Set second date to today" else "Set to today") }
            ErrorText(error)
            if (error == null) results.forEach { (caption, value) -> ResultCard(caption, value) }
        }
    }
}
