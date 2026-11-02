package com.omnitools.app

import com.omnitools.app.tools.qr.QrEncoder
import com.omnitools.app.tools.qr.QrPayloads
import com.omnitools.app.tools.qr.UrlSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QrTest {

    @Test fun wifiPayload() {
        assertEquals("WIFI:T:WPA;S:Home;P:secret123;;", QrPayloads.wifi("Home", "secret123", QrPayloads.WifiSecurity.WPA, false))
        assertEquals("WIFI:T:WPA;S:Cafe;P:pw;H:true;;", QrPayloads.wifi("Cafe", "pw", QrPayloads.WifiSecurity.WPA, true))
        assertEquals("WIFI:T:nopass;S:Open;;", QrPayloads.wifi("Open", "ignored", QrPayloads.WifiSecurity.NONE, false))
    }

    @Test fun wifiEscapesSpecialCharacters() {
        assertEquals("WIFI:T:WPA;S:a\\;b\\:c;P:p\\\\w\\\"x\\,y;;", QrPayloads.wifi("a;b:c", "p\\w\"x,y", QrPayloads.WifiSecurity.WPA, false))
    }

    @Test fun wifiValidation() {
        assertThrows(IllegalArgumentException::class.java) { QrPayloads.wifi(" ", "x", QrPayloads.WifiSecurity.WPA, false) }
        assertThrows(IllegalArgumentException::class.java) { QrPayloads.wifi("Net", "", QrPayloads.WifiSecurity.WPA, false) }
    }

    @Test fun contactPayload() {
        val v = QrPayloads.contact("Ada Lovelace", "+123456", "ada@example.com", "Analytical, Inc")
        assertTrue(v.startsWith("BEGIN:VCARD\nVERSION:3.0\n"))
        assertTrue(v.contains("FN:Ada Lovelace\n"))
        assertTrue(v.contains("TEL:+123456\n"))
        assertTrue(v.contains("EMAIL:ada@example.com\n"))
        assertTrue(v.contains("ORG:Analytical\\, Inc\n"))
        assertTrue(v.endsWith("END:VCARD"))
        assertThrows(IllegalArgumentException::class.java) { QrPayloads.contact("", "", "", "x") }
    }

    @Test fun encoderProducesSquareMatrix() {
        val m = QrEncoder.matrix("https://example.com", size = 256)
        assertEquals(256, m.width)
        assertEquals(256, m.height)
        // A QR code always has dark modules: matrix must not be blank.
        var dark = 0
        for (y in 0 until m.height) for (x in 0 until m.width) if (m.get(x, y)) dark++
        assertTrue(dark > 0)
    }

    @Test fun encoderValidatesInput() {
        assertThrows(QrEncoder.QrException::class.java) { QrEncoder.matrix("") }
        assertThrows(QrEncoder.QrException::class.java) { QrEncoder.matrix("x".repeat(QrEncoder.MAX_CHARS + 1)) }
    }

    @Test fun plainHttpsLinkHasNoWarnings() {
        val a = UrlSafety.analyze("https://example.com/path?q=1")
        assertTrue(a.isUri); assertTrue(a.canOpen)
        assertEquals("example.com", a.host)
        assertTrue(a.warnings.isEmpty())
    }

    @Test fun riskyLinksAreFlagged() {
        assertTrue(UrlSafety.analyze("http://example.com").warnings.any { it.contains("Not encrypted") })
        assertTrue(UrlSafety.analyze("https://192.168.0.1/login").warnings.any { it.contains("IP address") })
        assertTrue(UrlSafety.analyze("https://bit.ly/abc").warnings.any { it.contains("shortener") })
        assertTrue(UrlSafety.analyze("https://user@evil.com").warnings.any { it.contains("username") })
        assertTrue(UrlSafety.analyze("https://xn--pple-43d.com").warnings.any { it.contains("punycode") })
        assertTrue(UrlSafety.analyze("https://example.com:8443").warnings.any { it.contains("port") })
    }

    @Test fun nonWebSchemesCannotBeOpened() {
        for (s in listOf("javascript:alert(1)", "intent://x#Intent;end", "file:///sdcard/a", "tel:123", "sms:123")) {
            val a = UrlSafety.analyze(s)
            assertFalse("$s must not be openable", a.canOpen)
        }
    }

    @Test fun plainTextIsNotAUri() {
        assertFalse(UrlSafety.analyze("hello world").isUri)
        assertFalse(UrlSafety.analyze("").isUri)
        assertNull(UrlSafety.analyze("just-text").scheme)
    }
}
