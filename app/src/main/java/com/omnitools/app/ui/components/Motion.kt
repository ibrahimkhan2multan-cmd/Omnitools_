package com.omnitools.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Clickable with press feedback: the element shrinks a little while pressed and springs back. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
}

/** Remembers which list items have already played their entrance, so scrolling and returning do not replay it. */
object EntranceMemory {
    val seen = mutableSetOf<String>()
}

/** Fades and slides its content in, once per app run, staggered by [index]. */
@Composable
fun Entrance(key: String, index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val alreadySeen = remember { key in EntranceMemory.seen }
    val progress = remember { Animatable(if (alreadySeen) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadySeen) {
            EntranceMemory.seen.add(key)
            delay(index.coerceAtMost(10) * 45L)
            progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1f - progress.value) * 36.dp.toPx()
        },
    ) { content() }
}

/**
 * The OmniTools mark, drawn in code: four petals that spring in one after another, then a hub that gently pulses.
 * Same shapes and colors as the launcher icon.
 */
@Composable
fun OmniLogo(size: Dp, modifier: Modifier = Modifier) {
    val petals = remember { List(4) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        // Skip the intro on later visits so the logo is not replayed every time Home opens.
        if (EntranceMemory.seen.add("logo")) {
            for (i in 0..3) {
                delay(i * 90L)
                petals[i].animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
            }
        } else {
            petals.forEach { it.snapTo(1f) }
        }
    }
    val pulse = rememberInfiniteTransition(label = "hub").animateFloat(
        initialValue = 0.85f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hub pulse",
    )

    Canvas(modifier.size(size)) {
        // Icon coordinates run 33.5..74.5; map that square onto the whole canvas.
        val unit = this.size.minDimension / 41f
        fun px(v: Float) = (v - 33.5f) * unit

        fun petal(x: Float, y: Float, tl: Float, tr: Float, br: Float, bl: Float): Path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = px(x), top = px(y), right = px(x + 18f), bottom = px(y + 18f),
                    topLeftCornerRadius = CornerRadius(tl * unit), topRightCornerRadius = CornerRadius(tr * unit),
                    bottomRightCornerRadius = CornerRadius(br * unit), bottomLeftCornerRadius = CornerRadius(bl * unit),
                ),
            )
        }

        data class Spec(val path: Path, val a: Color, val b: Color, val cx: Float, val cy: Float)
        val specs = listOf(
            Spec(petal(33.5f, 33.5f, 9f, 3f, 3f, 3f), Color(0xFF4F8CFF), Color(0xFF22D3EE), 42.5f, 42.5f),
            Spec(petal(56.5f, 33.5f, 3f, 9f, 3f, 3f), Color(0xFF8B6CFF), Color(0xFFC084FC), 65.5f, 42.5f),
            Spec(petal(56.5f, 56.5f, 3f, 3f, 9f, 3f), Color(0xFFFFA94D), Color(0xFFFF5C8A), 65.5f, 65.5f),
            Spec(petal(33.5f, 56.5f, 3f, 3f, 3f, 9f), Color(0xFF34D399), Color(0xFF22D3EE), 42.5f, 65.5f),
        )
        specs.forEachIndexed { i, s ->
            val p = petals[i].value.coerceAtLeast(0f)
            scale(scale = p, pivot = Offset(px(s.cx), px(s.cy))) {
                drawPath(s.path, Brush.linearGradient(listOf(s.a, s.b), Offset(px(s.cx - 9f), px(s.cy - 9f)), Offset(px(s.cx + 9f), px(s.cy + 9f))))
            }
        }
        drawCircle(Color.White, radius = 4.2f * unit * pulse.value * petals[3].value.coerceIn(0f, 1f), center = Offset(px(54f), px(54f)))
    }
}
