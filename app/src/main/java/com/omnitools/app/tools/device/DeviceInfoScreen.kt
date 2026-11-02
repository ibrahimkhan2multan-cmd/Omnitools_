package com.omnitools.app.tools.device

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.omnitools.app.BuildConfig
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.util.Format

private data class InfoSection(val title: String, val note: String?, val rows: List<Pair<String, String>>)

@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sections = remember { collect(context.applicationContext) }

    ScreenScaffold(title = "Device Information", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            sections.forEach { section ->
                SectionTitle(section.title)
                section.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        section.rows.forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.42f),
                                )
                                SelectionContainer(Modifier.weight(0.58f)) {
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun collect(context: Context): List<InfoSection> {
    val device = listOf(
        "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        "Model" to Build.MODEL,
        "Android version" to Build.VERSION.RELEASE,
        "API level" to Build.VERSION.SDK_INT.toString(),
        "Security patch" to (Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() } ?: "Not reported"),
        "Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Not reported" },
    )

    val m = context.resources.displayMetrics
    val screen = listOf(
        "Resolution" to "${m.widthPixels} × ${m.heightPixels} px",
        "Density" to "${m.densityDpi} dpi (${"%.2f".format(java.util.Locale.ROOT, m.density)}x)",
        "Size in dp" to "${(m.widthPixels / m.density).toInt()} × ${(m.heightPixels / m.density).toInt()} dp",
    )

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val stat = StatFs(Environment.getDataDirectory().path)
    val memory = listOf(
        "RAM total" to Format.bytes(mem.totalMem),
        "RAM available now" to Format.bytes(mem.availMem),
        "Internal storage total" to Format.bytes(stat.blockCountLong * stat.blockSizeLong),
        "Internal storage free" to Format.bytes(stat.availableBlocksLong * stat.blockSizeLong),
    )

    return listOf(
        InfoSection("Device", "Values reported by Android for this phone.", device),
        InfoSection("Screen", null, screen),
        InfoSection("Memory & storage", "Free values change constantly; these are a snapshot from when you opened this screen.", memory),
        InfoSection("Battery", null, batteryRows(context)),
        InfoSection("Sensors", "Sensors listed by Android. Some may be virtual.", sensorRows(context)),
        InfoSection("Cameras", "Basic details only. Opening the camera is not needed to list them.", cameraRows(context)),
        InfoSection(
            "This app (OmniTools)",
            "These values are about OmniTools only, not the whole device.",
            listOf(
                "Version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                "Package" to context.packageName,
                "Camera permission" to
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) "Granted" else "Not granted",
            ),
        ),
    )
}

private fun batteryRows(context: Context): List<Pair<String, String>> {
    val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?: return listOf("Battery" to "Not available")
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE

    val rows = mutableListOf<Pair<String, String>>()
    rows += "Level" to if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "Not available"
    rows += "Status" to when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
    }
    rows += "Power source" to when {
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "AC adapter"
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Wireless"
        else -> "Battery"
    }
    if (tempTenths != Int.MIN_VALUE && tempTenths > 0) {
        rows += "Temperature" to "${"%.1f".format(java.util.Locale.ROOT, tempTenths / 10.0)} °C"
    }
    return rows
}

private fun sensorRows(context: Context): List<Pair<String, String>> {
    val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensors = sm.getSensorList(Sensor.TYPE_ALL)
    if (sensors.isEmpty()) return listOf("Sensors" to "None reported")
    return listOf("Count" to sensors.size.toString()) +
        sensors.map { it.name }.distinct().sorted().map { "•" to it }
}

private fun cameraRows(context: Context): List<Pair<String, String>> {
    return try {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val ids = cm.cameraIdList
        if (ids.isEmpty()) return listOf("Cameras" to "None reported")
        ids.map { id ->
            val facing = when (cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }
            "Camera $id" to facing
        }
    } catch (e: Exception) {
        listOf("Cameras" to "Could not be read on this device")
    }
}
