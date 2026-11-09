package com.omnitools.app

import com.omnitools.app.tools.media.AudioPlan
import com.omnitools.app.tools.media.Container
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlanTest {
    @Test fun chooseContainer() {
        assertEquals(Container.M4A, AudioPlan.choose("audio/mp4a-latm", 34, false))
        assertEquals(Container.WAV, AudioPlan.choose("audio/mp4a-latm", 34, true))
        assertEquals(Container.OGG, AudioPlan.choose("audio/opus", 34, false))
        assertEquals(Container.WEBM, AudioPlan.choose("audio/opus", 28, false))
        assertEquals(Container.WEBM, AudioPlan.choose("audio/vorbis", 34, false))
        assertEquals(Container.WAV, AudioPlan.choose("audio/mpeg", 34, false))
        assertEquals(Container.WAV, AudioPlan.choose("audio/ac3", 34, false))
        assertEquals(Container.WAV, AudioPlan.choose(null, 34, false))
    }

    @Test fun namesAreSafeAndUnique() {
        assertEquals("Holiday.m4a", AudioPlan.outputName("Holiday.mp4", Container.M4A, emptySet()))
        assertEquals("Holiday (2).m4a", AudioPlan.outputName("Holiday.mp4", Container.M4A, setOf("Holiday.m4a")))
        assertEquals("Holiday (3).m4a", AudioPlan.outputName("Holiday.mp4", Container.M4A, setOf("holiday.M4A", "Holiday (2).m4a")))
        assertEquals("a b c.wav", AudioPlan.outputName("a/b:c.mkv", Container.WAV, emptySet()))
        assertEquals("audio.wav", AudioPlan.outputName(null, Container.WAV, emptySet()))
        assertEquals("audio.wav", AudioPlan.outputName("   .mp4", Container.WAV, emptySet()))
        assertEquals("my.clip.m4a", AudioPlan.outputName("my.clip.mp4", Container.M4A, emptySet()))
        assertTrue(AudioPlan.outputName("x".repeat(300) + ".mp4", Container.M4A, emptySet()).length <= 85)
    }

    @Test fun wavHeaderIsCorrect() {
        val h = AudioPlan.wavHeader(44100, 2, 1000)
        assertEquals(44, h.size)
        assertEquals("RIFF", String(h, 0, 4)); assertEquals("WAVE", String(h, 8, 4))
        assertEquals("fmt ", String(h, 12, 4)); assertEquals("data", String(h, 36, 4))
        val b = java.nio.ByteBuffer.wrap(h).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        assertEquals(1036, b.getInt(4))
        assertEquals(2, b.getShort(22).toInt())
        assertEquals(44100, b.getInt(24))
        assertEquals(176400, b.getInt(28))
        assertEquals(4, b.getShort(32).toInt())
        assertEquals(16, b.getShort(34).toInt())
        assertEquals(1000, b.getInt(40))
    }

    @Test fun labels() {
        assertEquals("AAC", AudioPlan.codecName("audio/mp4a-latm"))
        assertEquals("unknown", AudioPlan.codecName(null))
        assertEquals("stereo", AudioPlan.channelsLabel(2))
        assertEquals("6 channels", AudioPlan.channelsLabel(6))
        assertEquals("1.5 MB", AudioPlan.size(1_572_864))
        assertEquals("512 B", AudioPlan.size(512))
    }
}
