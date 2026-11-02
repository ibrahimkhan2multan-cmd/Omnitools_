package com.omnitools.app.tools.timer

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitools.app.ui.components.NumberField
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.TimeFormat
import kotlinx.coroutines.delay

@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    ScreenScaffold(title = "Stopwatch & Timer", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Stopwatch") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Timer") })
            }
            if (tab == 0) StopwatchTab() else TimerTab()
            Text(
                "Runs while the app is open (it keeps counting if you switch screens). It does not ring when the app is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StopwatchTab() {
    val sw = TimerStore.stopwatch
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sw.running) {
        while (sw.running) {
            now = SystemClock.elapsedRealtime()
            delay(30)
        }
        now = SystemClock.elapsedRealtime()
    }

    Text(
        TimeFormat.stopwatch(sw.elapsed(now)),
        fontSize = 52.sp,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { if (sw.running) sw.pause() else sw.start() }, modifier = Modifier.weight(1f).height(52.dp)) {
            Text(if (sw.running) "Pause" else if (sw.elapsed(now) > 0) "Resume" else "Start")
        }
        OutlinedButton(
            onClick = { if (sw.running) sw.lap() else sw.reset() },
            enabled = sw.running || sw.elapsed(now) > 0,
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text(if (sw.running) "Lap" else "Reset") }
    }
    LazyColumn(Modifier.fillMaxWidth().height(260.dp)) {
        itemsIndexed(sw.laps) { index, total ->
            val previous = sw.laps.getOrNull(index + 1) ?: 0L
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("Lap ${sw.laps.size - index}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+" + TimeFormat.stopwatch(total - previous), fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(TimeFormat.stopwatch(total), fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerTab() {
    val context = LocalContext.current
    val timer = TimerStore.timer
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var ringtone by remember { mutableStateOf<Ringtone?>(null) }

    LaunchedEffect(timer.running) {
        while (timer.running) {
            now = SystemClock.elapsedRealtime()
            if (timer.checkFinished(now)) {
                // Plays the phone's default alarm sound. Needs no permission.
                ringtone = try {
                    RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.also { it.play() }
                } catch (e: Exception) {
                    null
                }
            }
            delay(100)
        }
    }
    DisposableEffect(Unit) { onDispose { ringtone?.stop() } }

    Text(
        if (timer.finished) "00:00" else TimeFormat.countdown(timer.remaining(now)),
        fontSize = 64.sp,
        fontFamily = FontFamily.Monospace,
        color = if (timer.finished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    )
    if (timer.finished) {
        Text("Time's up!", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Button(
            onClick = { ringtone?.stop(); ringtone = null; timer.dismissFinished() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Stop") }
        return
    }

    if (!timer.running) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 5, 10, 15, 30, 60).forEach { m ->
                FilterChip(
                    selected = timer.totalMs == m * 60_000L,
                    onClick = { timer.setDuration(m * 60_000L) },
                    label = { Text(if (m == 60) "1 h" else "$m min") },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Minutes", minutes, { minutes = it.filter(Char::isDigit) }, Modifier.weight(1f))
            NumberField("Seconds", seconds, { seconds = it.filter(Char::isDigit) }, Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = {
                val total = (minutes.toLongOrNull() ?: 0L) * 60_000L + (seconds.toLongOrNull() ?: 0L) * 1_000L
                if (total > 0) timer.setDuration(total)
            },
            enabled = (minutes.toLongOrNull() ?: 0L) > 0 || (seconds.toLongOrNull() ?: 0L) > 0,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Set custom time") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { if (timer.running) timer.pause() else timer.start() }, modifier = Modifier.weight(1f).height(52.dp)) {
            Text(if (timer.running) "Pause" else "Start")
        }
        OutlinedButton(onClick = { timer.reset() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Reset") }
    }
}
