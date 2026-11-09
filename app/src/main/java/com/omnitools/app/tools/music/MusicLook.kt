package com.omnitools.app.tools.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colours used to draw the player in one style. */
class PlayerColors(
    val brush: Brush?, val container: Color, val fg: Color, val sub: Color,
    val buttonBg: Color, val buttonFg: Color, val accentA: Color, val accentB: Color,
)

@Composable
fun accentPair(design: MusicDesign): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    if (design.accent == 0) return cs.primary to cs.tertiary
    val p = MusicAccents.pairs[MusicAccents.coerce(design.accent)]
    return Color(p.first) to Color(p.second)
}

@Composable
fun playerColors(design: MusicDesign): PlayerColors {
    val cs = MaterialTheme.colorScheme
    val (a, b) = accentPair(design)
    return when (design.style) {
        PlayerStyle.GRADIENT -> PlayerColors(Brush.linearGradient(listOf(a, b)), Color.Transparent, Color.White, Color.White.copy(alpha = 0.8f), Color.White, a, a, b)
        PlayerStyle.VINYL -> PlayerColors(null, Color(0xFF14161C), Color.White, Color.White.copy(alpha = 0.7f), a, Color.White, a, b)
        PlayerStyle.MINIMAL -> PlayerColors(null, cs.surface, cs.onSurface, cs.onSurfaceVariant, a, Color.White, a, b)
        PlayerStyle.CLASSIC -> PlayerColors(null, cs.surface, cs.onSurface, cs.onSurfaceVariant, a, Color.White, a, b)
    }
}

/** The picture next to the song name: a pulsing disc, a spinning record, or nothing (Minimal). */
@Composable
fun Artwork(design: MusicDesign, playing: Boolean, loading: Boolean, size: Dp) {
    val (a, b) = accentPair(design)
    when (design.style) {
        PlayerStyle.MINIMAL -> if (loading) CircularProgressIndicator(Modifier.size(size / 2)) else Unit
        PlayerStyle.VINYL -> {
            var angle by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(playing) {
                var last = 0L
                while (playing) {
                    androidx.compose.runtime.withFrameNanos { t ->
                        if (last != 0L) angle = (angle + (t - last) / 1_000_000_000f * 90f) % 360f
                        last = t
                    }
                }
            }
            Box(Modifier.size(size).rotate(angle), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(size)) {
                    val r = this.size.minDimension / 2f
                    drawCircle(Color(0xFF0B0C10), r)
                    for (i in 1..5) drawCircle(Color.White.copy(alpha = 0.07f), r * (0.45f + i * 0.09f), style = Stroke(1.2f))
                    drawCircle(Brush.linearGradient(listOf(a, b), Offset.Zero, Offset(r * 2, r * 2)), r * 0.36f)
                    drawCircle(Color(0xFF0B0C10), r * 0.06f)
                }
            }
        }
        else -> {
            val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "disc").animateFloat(
                initialValue = 1f, targetValue = 1.06f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "disc pulse",
            )
            val onBlob = if (design.style == PlayerStyle.GRADIENT) Color.White.copy(alpha = 0.22f) else Color.Transparent
            Box(
                Modifier.size(size).scale(if (playing) pulse.value else 1f).background(
                    if (design.style == PlayerStyle.GRADIENT) Brush.linearGradient(listOf(onBlob, onBlob)) else Brush.linearGradient(listOf(a, b)),
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(size / 2.6f))
                else Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(size / 2.1f))
            }
        }
    }
}
