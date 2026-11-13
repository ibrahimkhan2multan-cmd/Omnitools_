package com.omnitools.app.tools.timer

import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.omnitools.app.tools.timer.alarm.AlarmPermissions
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.TimeFormat

@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    ScreenScaffold(title = "Stopwatch & Timer", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Stopwatch") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Timer") })
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (slideInVertically(tween(260)) { it / 14 } + fadeIn(tween(260))) togetherWith fadeOut(tween(120))
                },
                label = "tool tab",
            ) { current ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (current == 0) StopwatchTab() else TimerTab()
                }
            }
        }
    }
}

@Composable
private fun StopwatchTab() {
    val sw = TimerStore.stopwatch
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sw.running) {
        while (sw.running) {
            withFrameNanos { }
            now = SystemClock.elapsedRealtime()
        }
        now = SystemClock.elapsedRealtime()
    }

    Text(
        TimeFormat.stopwatch(sw.elapsed(now)),
        fontSize = 52.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { if (sw.running) sw.pause() else sw.start() }, modifier = Modifier.weight(1f).height(56.dp)) {
            PlayPauseLabel(playing = sw.running, idleText = if (sw.elapsed(now) > 0) "Resume" else "Start", activeText = "Pause")
        }
        OutlinedButton(
            onClick = { if (sw.running) sw.lap() else sw.reset() },
            enabled = sw.running || sw.elapsed(now) > 0,
            modifier = Modifier.weight(1f).height(56.dp),
        ) { Text(if (sw.running) "Lap" else "Reset") }
    }
    LazyColumn(Modifier.fillMaxWidth().height(260.dp)) {
        itemsIndexed(sw.laps, key = { index, _ -> sw.laps.size - index }) { index, total ->
            val previous = sw.laps.getOrNull(index + 1) ?: 0L
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).animateItem()) {
                Text("Lap ${sw.laps.size - index}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+" + TimeFormat.stopwatch(total - previous), fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(TimeFormat.stopwatch(total), fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
    Text(
        "The stopwatch keeps counting while you move between screens. It stops if Android ends the app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Icon and text that swap with a small slide when switching between Start and Pause. */
@Composable
private fun PlayPauseLabel(playing: Boolean, idleText: String, activeText: String) {
    AnimatedContent(
        targetState = playing,
        transitionSpec = { (slideInVertically(tween(180)) { it } + fadeIn(tween(180))) togetherWith (slideOutVertically(tween(180)) { -it } + fadeOut(tween(120))) },
        label = "play pause",
    ) { isPlaying ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
            Text(if (isPlaying) activeText else idleText)
        }
    }
}

private val PRESETS = listOf(
    "1 ms" to 1L,
    "500 ms" to 500L,
    "10 s" to 10_000L,
    "1 min" to 60_000L,
    "5 min" to 300_000L,
    "15 min" to 900_000L,
    "1 h" to 3_600_000L,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerTab() {
    val context = LocalContext.current
    val timer = TimerStore.timer
    val ringing = TimerController.ringing
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    // One update per screen frame while running, so the milliseconds move smoothly.
    LaunchedEffect(timer.running) {
        while (timer.running) {
            withFrameNanos { }
            now = SystemClock.elapsedRealtime()
        }
        now = SystemClock.elapsedRealtime()
    }

    val start = remember { TimerModel.split(timer.totalMs) }
    var hours by remember { mutableStateOf(start.hours.toString()) }
    var minutes by remember { mutableStateOf(start.minutes.toString()) }
    var seconds by remember { mutableStateOf(start.seconds.toString()) }
    var millis by remember { mutableStateOf(start.millis.toString()) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    var notificationsOk by remember { mutableStateOf(AlarmPermissions.notificationsAllowed(context)) }
    var fullScreenOk by remember { mutableStateOf(AlarmPermissions.fullScreenAllowed(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOk = AlarmPermissions.notificationsAllowed(context)
    }
    LifecycleResumeEffect(Unit) {
        notificationsOk = AlarmPermissions.notificationsAllowed(context)
        fullScreenOk = AlarmPermissions.fullScreenAllowed(context)
        onPauseOrDispose { }
    }

    fun entered(): Long = TimerModel.durationFrom(
        hours.toLongOrNull() ?: 0L, minutes.toLongOrNull() ?: 0L, seconds.toLongOrNull() ?: 0L, millis.toLongOrNull() ?: 0L,
    )
    fun applyFields() {
        val d = entered()
        if (d > 0L) timer.setDuration(d)
    }
    fun showDuration(ms: Long) {
        val p = TimerModel.split(ms)
        hours = p.hours.toString(); minutes = p.minutes.toString(); seconds = p.seconds.toString(); millis = p.millis.toString()
        timer.setDuration(ms)
    }

    val idle = !timer.running && !timer.finished
    val paused = idle && timer.remaining(now) < timer.totalMs

    TimerRing(timer, now)

    AnimatedVisibility(visible = timer.finished, enter = fadeIn() + slideInVertically { it / 4 }, exit = fadeOut()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Time's up!",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { TimerController.stopRinging(context) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Text(if (ringing) "  Stop alarm" else "  Dismiss")
            }
        }
    }

    AnimatedVisibility(visible = idle && !paused, enter = fadeIn() + slideInVertically { it / 6 }, exit = fadeOut()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { (label, ms) ->
                    FilterChip(selected = timer.totalMs == ms, onClick = { showDuration(ms) }, label = { Text(label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DigitField("Hours", hours, { hours = it; applyFields() }, Modifier.weight(1f))
                DigitField("Min", minutes, { minutes = it; applyFields() }, Modifier.weight(1f))
                DigitField("Sec", seconds, { seconds = it; applyFields() }, Modifier.weight(1f))
                DigitField("ms", millis, { millis = it; applyFields() }, Modifier.weight(1f))
            }
            Text(
                if (entered() > 0L) "Any length from 1 millisecond to 99 hours."
                else "Enter at least 1 millisecond.",
                style = MaterialTheme.typography.bodySmall,
                color = if (entered() > 0L) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }

    if (!timer.finished) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (timer.running) {
                        TimerController.pause(context)
                    } else {
                        if (!notificationsOk && Build.VERSION.SDK_INT >= 33 && !askedForNotifications) {
                            askedForNotifications = true
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        TimerController.start(context)
                    }
                },
                enabled = timer.running || paused || entered() > 0L,
                modifier = Modifier.weight(1f).height(56.dp),
            ) { PlayPauseLabel(playing = timer.running, idleText = if (paused) "Resume" else "Start", activeText = "Pause") }
            OutlinedButton(
                onClick = { TimerController.reset(context) },
                enabled = timer.running || paused,
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text("Reset") }
        }
    }

    RingReadyCard(
        notificationsOk = notificationsOk,
        fullScreenOk = fullScreenOk,
        onAllowNotifications = {
            if (Build.VERSION.SDK_INT >= 33 && !askedForNotifications) {
                askedForNotifications = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(AlarmPermissions.notificationSettingsIntent(context))
            }
        },
        onAllowFullScreen = { context.startActivity(AlarmPermissions.fullScreenSettingsIntent(context)) },
    )
}

/** Progress ring around the time. Drains as time passes, pulses red when the timer has finished. */
@Composable
private fun TimerRing(timer: TimerModel, now: Long) {
    val remaining = timer.remaining(now)
    val target = if (timer.finished) 0f else (remaining.toFloat() / timer.totalMs.toFloat()).coerceIn(0f, 1f)
    // While running the value already changes every frame, so follow it exactly; otherwise glide to the new value.
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (timer.running) snap() else tween(450, easing = FastOutSlowInEasing),
        label = "timer progress",
    )
    val pulse by rememberInfiniteTransition(label = "finished pulse").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "pulse",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val a = MaterialTheme.colorScheme.primary
    val b = MaterialTheme.colorScheme.secondary
    val err = MaterialTheme.colorScheme.error

    val text = TimeFormat.countdownPrecise(if (timer.finished) 0L else remaining)
    val main = text.substringBeforeLast('.')
    val ms = text.substringAfterLast('.')

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val inset = stroke / 2f
                val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                if (timer.finished) {
                    drawArc(err.copy(alpha = pulse), 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                } else if (progress > 0f) {
                    rotate(-90f) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(a, b, a)),
                            startAngle = 0f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.animateContentSize()) {
                Text(
                    main,
                    fontSize = if (main.length > 5) 40.sp else 58.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (timer.finished) err else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    ".$ms",
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (timer.finished) err else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DigitField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** Shows what the phone still has to allow before the timer can ring with the app closed. */
@Composable
private fun RingReadyCard(
    notificationsOk: Boolean,
    fullScreenOk: Boolean,
    onAllowNotifications: () -> Unit,
    onAllowFullScreen: () -> Unit,
) {
    val allOk = notificationsOk && fullScreenOk
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (allOk) "Ready to ring with the app closed" else "Allow these so it can ring with the app closed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            PermissionLine("Notifications (shows the Stop button)", notificationsOk, onAllowNotifications)
            PermissionLine("Full-screen alert over the lock screen", fullScreenOk, onAllowFullScreen)
            Text(
                "The alarm uses your phone's Alarm volume and sound, and vibrates. It cannot ring while the phone is " +
                    "switched off. Android may deliver an alarm a fraction of a second late when the app is closed; " +
                    "the countdown itself is millisecond-exact.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionLine(label: String, ok: Boolean, onAllow: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
            contentDescription = if (ok) "Allowed" else "Not allowed",
            tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (!ok) TextButton(onClick = onAllow) { Text("Allow") }
    }
}
