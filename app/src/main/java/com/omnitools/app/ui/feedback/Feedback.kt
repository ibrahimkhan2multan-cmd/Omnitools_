package com.omnitools.app.ui.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

/** Plays the app's small sounds and haptic ticks. Both can be switched off in Settings. */
object Feedback {
    @Volatile var soundOn: Boolean = true
    @Volatile var hapticsOn: Boolean = true

    private val tracks = HashMap<Cue, AudioTrack>()

    @Synchronized
    fun play(cue: Cue) {
        if (!soundOn) return
        try {
            val track = tracks.getOrPut(cue) {
                val data = SoundSynth.render(cue)
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

    fun click(view: View?) { play(Cue.CLICK); tick(view) }
}

/** Wraps the normal ripple and also plays the click sound when a press is released. */
private class SoundIndication(private val base: IndicationNodeFactory) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource, base.create(interactionSource))
    override fun equals(other: Any?) = other is SoundIndication && other.base == base
    override fun hashCode() = base.hashCode()

    private class Node(private val source: InteractionSource, inner: DelegatableNode) : DelegatingNode() {
        init { delegate(inner) }
        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect { if (it is PressInteraction.Release) Feedback.click(currentValueOf(LocalView)) }
            }
        }
    }
}

/** Gives every clickable inside [content] a ripple plus a click sound. */
@Composable
fun WithClickSounds(content: @Composable () -> Unit) {
    val indication = androidx.compose.runtime.remember { SoundIndication(ripple()) }
    CompositionLocalProvider(LocalIndication provides indication, content = content)
}
