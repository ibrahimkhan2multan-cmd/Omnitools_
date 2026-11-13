package com.omnitools.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Animated backdrops drawn behind every screen. DEFAULT keeps the plain colour of the dark/light mode. */
enum class BackgroundTheme(
    val label: String, val description: String, val dark: Boolean,
    val primary: Color, val onPrimary: Color, val secondary: Color, val swatch: List<Color>,
) {
    DEFAULT("Plain", "Simple colour that follows Dark / Light / Follow phone", false, Color.Unspecified, Color.Unspecified, Color.Unspecified, listOf(Color(0xFF0E1220), Color(0xFFF6F7FC))),
    GALAXY("Galaxy", "Twinkling stars and purple nebula clouds (dark)", true, Color(0xFFA98BFF), Color(0xFF0A0620), Color(0xFFFF7BD5), listOf(Color(0xFF0B0620), Color(0xFF4A1E8C), Color(0xFF1B3A8C))),
    CANDY("Candy", "Pastel pink, mint and lemon with floating sweets (light)", false, Color(0xFFE0388A), Color.White, Color(0xFF7C4DFF), listOf(Color(0xFFFFD1EA), Color(0xFFD9F5E8), Color(0xFFFFF1B8))),
    RAINBOW("Rainbow", "Soft colours that slowly flow through the rainbow (light)", false, Color(0xFF6F4DF0), Color.White, Color(0xFFE0388A), listOf(Color(0xFFFF9AA2), Color(0xFFFFE08A), Color(0xFF9AE6B4), Color(0xFF9AC8FF), Color(0xFFC9A6FF))),
    AURORA("Aurora", "Green and violet northern lights (dark)", true, Color(0xFF3DDCB0), Color(0xFF03201A), Color(0xFFB48CFF), listOf(Color(0xFF04141F), Color(0xFF0E7C66), Color(0xFF5B3FA8))),
    SUNSET("Sunset", "Warm purple to orange evening sky (dark)", true, Color(0xFFFF9A5C), Color(0xFF2B0E00), Color(0xFFFF6FA3), listOf(Color(0xFF1E0B3A), Color(0xFF8A2F6E), Color(0xFFE0703F))),
    OCEAN("Ocean", "Deep blue water with moving waves (dark)", true, Color(0xFF3CC8E8), Color(0xFF021B26), Color(0xFF7FE3C8), listOf(Color(0xFF021B3A), Color(0xFF05506B), Color(0xFF0A7C8C)));

    companion object {
        fun from(name: String?): BackgroundTheme = values().firstOrNull { it.name == name } ?: DEFAULT
    }
}

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Int)

private val STARS = Random(7).let { r -> List(90) { Star(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 1.6f, r.nextFloat(), 1 + r.nextInt(3)) } }
private val SWEETS = Random(11).let { r -> List(16) { Star(r.nextFloat(), r.nextFloat(), 10f + r.nextFloat() * 34f, r.nextFloat(), 1 + r.nextInt(2)) } }
private val CANDY_COLORS = listOf(Color(0xFFFF8FC4), Color(0xFF8FE3C0), Color(0xFFFFE27A), Color(0xFFB79BFF), Color(0xFF8FD0FF))

private fun wave(t: Float, speed: Int, phase: Float): Float = sin(2f * PI.toFloat() * (t * speed + phase))

/** The animated picture. It loops seamlessly every 40 seconds and only redraws, so it costs no recomposition. */
@Composable
fun Backdrop(theme: BackgroundTheme, modifier: Modifier = Modifier.fillMaxSize()) {
    if (theme == BackgroundTheme.DEFAULT) return
    val transition = rememberInfiniteTransition(label = "backdrop")
    val t = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val reduced = rememberReducedMotion()
    val rainbow = remember { List(7) { it } }
    Canvas(modifier) {
        val time = if (reduced) 0.25f else t.value
        when (theme) {
            BackgroundTheme.GALAXY -> drawGalaxy(time)
            BackgroundTheme.CANDY -> drawCandy(time)
            BackgroundTheme.RAINBOW -> drawRainbow(time, rainbow.size)
            BackgroundTheme.AURORA -> drawAurora(time)
            BackgroundTheme.SUNSET -> drawSunset(time)
            BackgroundTheme.OCEAN -> drawOcean(time)
            BackgroundTheme.DEFAULT -> {}
        }
        // A light veil over the picture keeps text readable on every part of every theme.
        drawRect(if (theme.dark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.22f))
    }
}

private fun DrawScope.glow(c: Color, cx: Float, cy: Float, radius: Float) =
    drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), radius), radius, Offset(cx, cy))

private fun DrawScope.drawGalaxy(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B0620), Color(0xFF160A33), Color(0xFF081230))))
    val w = size.width; val h = size.height
    glow(Color(0x665B2BC0), w * (0.2f + 0.05f * wave(t, 1, 0f)), h * 0.25f, w * 0.8f)
    glow(Color(0x55D0409A), w * (0.85f + 0.04f * wave(t, 1, 0.3f)), h * 0.55f, w * 0.7f)
    glow(Color(0x552A5BD8), w * 0.4f, h * (0.9f + 0.03f * wave(t, 1, 0.6f)), w * 0.8f)
    for (s in STARS) {
        val a = 0.35f + 0.65f * (0.5f + 0.5f * wave(t, s.speed * 4, s.phase))
        drawCircle(Color.White.copy(alpha = a), s.r * density / 2.6f * 2f, Offset(s.x * w, s.y * h))
    }
}

private fun DrawScope.drawCandy(t: Float) {
    drawRect(Brush.linearGradient(listOf(Color(0xFFFFE3F1), Color(0xFFFFD6EC), Color(0xFFE3F4FF)), Offset.Zero, Offset(size.width, size.height)))
    val w = size.width; val h = size.height
    SWEETS.forEachIndexed { i, s ->
        val c = CANDY_COLORS[i % CANDY_COLORS.size]
        val x = s.x * w + 14f * density * wave(t, 1, s.phase)
        val y = s.y * h + 18f * density * wave(t, s.speed, s.phase + 0.25f)
        if (i % 3 == 0) {
            val len = s.r * density * 0.9f
            val ang = 2f * PI.toFloat() * (t * 2 + s.phase)
            drawLine(c.copy(alpha = 0.75f), Offset(x - cos(ang) * len / 2, y - sin(ang) * len / 2), Offset(x + cos(ang) * len / 2, y + sin(ang) * len / 2), strokeWidth = 6f * density, cap = StrokeCap.Round)
        } else {
            drawCircle(c.copy(alpha = 0.30f), s.r * density, Offset(x, y))
            drawCircle(Color.White.copy(alpha = 0.35f), s.r * density * 0.35f, Offset(x - s.r * density * 0.3f, y - s.r * density * 0.3f))
        }
    }
}

private fun DrawScope.drawRainbow(t: Float, n: Int) {
    val colors = List(n + 1) { Color.hsv(((it.toFloat() / n) + t) % 1f * 360f, 0.38f, 1f) }
    drawRect(Brush.linearGradient(colors, Offset.Zero, Offset(size.width, size.height)))
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent, Color.White.copy(alpha = 0.25f))))
}

private fun DrawScope.band(t: Float, baseY: Float, amp: Float, speed: Int, phase: Float, height: Float, c: Color) {
    val path = Path()
    val step = 14f * density
    var x = 0f
    path.moveTo(0f, baseY)
    while (x <= size.width + step) {
        path.lineTo(x, baseY + amp * sin(x / size.width * 5f + 2f * PI.toFloat() * (t * speed + phase)))
        x += step
    }
    path.lineTo(size.width + step, baseY + height); path.lineTo(0f, baseY + height); path.close()
    drawPath(path, Brush.verticalGradient(listOf(c, Color.Transparent), baseY - amp, baseY + height))
}

private fun DrawScope.drawAurora(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF04141F), Color(0xFF071F2B), Color(0xFF0A2A33))))
    val h = size.height
    band(t, h * 0.25f, h * 0.05f, 1, 0f, h * 0.35f, Color(0xAA2FE0A8))
    band(t, h * 0.38f, h * 0.06f, 2, 0.3f, h * 0.35f, Color(0x887B5CFF))
    band(t, h * 0.52f, h * 0.05f, 1, 0.6f, h * 0.3f, Color(0x6639C6E8))
    for (s in STARS.take(40)) drawCircle(Color.White.copy(alpha = 0.3f + 0.4f * (0.5f + 0.5f * wave(t, s.speed * 4, s.phase))), s.r * density, Offset(s.x * size.width, s.y * h * 0.5f))
}

private fun DrawScope.drawSunset(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1E0B3A), Color(0xFF5A1F6E), Color(0xFFA83A6E), Color(0xFFE0703F))))
    val w = size.width; val h = size.height
    val sy = h * (0.72f + 0.01f * wave(t, 1, 0f))
    glow(Color(0x88FFB070), w * 0.5f, sy, w * 0.9f)
    drawCircle(Color(0xFFFFC27A), w * 0.16f, Offset(w * 0.5f, sy))
    for (s in STARS.take(35)) drawCircle(Color.White.copy(alpha = 0.25f + 0.4f * (0.5f + 0.5f * wave(t, s.speed * 4, s.phase))), s.r * density, Offset(s.x * w, s.y * h * 0.4f))
    band(t, h * 0.80f, h * 0.012f, 1, 0.1f, h * 0.2f, Color(0x992B0E3A))
}

private fun DrawScope.drawOcean(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF021B3A), Color(0xFF05406B), Color(0xFF0A7C8C))))
    val h = size.height; val w = size.width
    band(t, h * 0.30f, h * 0.025f, 1, 0f, h * 0.7f, Color(0x4430B8D8))
    band(t, h * 0.50f, h * 0.03f, 2, 0.4f, h * 0.5f, Color(0x3350D8C8))
    band(t, h * 0.72f, h * 0.03f, 1, 0.7f, h * 0.3f, Color(0x44208C9C))
    SWEETS.take(10).forEach { s ->
        val y = ((s.y - t * s.speed) % 1f + 1f) % 1f
        drawCircle(Color.White.copy(alpha = 0.18f), s.r * density * 0.25f, Offset(s.x * w + 8f * density * wave(t, 2, s.phase), y * h))
    }
}
