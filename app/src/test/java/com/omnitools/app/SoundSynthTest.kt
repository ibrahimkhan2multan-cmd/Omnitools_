package com.omnitools.app

import com.omnitools.app.ui.feedback.Cue
import com.omnitools.app.ui.feedback.SoundSynth
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundSynthTest {
    @Test fun everyCueHasSoundThatNeverClips() {
        for (cue in Cue.values()) {
            val s = SoundSynth.render(cue)
            assertTrue("$cue empty", s.size > SoundSynth.SAMPLE_RATE / 100)
            assertTrue("$cue too long", s.size < SoundSynth.SAMPLE_RATE)
            val peak = s.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$cue silent", peak > 1000)
            assertTrue("$cue clips", peak < Short.MAX_VALUE * 0.9 + 1)
        }
    }

    @Test fun soundsStartAndEndSilently() {
        for (cue in Cue.values()) {
            val s = SoundSynth.render(cue)
            assertTrue(kotlin.math.abs(s.first().toInt()) < 200)
            assertTrue(kotlin.math.abs(s.last().toInt()) < 200)
        }
    }
}
