package com.omnitools.app.ui.feedback

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** The short sounds OmniTools can play. */
enum class Cue { CLICK, TOGGLE, RUN, SUCCESS, ERROR }

/** Makes the sounds from maths, so the app needs no audio files. Pure functions (unit tested). */
object SoundSynth {
    const val SAMPLE_RATE = 22050

    /** One note: [freq] Hz lasting [ms] milliseconds, starting [startMs] into the sound. */
    data class Note(val freq: Double, val ms: Int, val startMs: Int = 0, val gain: Double = 1.0)

    fun notes(cue: Cue): List<Note> = when (cue) {
        Cue.CLICK -> listOf(Note(1900.0, 22, 0, 0.8), Note(950.0, 22, 0, 0.35))
        Cue.TOGGLE -> listOf(Note(1250.0, 40, 0, 0.7), Note(1750.0, 55, 35, 0.7))
        Cue.RUN -> listOf(Note(523.25, 70, 0, 0.7), Note(783.99, 110, 60, 0.7))
        Cue.SUCCESS -> listOf(Note(659.25, 80, 0, 0.7), Note(830.61, 80, 70, 0.7), Note(987.77, 160, 140, 0.7))
        Cue.ERROR -> listOf(Note(311.13, 110, 0, 0.7), Note(233.08, 190, 100, 0.7))
    }

    /** Mono 16-bit samples for [cue]. Loudness never reaches full scale, so it cannot clip. */
    fun render(cue: Cue, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val list = notes(cue)
        val totalMs = list.maxOf { it.startMs + it.ms } + 20
        val out = DoubleArray(sampleRate * totalMs / 1000)
        for (n in list) {
            val start = sampleRate * n.startMs / 1000
            val len = sampleRate * n.ms / 1000
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= out.size) break
                val t = i.toDouble() / sampleRate
                val attack = (i / (sampleRate * 0.003)).coerceAtMost(1.0)      // 3 ms fade-in avoids a pop
                val decay = exp(-5.0 * i / len)                                  // natural fade-out
                val release = ((len - i) / (sampleRate * 0.004)).coerceIn(0.0, 1.0)
                out[idx] += sin(2 * PI * n.freq * t) * attack * decay * release * n.gain * 0.45
            }
        }
        return ShortArray(out.size) { (out[it].coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt().toShort() }
    }
}
