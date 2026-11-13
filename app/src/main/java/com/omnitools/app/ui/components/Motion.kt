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
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Clickable with press feedback: the element shrinks a little while pressed and springs back. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduced = com.omnitools.app.ui.theme.rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.97f else 1f,
        animationSpec = tween(com.omnitools.app.ui.theme.Dur.PRESS, easing = FastOutSlowInEasing),
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
    val reduced = com.omnitools.app.ui.theme.rememberReducedMotion()
    val alreadySeen = remember { reduced || key in EntranceMemory.seen }
    val progress = remember { Animatable(if (alreadySeen) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadySeen) {
            EntranceMemory.seen.add(key)
            delay(index.coerceAtMost(6) * 35L)
            progress.animateTo(1f, tween(com.omnitools.app.ui.theme.Dur.MEDIUM + 60, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1f - progress.value) * 18.dp.toPx()
        },
    ) { content() }
}

/**
 * The OmniTools mark, drawn in code: six colour blades that spin and spring in one after another around a hub that
 * gently pulses. Same shapes and colours as the launcher icon.
 */
@Composable
fun OmniLogo(size: Dp, modifier: Modifier = Modifier) {
    // Decided before the first frame, so later visits never draw an empty logo for a moment.
    val reduced = com.omnitools.app.ui.theme.rememberReducedMotion()
    val firstTime = remember { !reduced && "logo" !in EntranceMemory.seen }
    val blades = remember { List(6) { Animatable(if (firstTime) 0f else 1f) } }
    val spin = remember { Animatable(if (firstTime) 0f else 1f) }
    LaunchedEffect(Unit) {
        // Skip the intro on later visits so the logo is not replayed every time Home opens.
        if (firstTime && EntranceMemory.seen.add("logo")) {
            launch { spin.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            for (i in 0..5) {
                delay(i * 70L)
                launch { blades[i].animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
            }
        } else {
            spin.snapTo(1f)
            blades.forEach { it.snapTo(1f) }
        }
    }
    val pulseAnim = rememberInfiniteTransition(label = "hub").animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hub pulse",
    )
    val pulse = if (reduced) androidx.compose.runtime.rememberUpdatedState(1f) else pulseAnim
    val bladePath = remember { PathParser().parsePathString("M54,54 C61,47 63,36 54,25 C68,26 79,38 73,52 C70,58 62,58 54,54 Z").toPath() }
    val colors = remember {
        listOf(0xFF4F8CFF to 0xFF22D3EE, 0xFF22D3EE to 0xFF34D399, 0xFF34D399 to 0xFFFACC15,
            0xFFFFA94D to 0xFFFF5C8A, 0xFFFF5C8A to 0xFFC084FC, 0xFFC084FC to 0xFF4F8CFF)
    }

    Canvas(modifier.size(size)) {
        // Blade geometry spans roughly 25..79 in a 108 box; map that onto the whole canvas.
        val unit = this.size.minDimension / 62f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        withTransform({
            translate(center.x - 54f * unit, center.y - 54f * unit)
            scale(unit, unit, pivot = Offset.Zero)
        }) {
            rotate(degrees = (1f - spin.value) * -120f, pivot = Offset(54f, 54f)) {
                colors.forEachIndexed { i, (a, b) ->
                    val p = blades[i].value.coerceAtLeast(0f)
                    rotate(degrees = i * 60f, pivot = Offset(54f, 54f)) {
                        scale(scale = p, pivot = Offset(54f, 54f)) {
                            drawPath(bladePath, Brush.linearGradient(listOf(Color(a), Color(b)), Offset(54f, 56f), Offset(70f, 30f)))
                        }
                    }
                }
            }
            val hub = blades[5].value.coerceIn(0f, 1f)
            drawCircle(Color(0xFF0A0F24), radius = 7.5f * hub, center = Offset(54f, 54f))
            drawCircle(Color.White, radius = 5f * pulse.value * hub, center = Offset(54f, 54f))
        }
    }
}
