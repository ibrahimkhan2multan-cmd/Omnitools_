package com.omnitools.app.tools.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.util.Actions
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ColorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var rgb by remember { mutableStateOf(ColorTools.Rgb(79, 140, 255)) }
    var hexText by remember { mutableStateOf(ColorTools.toHex(rgb)) }
    val hex = ColorTools.toHex(rgb)
    val hsl = ColorTools.toHsl(rgb)
    val hexInvalid = hexText.isNotBlank() && ColorTools.parseHex(hexText) == null

    fun setRgb(new: ColorTools.Rgb) {
        rgb = new
        hexText = ColorTools.toHex(new)
    }

    ScreenScaffold(title = "Color Converter", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(rgb.r, rgb.g, rgb.b), RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(hex, color = if (ColorTools.luminance(rgb) > 0.4) Color.Black else Color.White, style = MaterialTheme.typography.titleLarge)
            }

            TextInput("HEX (for example #4F8CFF)", hexText, { text ->
                hexText = text
                ColorTools.parseHex(text)?.let { rgb = it }
            })
            if (hexInvalid) Text("Enter 3 or 6 hex digits, for example #4F8CFF.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            Channel("Red", rgb.r) { setRgb(rgb.copy(r = it)) }
            Channel("Green", rgb.g) { setRgb(rgb.copy(g = it)) }
            Channel("Blue", rgb.b) { setRgb(rgb.copy(b = it)) }

            ResultCard("RGB", "${rgb.r}, ${rgb.g}, ${rgb.b}")
            ResultCard(
                "HSL",
                String.format(Locale.ROOT, "%d°, %d%%, %d%%", hsl.h.roundToInt(), hsl.s.roundToInt(), hsl.l.roundToInt()),
            )
            val onWhite = ColorTools.contrast(rgb, ColorTools.Rgb(255, 255, 255))
            val onBlack = ColorTools.contrast(rgb, ColorTools.Rgb(0, 0, 0))
            ResultCard(
                "Contrast ratio (readability)",
                String.format(Locale.ROOT, "%.1f : 1 with white text, %.1f : 1 with black text", onWhite, onBlack),
            )
            Text(
                "WCAG asks for at least 4.5 : 1 for normal text.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { Actions.copy(context, "HEX", hex) }, modifier = Modifier.weight(1f)) { Text("Copy HEX") }
                OutlinedButton(
                    onClick = { Actions.copy(context, "RGB", "rgb(${rgb.r}, ${rgb.g}, ${rgb.b})") },
                    modifier = Modifier.weight(1f),
                ) { Text("Copy RGB") }
            }
        }
    }
}

@Composable
private fun Channel(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value", style = MaterialTheme.typography.labelLarge)
        Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) }, valueRange = 0f..255f)
    }
}
