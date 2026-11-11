package com.omnitools.app

import com.omnitools.app.ui.feedback.Cue
import com.omnitools.app.ui.feedback.SoundSynth
import com.omnitools.app.ui.feedback.SoundTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundSynthTest {
    @Test fun everyThemeAndCueIsAValidShortSound() {
        for (t in SoundTheme.values()) for (c in Cue.values()) {
            val s = SoundSynth.render(t, c)
            val peak = s.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$t $c too short", s.size > SoundSynth.SAMPLE_RATE / 100)
            assertTrue("$t $c too long", s.size < SoundSynth.SAMPLE_RATE)
            assertTrue("$t $c too quiet", peak > 1000)
            assertTrue("$t $c too loud", peak < Short.MAX_VALUE * 0.9)
            assertTrue("$t $c starts abruptly", kotlin.math.abs(s.first().toInt()) < 200)
            assertTrue("$t $c ends abruptly", kotlin.math.abs(s.last().toInt()) < 200)
        }
    }

    @Test fun themesSoundDifferent() {
        for (c in Cue.values()) {
            val seen = SoundTheme.values().map { SoundSynth.render(it, c).toList() }
            assertEquals("$c", seen.size, seen.toSet().size)
        }
    }

    @Test fun themeLookupFallsBackToClassic() {
        assertEquals(SoundTheme.GALAXY, SoundTheme.from("GALAXY"))
        assertEquals(SoundTheme.CLASSIC, SoundTheme.from("nonsense"))
        assertEquals(SoundTheme.CLASSIC, SoundTheme.from(null))
        assertFalse(SoundTheme.values().any { it.label.isBlank() })
    }
}
