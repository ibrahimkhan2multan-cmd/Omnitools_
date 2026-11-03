package com.omnitools.app.tools.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun TimestampScreen(onBack: () -> Unit) {
    val local = ZoneId.systemDefault()
    var stamp by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf("") }
    var useUtc by rememberSaveable { mutableStateOf(true) }

    val value = stamp.trim().toLongOrNull()
    val stampError = if (stamp.isNotBlank() && value == null) "Enter whole digits, like 1700000000." else null
    val instant = value?.let {
        try { TimestampTool.toInstant(it) } catch (e: Exception) { null }
    }

    val zone: ZoneId = if (useUtc) ZoneOffset.UTC else local
    var dateError: String? = null
    var parsed: Instant? = null
    if (dateText.isNotBlank()) {
        try { parsed = TimestampTool.parse(dateText, zone) } catch (e: IllegalArgumentException) { dateError = e.message }
    }

    ScreenScaffold(title = "Timestamp Converter", onBack = onBack) { padding ->
        ToolPage(padding) {
            SectionTitle("Timestamp → date")
            TextInput("Unix timestamp", stamp, { stamp = it.filter { c -> c.isDigit() || c == '-' }.take(19) })
            OutlinedButton(onClick = { stamp = Instant.now().epochSecond.toString() }, modifier = Modifier.fillMaxWidth()) { Text("Use the current time") }
            ErrorText(stampError)
            if (value != null) {
                if (instant == null) {
                    ErrorText("That number is outside the dates this tool can show.")
                } else {
                    ResultCard("Read as ${TimestampTool.detectUnit(value).label}", "")
                    ResultCard("UTC", TimestampTool.format(instant, ZoneOffset.UTC))
                    ResultCard("Your time zone (${local.id})", TimestampTool.format(instant, local))
                }
            }

            SectionTitle("Date → timestamp")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = useUtc, onClick = { useUtc = true }, label = { Text("UTC") })
                FilterChip(selected = !useUtc, onClick = { useUtc = false }, label = { Text("My time zone") })
            }
            TextInput("Date and time, e.g. 2024-03-15 14:30:00", dateText, { dateText = it.take(30) })
            ErrorText(dateError)
            parsed?.let {
                ResultCard("Seconds", it.epochSecond.toString())
                ResultCard("Milliseconds", it.toEpochMilli().toString())
            }
            Text(
                "Numbers of 100 billion or more are read as milliseconds, smaller ones as seconds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
