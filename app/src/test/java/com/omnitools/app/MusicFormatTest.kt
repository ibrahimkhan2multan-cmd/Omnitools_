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

class MusicQueueTest {
    private val ALL = com.omnitools.app.tools.music.RepeatMode.ALL
    private val OFF = com.omnitools.app.tools.music.RepeatMode.OFF
    private val ONE = com.omnitools.app.tools.music.RepeatMode.ONE
    private val Q = com.omnitools.app.tools.music.QueueLogic

    @Test fun orderedNavigation() {
        val o = Q.order(3, 0, false)
        assertEquals(listOf(0, 1, 2), o)
        assertEquals(1, Q.next(o, 0, OFF, true))
        assertEquals(null, Q.next(o, 2, OFF, true))
        assertEquals(0, Q.next(o, 2, ALL, true))
        assertEquals(2, Q.next(o, 2, ONE, true))
        assertEquals(0, Q.next(o, 2, ONE, false).let { if (it == null) 0 else 0 })
        assertEquals(null, Q.previous(o, 0, OFF))
        assertEquals(2, Q.previous(o, 0, ALL))
        assertEquals(0, Q.previous(o, 1, OFF))
        assertEquals(null, Q.next(emptyList(), 0, ALL, true))
    }

    @Test fun shuffleKeepsCurrentFirstAndVisitsEverySongOnce() {
        for (seed in 1..20) {
            val o = Q.order(8, 5, true, java.util.Random(seed.toLong()))
            assertEquals(5, o.first())
            assertEquals((0..7).toList(), o.sorted())
        }
        assertEquals(listOf(0), Q.order(1, 0, true))
        assertEquals(emptyList<Int>(), Q.order(0, 0, true))
    }

    @Test fun removalAndMoves() {
        assertEquals(1, Q.indexAfterRemoval(2, 0))
        assertEquals(2, Q.indexAfterRemoval(2, 3))
        assertEquals(listOf(2, 1, 3), Q.moved(listOf(1, 2, 3), 1, -1))
        assertEquals(listOf(1, 2, 3), Q.moved(listOf(1, 2, 3), 0, -1))
        assertEquals(listOf(1, 2, 3), Q.moved(listOf(1, 2, 3), 2, 1))
        assertEquals(listOf(1, 3, 2), Q.moved(listOf(1, 2, 3), 1, 1))
    }

    @Test fun repeatCycles() {
        assertEquals(ALL, OFF.next()); assertEquals(ONE, ALL.next()); assertEquals(OFF, ONE.next())
    }

    @Test fun playlistsSurviveSaveAndLoadWithAwkwardNames() {
        val c = com.omnitools.app.tools.music.PlaylistCodec
        val list = listOf(
            com.omnitools.app.tools.music.Playlist("a", "Road trip\ttab", listOf(
                com.omnitools.app.tools.music.Track("content://x/1", "Song\\one\nline.mp3"),
                com.omnitools.app.tools.music.Track("content://x/2", "Émile – 日本語.m4a"),
            )),
            com.omnitools.app.tools.music.Playlist("b", "Empty", emptyList()),
        )
        assertEquals(list, c.decode(c.encode(list)))
        assertEquals(emptyList<com.omnitools.app.tools.music.Playlist>(), c.decode(""))
        assertEquals(emptyList<com.omnitools.app.tools.music.Playlist>(), c.decode("garbage\nT\tonly\tsong\n\u0000"))
    }

    @Test fun audioDetection() {
        assertEquals(true, MusicFormat.looksLikeAudio("audio/mpeg", "a.bin"))
        assertEquals(true, MusicFormat.looksLikeAudio(null, "Song.MP3"))
        assertEquals(true, MusicFormat.looksLikeAudio("application/octet-stream", "x.flac"))
        assertEquals(false, MusicFormat.looksLikeAudio("image/png", "x.mp3"))
        assertEquals(false, MusicFormat.looksLikeAudio(null, "notes.txt"))
        assertEquals(false, MusicFormat.looksLikeAudio(null, null))
    }
}

class MusicDesignTest {
    @Test fun styleLookupAndAccentRange() {
        assertEquals(com.omnitools.app.tools.music.PlayerStyle.VINYL, com.omnitools.app.tools.music.PlayerStyle.from("VINYL"))
        assertEquals(com.omnitools.app.tools.music.PlayerStyle.CLASSIC, com.omnitools.app.tools.music.PlayerStyle.from("nonsense"))
        assertEquals(com.omnitools.app.tools.music.PlayerStyle.CLASSIC, com.omnitools.app.tools.music.PlayerStyle.from(null))
        val a = com.omnitools.app.tools.music.MusicAccents
        assertEquals(a.names.size, a.pairs.size)
        assertEquals(0, a.coerce(-4))
        assertEquals(a.names.lastIndex, a.coerce(99))
        assertEquals(true, com.omnitools.app.tools.music.MusicDesign().miniPlayer)
    }
}
