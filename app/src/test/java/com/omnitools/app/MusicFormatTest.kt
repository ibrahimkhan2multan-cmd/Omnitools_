package com.omnitools.app

import com.omnitools.app.tools.music.MusicFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicFormatTest {
    @Test fun timeFormats() {
        assertEquals("0:00", MusicFormat.time(0))
        assertEquals("0:00", MusicFormat.time(-5000))
        assertEquals("0:59", MusicFormat.time(59_999))
        assertEquals("1:23", MusicFormat.time(83_000))
        assertEquals("1:01:40", MusicFormat.time(3_700_000))
    }

    @Test fun fractionAndSeekStayInRange() {
        assertEquals(0f, MusicFormat.fraction(5, 0), 0f)
        assertEquals(0.5f, MusicFormat.fraction(50, 100), 0f)
        assertEquals(1f, MusicFormat.fraction(500, 100), 0f)
        assertEquals(0f, MusicFormat.fraction(-5, 100), 0f)
        assertEquals(0, MusicFormat.seekTarget(0.5f, 0))
        assertEquals(100, MusicFormat.seekTarget(2f, 100))
        assertEquals(0, MusicFormat.seekTarget(-1f, 100))
        assertEquals(50, MusicFormat.seekTarget(0.5f, 100))
    }

    @Test fun titles() {
        assertEquals("Audio file", MusicFormat.title(null))
        assertEquals("Audio file", MusicFormat.title("   "))
        assertEquals("a.mp3", MusicFormat.title(" a.mp3 "))
        assertEquals("My Song", MusicFormat.titleNoExtension("My Song.mp3"))
        assertEquals("v1.2 mix", MusicFormat.titleNoExtension("v1.2 mix.flac"))
        assertEquals(".hidden", MusicFormat.titleNoExtension(".hidden"))
        assertEquals("no extension", MusicFormat.titleNoExtension("no extension"))
    }
}
