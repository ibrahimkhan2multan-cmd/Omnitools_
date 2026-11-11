package com.omnitools.app.ui.feedback

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** The short sounds OmniTools can play. */
enum class Cue { CLICK, TOGGLE, RUN, SUCCESS, ERROR, BACK }

/** A complete set of the sounds above in one style. */
enum class SoundTheme(val label: String, val description: String) {
    CLASSIC("Classic", "Crisp, short ticks"),
    GALAXY("Galaxy", "Airy, shimmering space tones"),
    BUBBLE("Bubble", "Soft water pops"),
    RETRO("Retro", "8-bit game blips"),
    CRYSTAL("Crystal", "Bright glass chimes"),
    WOOD("Wood", "Gentle wooden knocks");

    companion object {
        fun from(name: String?): SoundTheme = values().firstOrNull { it.name == name } ?: CLASSIC
    }
}

enum class Wave { SINE, SQUARE, TRIANGLE }

/** Makes the sounds from maths, so the app needs no audio files. Pure functions (unit tested). */
object SoundSynth {
    const val SAMPLE_RATE = 22050
    private const val TARGET_PEAK = 0.55   // every sound is scaled to this loudness, so none can clip and all feel similar

    /**
     * One note: [freq] Hz (gliding to [endFreq] if given) lasting [ms] milliseconds, starting [startMs] into the sound.
     * [partials] adds overtones as (multiple of the base pitch, loudness); [decay] is how fast it fades (higher = shorter).
     */
    data class Note(
        val freq: Double, val ms: Int, val startMs: Int = 0, val gain: Double = 1.0, val endFreq: Double? = null,
        val wave: Wave = Wave.SINE, val partials: List<Pair<Double, Double>> = emptyList(), val decay: Double = 5.0,
    )

    private val SHIMMER = listOf(2.0 to 0.35, 3.0 to 0.15)
    private val GLASS = listOf(2.76 to 0.5, 5.4 to 0.25)
    private val KNOCK = listOf(2.3 to 0.3)

    fun notes(theme: SoundTheme, cue: Cue): List<Note> = when (theme) {
        SoundTheme.CLASSIC -> when (cue) {
            Cue.CLICK -> listOf(Note(1900.0, 22, 0, 0.8), Note(950.0, 22, 0, 0.35))
            Cue.TOGGLE -> listOf(Note(1250.0, 40, 0, 0.7), Note(1750.0, 55, 35, 0.7))
            Cue.RUN -> listOf(Note(523.25, 70, 0, 0.7), Note(783.99, 110, 60, 0.7))
            Cue.SUCCESS -> listOf(Note(659.25, 80, 0, 0.7), Note(830.61, 80, 70, 0.7), Note(987.77, 160, 140, 0.7))
            Cue.ERROR -> listOf(Note(311.13, 110, 0, 0.7), Note(233.08, 190, 100, 0.7))
            Cue.BACK -> listOf(Note(1300.0, 24, 0, 0.7), Note(650.0, 26, 18, 0.5))
        }
        SoundTheme.GALAXY -> when (cue) {
            Cue.CLICK -> listOf(Note(1500.0, 90, 0, 0.8, 2300.0, partials = SHIMMER, decay = 4.5))
            Cue.TOGGLE -> listOf(Note(880.0, 90, 0, 0.7, partials = SHIMMER, decay = 4.0), Note(1320.0, 120, 60, 0.7, partials = SHIMMER, decay = 4.0))
            Cue.RUN -> listOf(Note(330.0, 260, 0, 0.8, 1500.0, partials = listOf(2.0 to 0.3), decay = 2.5), Note(1760.0, 160, 150, 0.4, partials = SHIMMER, decay = 4.0))
            Cue.SUCCESS -> listOf(
                Note(784.0, 170, 0, 0.6, partials = SHIMMER), Note(988.0, 170, 90, 0.6, partials = SHIMMER),
                Note(1175.0, 170, 180, 0.6, partials = SHIMMER), Note(1568.0, 320, 270, 0.7, partials = SHIMMER, decay = 3.5),
            )
            Cue.ERROR -> listOf(Note(620.0, 300, 0, 0.8, 180.0, partials = listOf(1.5 to 0.3), decay = 3.0))
            Cue.BACK -> listOf(Note(1500.0, 90, 0, 0.7, 800.0, partials = SHIMMER, decay = 4.5))
        }
        SoundTheme.BUBBLE -> when (cue) {
            Cue.CLICK -> listOf(Note(950.0, 55, 0, 0.9, 380.0, decay = 4.0))
            Cue.TOGGLE -> listOf(Note(520.0, 55, 0, 0.8, 1000.0), Note(1000.0, 70, 50, 0.8, 1500.0))
            Cue.RUN -> listOf(Note(500.0, 60, 0, 0.8, 900.0), Note(750.0, 60, 55, 0.8, 1300.0), Note(1000.0, 90, 110, 0.8, 1700.0))
            Cue.SUCCESS -> listOf(Note(700.0, 60, 0, 0.8, 1200.0), Note(900.0, 60, 70, 0.8, 1500.0), Note(1200.0, 110, 140, 0.8, 2000.0))
            Cue.ERROR -> listOf(Note(400.0, 90, 0, 0.9, 200.0), Note(300.0, 120, 90, 0.9, 140.0))
            Cue.BACK -> listOf(Note(700.0, 55, 0, 0.8, 260.0, decay = 4.0))
        }
        SoundTheme.RETRO -> when (cue) {
            Cue.CLICK -> listOf(Note(880.0, 32, 0, 0.6, wave = Wave.SQUARE, decay = 3.0))
            Cue.TOGGLE -> listOf(Note(660.0, 40, 0, 0.5, wave = Wave.SQUARE), Note(990.0, 55, 38, 0.5, wave = Wave.SQUARE))
            Cue.RUN -> listOf(Note(523.0, 55, 0, 0.5, wave = Wave.SQUARE), Note(659.0, 55, 55, 0.5, wave = Wave.SQUARE), Note(784.0, 90, 110, 0.5, wave = Wave.SQUARE))
            Cue.SUCCESS -> listOf(
                Note(523.0, 60, 0, 0.5, wave = Wave.SQUARE), Note(659.0, 60, 60, 0.5, wave = Wave.SQUARE),
                Note(784.0, 60, 120, 0.5, wave = Wave.SQUARE), Note(1047.0, 160, 180, 0.5, wave = Wave.SQUARE),
            )
            Cue.ERROR -> listOf(Note(220.0, 110, 0, 0.5, wave = Wave.SQUARE), Note(165.0, 180, 100, 0.5, wave = Wave.SQUARE))
            Cue.BACK -> listOf(Note(440.0, 36, 0, 0.6, wave = Wave.SQUARE, decay = 3.0))
        }
        SoundTheme.CRYSTAL -> when (cue) {
            Cue.CLICK -> listOf(Note(2300.0, 70, 0, 0.8, partials = GLASS, decay = 6.0))
            Cue.TOGGLE -> listOf(Note(1568.0, 80, 0, 0.7, partials = GLASS), Note(2093.0, 110, 60, 0.7, partials = GLASS))
            Cue.RUN -> listOf(Note(1175.0, 90, 0, 0.7, partials = GLASS), Note(1760.0, 150, 70, 0.7, partials = GLASS))
            Cue.SUCCESS -> listOf(Note(1319.0, 110, 0, 0.7, partials = GLASS), Note(1568.0, 110, 80, 0.7, partials = GLASS), Note(2093.0, 260, 160, 0.7, partials = GLASS))
            Cue.ERROR -> listOf(Note(554.0, 200, 0, 0.8, partials = GLASS, decay = 4.0), Note(415.0, 260, 110, 0.8, partials = GLASS, decay = 4.0))
            Cue.BACK -> listOf(Note(1760.0, 70, 0, 0.7, partials = GLASS, decay = 6.0))
        }
        SoundTheme.WOOD -> when (cue) {
            Cue.CLICK -> listOf(Note(420.0, 45, 0, 0.9, 210.0, partials = KNOCK, decay = 7.0))
            Cue.TOGGLE -> listOf(Note(320.0, 45, 0, 0.9, 200.0, partials = KNOCK, decay = 7.0), Note(480.0, 50, 40, 0.8, 300.0, partials = KNOCK, decay = 7.0))
            Cue.RUN -> listOf(Note(260.0, 60, 0, 0.9, 180.0, partials = KNOCK, decay = 6.0), Note(390.0, 70, 60, 0.9, 260.0, partials = KNOCK, decay = 6.0))
            Cue.SUCCESS -> listOf(
                Note(300.0, 60, 0, 0.9, 210.0, partials = KNOCK, decay = 6.0), Note(380.0, 60, 70, 0.9, 260.0, partials = KNOCK, decay = 6.0),
                Note(500.0, 110, 140, 0.9, 340.0, partials = KNOCK, decay = 5.0),
            )
            Cue.ERROR -> listOf(Note(200.0, 90, 0, 0.9, 110.0, partials = KNOCK, decay = 5.0), Note(150.0, 140, 90, 0.9, 80.0, partials = KNOCK, decay = 5.0))
            Cue.BACK -> listOf(Note(300.0, 50, 0, 0.8, 150.0, partials = KNOCK, decay = 7.0))
        }
    }

    private fun wave(w: Wave, phase: Double): Double = when (w) {
        Wave.SINE -> sin(phase)
        Wave.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
        Wave.TRIANGLE -> asin(sin(phase)) * 2 / PI
    }

    /** Mono 16-bit samples for [cue] in [theme]. Always scaled to the same loudness below full scale, so it cannot clip. */
    fun render(theme: SoundTheme, cue: Cue, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val list = notes(theme, cue)
        val totalMs = list.maxOf { it.startMs + it.ms } + 20
        val out = DoubleArray(sampleRate * totalMs / 1000)
        for (n in list) {
            val start = sampleRate * n.startMs / 1000
            val len = sampleRate * n.ms / 1000
            var phase = 0.0
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= out.size) break
                val f = if (n.endFreq == null) n.freq else n.freq * (n.endFreq / n.freq).pow(i.toDouble() / len)
                phase += 2 * PI * f / sampleRate
                val attack = (i / (sampleRate * 0.003)).coerceAtMost(1.0)      // 3 ms fade-in avoids a pop
                val decay = exp(-n.decay * i / len)                              // natural fade-out
                val release = ((len - i) / (sampleRate * 0.004)).coerceIn(0.0, 1.0)
                var v = wave(n.wave, phase)
                for ((ratio, amp) in n.partials) v += amp * sin(phase * ratio)
                out[idx] += v * attack * decay * release * n.gain
            }
        }
        val peak = out.maxOf { kotlin.math.abs(it) }
        val scale = if (peak > 0) TARGET_PEAK / peak else 0.0
        return ShortArray(out.size) { (out[it] * scale * Short.MAX_VALUE).toInt().toShort() }
    }

    fun render(cue: Cue, sampleRate: Int = SAMPLE_RATE): ShortArray = render(SoundTheme.CLASSIC, cue, sampleRate)
}
