package com.omnitools.app

import com.omnitools.app.tools.advanced.Chmod
import com.omnitools.app.tools.advanced.CsvJson
import com.omnitools.app.tools.advanced.Json
import com.omnitools.app.tools.advanced.TextDiff
import com.omnitools.app.tools.net.Ipv4
import com.omnitools.app.tools.net.Ipv6
import com.omnitools.app.tools.net.NetReference
import com.omnitools.app.tools.net.UrlParts
import com.omnitools.app.tools.qr.QrInspector
import com.omnitools.app.tools.security.Passphrase
import com.omnitools.app.tools.security.PasswordStrength
import com.omnitools.app.tools.security.TextCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NewSectionsTest {
    @Test fun subnet24() {
        val s = Ipv4.subnet("192.168.1.10/24")
        assertEquals("192.168.1.0", Ipv4.format(s.network))
        assertEquals("192.168.1.255", Ipv4.format(s.broadcast))
        assertEquals("192.168.1.1", Ipv4.format(s.firstHost))
        assertEquals("192.168.1.254", Ipv4.format(s.lastHost))
        assertEquals(254L, s.hosts)
    }
    @Test fun subnetBadInputRejected() {
        for (bad in listOf("999.1.1.1/24", "1.2.3.4/33", "abc", "1.2.3/24")) {
            try { Ipv4.subnet(bad); fail("accepted $bad") } catch (e: IllegalArgumentException) { }
        }
    }
    @Test fun maskAndPrefix() {
        assertEquals(24, Ipv4.prefixOf(Ipv4.maskOf(24)))
        assertEquals("255.255.255.0", Ipv4.format(Ipv4.maskOf(24)))
        assertEquals(0L, Ipv4.maskOf(0))
        assertNull(Ipv4.prefixOf(0xFF00FF00L))
    }
    @Test fun ipv4ParseFormat() {
        assertEquals("10.0.0.1", Ipv4.format(Ipv4.parse("10.0.0.1")!!))
        assertNull(Ipv4.parse("256.0.0.1"))
        assertEquals("11000000.10101000.00000001.00000001", Ipv4.binary(Ipv4.parse("192.168.1.1")!!))
    }
    @Test fun ipv6RoundTrip() {
        val g = Ipv6.parse("2001:db8::1")!!
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001", Ipv6.expand(g))
        assertEquals("2001:db8::1", Ipv6.compress(g))
        assertNull(Ipv6.parse("2001:::1"))
        assertNull(Ipv6.parse("12345::1"))
    }
    @Test fun urlParts() {
        val p = UrlParts.parse("https://user@example.com:8443/a/b.html?q=hello%20world&x=1#top")
        assertEquals("https", p.scheme)
        assertEquals("example.com", p.host)
        assertEquals(8443, p.port)
        assertEquals("/a/b.html", p.path)
        assertEquals("top", p.fragment)
        assertEquals("hello world", p.query.first { it.first == "q" }.second)
        assertEquals("user", p.userInfo)
    }
    @Test fun referenceSearch() {
        assertTrue(NetReference.search(NetReference.httpStatus, "404").any { it.key == "404" })
        assertTrue(NetReference.search(NetReference.ports, "ssh").any { it.key == "22" })
        assertEquals(NetReference.ports.size, NetReference.search(NetReference.ports, "").size)
        assertTrue(NetReference.httpStatus.map { it.key }.toSet().size == NetReference.httpStatus.size)
    }

    @Test fun jsonFormatAndErrors() {
        assertEquals("{\"a\":1,\"b\":[true,null]}", Json.format("{ \"a\" : 1, \"b\" : [ true , null ] }", 0))
        assertEquals("{\"a\":2,\"b\":1}", Json.format("{\"b\":1,\"a\":2}", 0, true))
        for (bad in listOf("{", "{'a':1}", "[1,]", "{\"a\":1,}", "01", "[1 2]", "nul", "\"abc")) {
            try { Json.parse(bad); fail("accepted $bad") } catch (e: Exception) { assertTrue(e is Json.JsonException || e is IllegalArgumentException) }
        }
    }
    @Test fun jsonDepthLimit() {
        val deep = "[".repeat(5000) + "]".repeat(5000)
        try { Json.parse(deep); fail("deep accepted") } catch (e: Exception) { }
    }
    @Test fun diffCounts() {
        val r = TextDiff.diff("a\nb\nc", "a\nx\nc")
        assertEquals(1, r.added); assertEquals(1, r.removed); assertEquals(2, r.same)
        val same = TextDiff.diff("one", "ONE", ignoreCase = true)
        assertEquals(0, same.added); assertEquals(0, same.removed)
    }
    @Test fun diffLimit() {
        try { TextDiff.diff("x\n".repeat(2000), "y"); fail() } catch (e: IllegalArgumentException) { }
    }
    @Test fun csvJson() {
        val j = CsvJson.csvToJson("name,age\nAli,30\n\"Smith, J\",41")
        assertTrue(j.contains("\"Smith, J\""))
        val back = CsvJson.jsonToCsv(j)
        assertTrue(back.startsWith("name,age"))
        assertTrue(back.contains("\"Smith, J\""))
    }
    @Test fun chmod() {
        assertEquals("rwxr-xr-x", Chmod.fromOctal("755").symbolic)
        assertEquals("rwsr-xr-x", Chmod.fromOctal("4755").symbolic)
        assertEquals("rw-r--r--", Chmod.fromOctal("644").symbolic)
        assertEquals("755", Chmod.fromBits(Chmod.toBits("755")))
        try { Chmod.fromOctal("789"); fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun passwordStrengthOrdering() {
        val weak = PasswordStrength.evaluate("password")
        val strong = PasswordStrength.evaluate("k9#Lm2\$vQx8!zP4w")
        assertTrue(strong.bits > weak.bits)
        assertTrue(weak.tips.isNotEmpty())
    }
    @Test fun passphraseList() {
        assertTrue(Passphrase.words.size >= 400)
        assertEquals(Passphrase.words.size, Passphrase.words.toSet().size)
        assertTrue(Passphrase.words.all { w -> w.isNotEmpty() && w.all { it in 'a'..'z' } })
    }
    @Test fun passphraseGenerate() {
        val r = Passphrase.generate(5, "-")
        assertEquals(5, r.text.split("-").size)
        assertTrue(r.bits > 40)
        try { Passphrase.generate(2); fail() } catch (e: IllegalArgumentException) { }
        try { Passphrase.generate(13); fail() } catch (e: IllegalArgumentException) { }
    }
    @Test fun cryptoRoundTrip() {
        val t = TextCrypto.encrypt("héllo secret ✓", "correct horse", 1000)
        assertTrue(t.startsWith("OT1."))
        assertEquals("héllo secret ✓", TextCrypto.decrypt(t, "correct horse", 1000))
        assertNotEquals(t, TextCrypto.encrypt("héllo secret ✓", "correct horse", 1000))
    }
    @Test fun cryptoWrongPasswordAndTamper() {
        val t = TextCrypto.encrypt("data", "correct horse", 1000)
        try { TextCrypto.decrypt(t, "wrong horse", 1000); fail() } catch (e: IllegalArgumentException) { }
        val tampered = t.dropLast(2) + (if (t.last() == 'A') "BB" else "AA")
        try { TextCrypto.decrypt(tampered, "correct horse", 1000); fail() } catch (e: IllegalArgumentException) { }
        try { TextCrypto.encrypt("x", "123", 1000); fail() } catch (e: IllegalArgumentException) { }
        try { TextCrypto.decrypt("garbage", "correct horse", 1000); fail() } catch (e: IllegalArgumentException) { }
    }
    @Test fun qrInspect() {
        val w = QrInspector.inspect("WIFI:T:WPA;S:Home;P:secret;;")
        assertTrue(w.type.contains("Wi-Fi", ignoreCase = true))
        assertTrue(w.fields.any { it.second == "Home" })
        assertTrue(QrInspector.inspect("hello there").type.isNotBlank())
        assertNotNull(QrInspector.inspect("https://example.com/a?b=1"))
        assertFalse(QrInspector.inspect("tel:+923001234567").fields.isEmpty())
    }
}

class DeviceFormatTest {
    @Test fun bytes() {
        assertEquals("0 B", com.omnitools.app.tools.device.DeviceFormat.bytes(0))
        assertEquals("1.0 KB", com.omnitools.app.tools.device.DeviceFormat.bytes(1024))
        assertEquals("1.5 GB", com.omnitools.app.tools.device.DeviceFormat.bytes(1610612736L))
        assertEquals("unknown", com.omnitools.app.tools.device.DeviceFormat.bytes(-1))
    }
    @Test fun percentAndLabels() {
        val f = com.omnitools.app.tools.device.DeviceFormat
        assertEquals(50, f.percent(5, 10)); assertEquals(0, f.percent(1, 0)); assertEquals(100, f.percent(20, 10))
        assertEquals("Charging", f.batteryStatus(2)); assertEquals("USB", f.plugged(2)); assertEquals("AC charger", f.plugged(1))
        assertEquals("Not plugged in", f.plugged(0)); assertEquals("Accelerometer", f.sensorType(1))
    }
}
