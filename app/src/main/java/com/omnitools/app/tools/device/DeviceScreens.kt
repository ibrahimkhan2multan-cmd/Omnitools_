package com.omnitools.app.tools.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.ToolPage
import java.util.Locale

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun BatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val i: Intent? = remember(tick) { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
    val temp = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
    val volt = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    val rows = listOf(
        "Level" to (if (pct >= 0) "$pct%" else "Unknown"),
        "Status" to DeviceFormat.batteryStatus(i?.getIntExtra(BatteryManager.EXTRA_STATUS, 1) ?: 1),
        "Power source" to DeviceFormat.plugged(i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0),
        "Health" to DeviceFormat.batteryHealth(i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 1) ?: 1),
        "Temperature" to (if (temp != Int.MIN_VALUE) String.format(Locale.US, "%.1f °C", temp / 10.0) else "Unknown"),
        "Voltage" to (if (volt > 0) "$volt mV" else "Unknown"),
        "Technology" to (i?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"),
    )
    ScreenScaffold(title = "Battery", onBack = onBack) { padding ->
        ToolPage(padding) {
            if (pct >= 0) LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
            rows.forEach { (k, v) -> ResultCard(k, v) }
            OutlinedButton(onClick = { tick++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
            CopyShareRow(rows.joinToString("\n") { "${it.first}: ${it.second}" }, "Battery")
            Note("Read from the system. Android does not share the battery's wear or capacity with apps.")
        }
    }
}

@Composable
fun StorageMemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val data = remember(tick) { runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull() }
    val total = data?.totalBytes ?: -1L
    val free = data?.availableBytes ?: -1L
    val mem = remember(tick) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }
    val usedStorage = if (total >= 0 && free >= 0) total - free else -1L
    val usedRam = mem.totalMem - mem.availMem
    ScreenScaffold(title = "Storage & Memory", onBack = onBack) { padding ->
        ToolPage(padding) {
            Text("Storage", style = MaterialTheme.typography.titleMedium)
            if (total > 0) LinearProgressIndicator(progress = { DeviceFormat.percent(usedStorage, total) / 100f }, modifier = Modifier.fillMaxWidth())
            ResultCard("Total", DeviceFormat.bytes(total))
            ResultCard("Used", "${DeviceFormat.bytes(usedStorage)}  (${DeviceFormat.percent(usedStorage, total)}%)")
            ResultCard("Free", DeviceFormat.bytes(free))
            Text("Memory (RAM)", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { DeviceFormat.percent(usedRam, mem.totalMem) / 100f }, modifier = Modifier.fillMaxWidth())
            ResultCard("Total", DeviceFormat.bytes(mem.totalMem))
            ResultCard("In use", "${DeviceFormat.bytes(usedRam)}  (${DeviceFormat.percent(usedRam, mem.totalMem)}%)")
            ResultCard("Available", DeviceFormat.bytes(mem.availMem))
            if (mem.lowMemory) Text("The system reports low memory right now.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { tick++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
            Note("Sizes use 1024 per step, so they can differ from the numbers on the phone's box.")
        }
    }
}

@Composable
fun DisplayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dm = context.resources.displayMetrics
    val cfg = context.resources.configuration
    val w = dm.widthPixels; val h = dm.heightPixels
    val inchesW = w / dm.xdpi.toDouble(); val inchesH = h / dm.ydpi.toDouble()
    val diag = Math.sqrt(inchesW * inchesW + inchesH * inchesH)
    val refresh = runCatching { context.display?.refreshRate }.getOrNull()
        ?: runCatching { @Suppress("DEPRECATION") (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.refreshRate }.getOrNull()
        ?: 0f
    val rows = listOf(
        "Resolution" to "$w × $h px",
        "Density" to "${dm.densityDpi} dpi (${String.format(Locale.US, "%.2f", dm.density)}x)",
        "Screen size (approx.)" to (if (diag.isFinite() && diag > 0) String.format(Locale.US, "%.1f inches", diag) else "Unknown"),
        "Refresh rate" to (if (refresh > 0) String.format(Locale.US, "%.0f Hz", refresh) else "Unknown"),
        "Width in dp" to "${cfg.screenWidthDp} dp",
        "Height in dp" to "${cfg.screenHeightDp} dp",
        "Font scale" to String.format(Locale.US, "%.2f", cfg.fontScale),
        "Orientation" to (if (cfg.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) "Landscape" else "Portrait"),
        "Dark mode" to (if ((cfg.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) "On" else "Off"),
    )
    ScreenScaffold(title = "Display Info", onBack = onBack) { padding ->
        ToolPage(padding) {
            rows.forEach { (k, v) -> ResultCard(k, v) }
            CopyShareRow(rows.joinToString("\n") { "${it.first}: ${it.second}" }, "Display")
            Note("The size is worked out from the screen's reported density and can be a little off.")
        }
    }
}

@Composable
fun SensorsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensors: List<Sensor> = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getSensorList(Sensor.TYPE_ALL).sortedBy { it.type }
    }
    ScreenScaffold(title = "Sensors", onBack = onBack) { padding ->
        ToolPage(padding) {
            Text("${sensors.size} sensors found", style = MaterialTheme.typography.titleMedium)
            sensors.forEach { s ->
                ResultCard(DeviceFormat.sensorType(s.type), "${s.name}\n${s.vendor}  ·  range ${String.format(Locale.US, "%.1f", s.maximumRange)}  ·  ${String.format(Locale.US, "%.2f", s.power)} mA")
            }
            Note("Lists the sensors the phone has. This tool only reads their names and specs; it does not record readings.")
        }
    }
}
