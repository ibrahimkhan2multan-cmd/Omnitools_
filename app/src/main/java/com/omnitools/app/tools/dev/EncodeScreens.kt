package com.omnitools.app.tools.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.util.Actions
import java.time.ZoneId

@Composable
fun EncodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var kind by rememberSaveable { mutableStateOf(EncodeKind.BASE64) }
    var encode by rememberSaveable { mutableStateOf(true) }
    var input by rememberSaveable { mutableStateOf("") }

    var output: String? = null
    var error: String? = null
    if (input.isNotEmpty()) {
        try { output = if (encode) EncodeTools.encode(kind, input) else EncodeTools.decode(kind, input) }
        catch (e: IllegalArgumentException) { error = e.message }
    }

    ScreenScaffold(title = "Encode / Decode", onBack = onBack) { padding ->
        ToolPage(padding) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EncodeKind.values().forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = encode, onClick = { encode = true }, label = { Text("Encode") })
                FilterChip(selected = !encode, onClick = { encode = false }, label = { Text("Decode") })
            }
            TextInput(if (encode) "Text" else "${kind.label} text", input, { input = it.take(20000) }, singleLine = false, minLines = 4)
            ErrorText(error)
            if (output != null) {
                ResultCard(if (encode) "Encoded (${kind.label})" else "Decoded", output)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { Actions.copy(context, "Result", output) }, modifier = Modifier.weight(1f)) { Text("Copy") }
                    OutlinedButton(onClick = { Actions.shareText(context, output) }, modifier = Modifier.weight(1f)) { Text("Share") }
                }
            }
            Text(
                "Encoding is not encryption: anyone can decode it. Works on UTF-8 text, entirely on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TimeZoneScreen(onBack: () -> Unit) {
    val device = ZoneId.systemDefault().id
    var from by rememberSaveable { mutableStateOf(if (TimeZones.isValid(device)) device else "UTC") }
    var query by rememberSaveable { mutableStateOf("") }
    var timeText by rememberSaveable { mutableStateOf(java.time.LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }) }
    var targets by rememberSaveable { mutableStateOf(TimeZones.popular.take(6).joinToString(",")) }

    val time = TimeZones.parseTime(timeText)
    val targetList = targets.split(",").filter { it.isNotBlank() }
    val rows = if (time != null) TimeZones.convert(TimeZones.today(from).atTime(time), from, targetList) else emptyList()
    val matches = TimeZones.search(query)

    ScreenScaffold(title = "Time Zones", onBack = onBack) { padding ->
        ToolPage(padding) {
            Text("From: $from", style = MaterialTheme.typography.titleMedium)
            TextInput("Time here (HH:mm, 24-hour)", timeText, { timeText = it.take(5) })
            if (time == null) ErrorText("Enter a time like 09:30 or 18:05")
            TextInput("Search a city or zone (e.g. tokyo)", query, { query = it.take(40) })
            if (matches.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    matches.forEach { id ->
                        FilterChip(selected = id == from, onClick = { from = id; query = "" }, label = { Text(id) })
                    }
                }
                Text("Tap a result to use it as the starting zone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionTitle("Same moment in")
            rows.forEach { r ->
                ResultCard(
                    "${r.label} · UTC${r.offset}",
                    r.time + "  " + r.date + (if (r.dayShift > 0) " (+${r.dayShift} day)" else if (r.dayShift < 0) " (${r.dayShift} day)" else ""),
                )
            }
            SectionTitle("Show these zones")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeZones.popular.forEach { id ->
                    val on = id in targetList
                    FilterChip(
                        selected = on,
                        onClick = { targets = (if (on) targetList - id else targetList + id).joinToString(",") },
                        label = { Text(id.substringAfterLast('/').replace('_', ' ')) },
                    )
                }
            }
        }
    }
}
