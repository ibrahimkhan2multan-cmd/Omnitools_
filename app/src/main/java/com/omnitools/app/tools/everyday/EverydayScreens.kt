package com.omnitools.app.tools.everyday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.NumberField
import com.omnitools.app.ui.components.OmniCard
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.Format
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
fun BaseConverterScreen(onBack: () -> Unit) {
    var base by rememberSaveable { mutableStateOf(10) }
    var text by rememberSaveable { mutableStateOf("") }
    val value = if (text.isBlank()) null else NumberBase.parse(text, base)
    val invalid = text.isNotBlank() && value == null

    ScreenScaffold(title = "Number Base Converter", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2 to "Binary", 8 to "Octal", 10 to "Decimal", 16 to "Hex").forEach { (b, label) ->
                    FilterChip(selected = base == b, onClick = { base = b }, label = { Text(label) })
                }
            }
            TextInput("Number in base $base", text, { text = it.take(NumberBase.MAX_DIGITS + 4) })
            ErrorText(if (invalid) "That isn't a valid base-$base number." else null)
            if (value != null) {
                listOf(2 to "Binary (base 2)", 8 to "Octal (base 8)", 10 to "Decimal (base 10)", 16 to "Hexadecimal (base 16)").forEach { (b, label) ->
                    ResultCard(label, NumberBase.grouped(NumberBase.format(value, b), b))
                }
            }
            Text(
                "Whole numbers only. Negative numbers are shown with a minus sign, not two's complement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DiscountScreen(onBack: () -> Unit) {
    var findPercent by rememberSaveable { mutableStateOf(false) }
    var price by rememberSaveable { mutableStateOf("") }
    var percent by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("") }
    var sale by rememberSaveable { mutableStateOf("") }

    ScreenScaffold(title = "Discount & Sale Price", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !findPercent, onClick = { findPercent = false }, label = { Text("Price after discount") })
                FilterChip(selected = findPercent, onClick = { findPercent = true }, label = { Text("Find % off") })
            }
            NumberField("Original price", price, { price = it })
            if (!findPercent) {
                NumberField("Discount (%)", percent, { percent = it })
                NumberField("Sales tax / VAT (%) — optional", tax, { tax = it })
                val p = price.toDoubleOrNull()
                val d = percent.toDoubleOrNull()
                val t = if (tax.isBlank()) 0.0 else tax.toDoubleOrNull()
                if (p != null && d != null && t != null) {
                    val r = runCatching { Discount.apply(p, d, t) }
                    r.exceptionOrNull()?.let { ErrorText(it.message) }
                    r.getOrNull()?.let {
                        ResultCard("You save", Format.money(it.savings))
                        ResultCard("Price after discount", Format.money(it.afterDiscount))
                        if (t > 0.0) {
                            ResultCard("Tax", Format.money(it.tax))
                            ResultCard("Total to pay", Format.money(it.total))
                        }
                    }
                }
            } else {
                NumberField("Sale price", sale, { sale = it })
                val p = price.toDoubleOrNull()
                val s = sale.toDoubleOrNull()
                if (p != null && s != null) {
                    val r = runCatching { Discount.percentOff(p, s) }
                    r.exceptionOrNull()?.let { ErrorText(it.message) }
                    r.getOrNull()?.let {
                        ResultCard(
                            if (it >= 0) "Discount" else "Price increase",
                            String.format(Locale.US, "%.2f%%", kotlin.math.abs(it)),
                        )
                        ResultCard("Difference", Format.money(kotlin.math.abs(p - s)))
                    }
                }
            }
        }
    }
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { EverydayStore(context.applicationContext, "countdowns.json") }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CountdownItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    LaunchedEffect(Unit) {
        items = Countdown.decode(store.read())
        loaded = true
    }
    fun update(new: List<CountdownItem>) {
        items = new
        if (loaded) scope.launch { store.write(Countdown.encode(new)) }
    }

    ScreenScaffold(title = "Countdown", onBack = onBack) { padding ->
        ToolPage(padding) {
            OmniCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextInput("Event name", name, { name = it.take(60) })
                    OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(pickedDay?.let { LocalDate.ofEpochDay(it).format(dateFormat) } ?: "Choose date")
                    }
                    Button(
                        onClick = {
                            val day = pickedDay ?: return@Button
                            update(items + CountdownItem(UUID.randomUUID().toString(), name.trim().ifBlank { "Event" }, day))
                            name = ""
                            pickedDay = null
                        },
                        enabled = pickedDay != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add countdown") }
                }
            }
            if (loaded && items.isEmpty()) EmptyState("No countdowns yet. Add a birthday, trip or deadline above.")
            items.sortedBy { it.epochDay }.forEach { item ->
                val target = LocalDate.ofEpochDay(item.epochDay)
                val days = Countdown.daysUntil(today, target)
                OmniCard {
                    Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(target.format(dateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                Countdown.describe(days) + if (days != 0L) " · " + Countdown.breakdown(today, target) else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { update(items.filterNot { it.id == item.id }) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Delete ${item.name}")
                        }
                    }
                }
            }
        }
    }

    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (pickedDay ?: today.toEpochDay()) * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pickedDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
fun HabitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { EverydayStore(context.applicationContext, "habits.json") }
    val scope = rememberCoroutineScope()
    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now().toEpochDay()

    LaunchedEffect(Unit) {
        habits = Habits.decode(store.read())
        loaded = true
    }
    fun update(new: List<Habit>) {
        habits = new
        if (loaded) scope.launch { store.write(Habits.encode(new)) }
    }

    ScreenScaffold(title = "Habit Tracker", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextInput("New habit", name, { name = it.take(40) }, Modifier.weight(1f))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            update(habits + Habit(UUID.randomUUID().toString(), name.trim(), emptySet()))
                            name = ""
                        }
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Add") }
            }
            if (loaded && habits.isEmpty()) EmptyState("No habits yet. Add one, then tap the circles to tick off each day.")
            habits.forEach { h ->
                OmniCard {
                    Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(h.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Streak ${Habits.currentStreak(h.days, today)} · Best ${Habits.bestStreak(h.days)} · Total ${h.days.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { update(habits.filterNot { it.id == h.id }) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Delete ${h.name}")
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            (6 downTo 0).forEach { back ->
                                val day = today - back
                                val done = day in h.days
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                    Box(
                                        Modifier
                                            .padding(top = 4.dp)
                                            .size(36.dp)
                                            .background(
                                                if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape,
                                            )
                                            .clickable {
                                                update(habits.map { if (it.id == h.id) it.copy(days = Habits.toggle(it.days, day)) else it })
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (done) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
