package com.omnitools.app.ui.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

/** Plays the app's small sounds and haptic ticks. Both can be switched off in Settings. */
object Feedback {
    @Volatile var soundOn: Boolean = true
    @Volatile var hapticsOn: Boolean = true
    @Volatile var theme: SoundTheme = SoundTheme.CLASSIC
    @Volatile var volume: Float = 0.8f

    private val tracks = HashMap<Pair<SoundTheme, Cue>, AudioTrack>()
    private var lastClickAt = 0L

    /** Builds the sounds of the current theme ahead of time, so the first tap after a change is not late. */
    fun warmUp() {
        Thread {
            for (c in Cue.values()) { try { track(theme, c) } catch (_: Throwable) {} }
        }.start()
    }

    @Synchronized
    private fun track(t: SoundTheme, cue: Cue): AudioTrack = tracks.getOrPut(t to cue) {
        val data = SoundSynth.render(t, cue)
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SoundSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
            .also { it.write(data, 0, data.size) }
    }

    @Synchronized
    fun play(cue: Cue) {
        if (!soundOn) return
        try {
            val track = tracks.getOrPut(theme to cue) {
                val data = SoundSynth.render(theme, cue)
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SoundSynth.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size * 2)
                    .build()
                    .also { it.write(data, 0, data.size) }
            }
            track.stop()
            track.reloadStaticData()
            track.setVolume(volume.coerceIn(0f, 1f))
            track.play()
        } catch (_: Throwable) {
            // Sound is a nicety: never let it break a button.
        }
    }

    fun tick(view: View?) {
        if (hapticsOn && view != null) {
            try { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } catch (_: Throwable) {}
        }
    }

    /** The sound and tick for any tap. Two detectors can see the same tap, so repeats within a split second are ignored. */
    fun click(view: View?) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastClickAt < 90) return
        lastClickAt = now
        play(Cue.CLICK); tick(view)
    }

    /** The "back" sound, skipped when a tap on a back button has just made the normal click. */
    fun back(view: View?) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastClickAt < 300) return
        lastClickAt = now
        play(Cue.BACK); tick(view)
    }
}

/** Wraps the normal ripple and also plays the click sound when a press is released. */
private class SoundIndication(private val base: IndicationNodeFactory) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource, base.create(interactionSource))
    override fun equals(other: Any?) = other is SoundIndication && other.base == base
    override fun hashCode() = base.hashCode()

    private class Node(private val source: InteractionSource, inner: DelegatableNode) : DelegatingNode(), androidx.compose.ui.node.CompositionLocalConsumerModifierNode {
        init { delegate(inner) }
        override fun onAttach() {
            val view: View = currentValueOf(LocalView)
            coroutineScope.launch {
                source.interactions.collect { interaction: Interaction ->
                    if (interaction is PressInteraction.Release) Feedback.click(view)
                }
            }
        }
    }
}

/** Gives every clickable inside [content] a ripple plus a click sound. */
@Composable
fun WithClickSounds(content: @Composable () -> Unit) {
    val indication = androidx.compose.runtime.remember { SoundIndication(ripple()) }
    CompositionLocalProvider(LocalIndication provides indication) { ClickSoundHost(content) }
}

/**
 * Plays the click for every tap that something handled, wherever it is on screen. Material icon buttons (back arrows,
 * toolbar icons), switches and custom cards draw their own press effect and would otherwise stay silent.
 * It only watches: it never consumes touches, and drags or scrolls (finger moved) make no sound.
 */
private fun Modifier.clickSoundOnTaps(view: View): Modifier = pointerInput(view) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        var moved = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            if (event.changes.size > 1) moved = true
            val c = event.changes.firstOrNull { it.id == down.id } ?: break
            if ((c.position - down.position).getDistance() > slop) moved = true
            if (c.previousPressed && !c.pressed) {
                // A button that handled the tap consumes the release; empty space does not.
                if (!moved && c.isConsumed) Feedback.click(view)
                break
            }
            if (!c.pressed) break
        }
    }
}

@Composable
private fun ClickSoundHost(content: @Composable () -> Unit) {
    val view = LocalView.current
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().clickSoundOnTaps(view)) { content() }
}
