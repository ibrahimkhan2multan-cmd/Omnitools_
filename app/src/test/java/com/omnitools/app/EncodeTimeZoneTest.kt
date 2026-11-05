package com.omnitools.app

import com.omnitools.app.tools.dev.EncodeKind
import com.omnitools.app.tools.dev.EncodeTools
import com.omnitools.app.tools.dev.TimeZones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDateTime

class EncodeTimeZoneTest {
    @Test fun roundTripsEveryKind() {
        val text = "Héllo wörld ✓ <a href=\"x\">&'</a>"
        for (k in EncodeKind.values()) assertEquals(k.name, text, EncodeTools.decode(k, EncodeTools.encode(k, text)))
    }

    @Test fun knownValues() {
        assertEquals("aGVsbG8=", EncodeTools.encode(EncodeKind.BASE64, "hello"))
        assertEquals("a%20b%26c", EncodeTools.encode(EncodeKind.URL, "a b&c"))
        assertEquals("68 69", EncodeTools.encode(EncodeKind.HEX, "hi"))
        assertEquals("01101000 01101001", EncodeTools.encode(EncodeKind.BINARY, "hi"))
        assertEquals("a+b c", EncodeTools.decode(EncodeKind.URL, "a%2Bb%20c"))
    }

    @Test fun invalidInputGivesReadableError() {
        for ((k, bad) in listOf(EncodeKind.BASE64 to "@@@", EncodeKind.HEX to "xyz", EncodeKind.BINARY to "0102", EncodeKind.HEX to "ff")) {
            try { EncodeTools.decode(k, bad); fail("$k $bad") } catch (e: IllegalArgumentException) { assertTrue(e.message!!.isNotBlank()) }
        }
    }

    @Test fun convertsAcrossZonesAndDays() {
        val rows = TimeZones.convert(LocalDateTime.of(2026, 11, 5, 23, 30), "UTC", listOf("Asia/Karachi", "America/New_York", "Nope/Zone"))
        assertEquals(2, rows.size)
        assertEquals("04:30", rows[0].time); assertEquals(1, rows[0].dayShift); assertEquals("+05:00", rows[0].offset)
        assertEquals("18:30", rows[1].time); assertEquals(0, rows[1].dayShift)
    }

    @Test fun parsesTimesAndSearches() {
        assertEquals("09:05", TimeZones.parseTime("9:05")?.toString())
        assertNull(TimeZones.parseTime("25:00")); assertNull(TimeZones.parseTime("abc"))
        assertTrue("Asia/Tokyo" in TimeZones.search("tokyo"))
    }
}
