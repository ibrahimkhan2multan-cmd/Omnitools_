package com.omnitools.app

import com.omnitools.app.data.Prefs
import com.omnitools.app.data.ToolRegistry
import com.omnitools.app.tools.password.PasswordGenerator
import com.omnitools.app.tools.text.TextTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class TextAndPasswordTest {

    @Test fun stats() {
        val s = TextTools.stats("Hello world. How are you?\n\nNew paragraph")
        assertEquals(7, s.words)
        assertEquals(3, s.sentences)
        assertEquals(2, s.paragraphs)
        assertEquals(0, TextTools.stats("").words)
    }

    @Test fun lineTools() {
        assertEquals("a\nb\nc", TextTools.removeDuplicateLines("a\nb\na\nc\nb"))
        assertEquals("apple\nBanana\ncherry", TextTools.sortLines("cherry\nBanana\napple"))
        assertEquals("1. x\n2. y", TextTools.numberLines("x\ny"))
        assertEquals("a b\nc", TextTools.removeExtraWhitespace("  a    b \n\t c  "))
        assertEquals("cba", TextTools.reverseText("abc"))
        assertEquals("Hello World", TextTools.titleCase("hELLO wORLD"))
    }

    @Test fun findReplace() {
        assertEquals("xbxb" to 2, TextTools.findReplace("abab", "a", "x", false))
        assertEquals("xbxb" to 2, TextTools.findReplace("AbaB".lowercase(), "A", "x", true))
        assertEquals("xbxb" to 2, TextTools.findReplace("AbAb", "a", "x", true))
        assertEquals("abab" to 0, TextTools.findReplace("abab", "z", "x", false))
        assertThrows(TextTools.TextToolException::class.java) { TextTools.findReplace("abc", "", "x", false) }
    }

    @Test fun base64AndUrl() {
        assertEquals("SGVsbG8sIOKCrA==", TextTools.base64Encode("Hello, €"))
        assertEquals("Hello, €", TextTools.base64Decode("SGVsbG8sIOKCrA=="))
        assertThrows(TextTools.TextToolException::class.java) { TextTools.base64Decode("***not base64***") }
        assertEquals("a+b%26c", TextTools.urlEncode("a b&c"))
        assertEquals("a b&c", TextTools.urlDecode("a+b%26c"))
    }

    @Test fun json() {
        assertEquals("{\"a\":1,\"b\":[1,2]}", TextTools.jsonMinify("{ \"a\": 1,\n \"b\": [1, 2] }"))
        // Two keys: the JVM org.json test library prints single-key objects on one line, Android does not.
        assertTrue(TextTools.jsonFormat("{\"a\":1,\"b\":2}").contains("\n"))
        assertThrows(TextTools.TextToolException::class.java) { TextTools.jsonFormat("{bad json") }
        assertThrows(TextTools.TextToolException::class.java) { TextTools.jsonFormat("") }
        assertThrows(TextTools.TextToolException::class.java) { TextTools.jsonFormat("hello") }
    }

    @Test fun passwordRespectsOptions() {
        val rnd = SecureRandom()
        repeat(200) {
            val o = PasswordGenerator.Options(length = 12, symbols = false, avoidAmbiguous = true)
            val p = PasswordGenerator.generate(o, rnd)
            assertEquals(12, p.length)
            assertTrue(p.any { it.isLowerCase() })
            assertTrue(p.any { it.isUpperCase() })
            assertTrue(p.any { it.isDigit() })
            assertTrue(p.none { it in "l1IO0" })
            assertTrue(p.all { it.isLetterOrDigit() })
        }
    }

    @Test fun passwordValidation() {
        assertThrows(IllegalArgumentException::class.java) {
            PasswordGenerator.generate(PasswordGenerator.Options(lower = false, upper = false, digits = false, symbols = false))
        }
        assertThrows(IllegalArgumentException::class.java) { PasswordGenerator.generate(PasswordGenerator.Options(length = 3)) }
        assertThrows(IllegalArgumentException::class.java) { PasswordGenerator.generate(PasswordGenerator.Options(length = 500)) }
    }

    @Test fun passwordsDiffer() {
        val o = PasswordGenerator.Options()
        assertTrue(PasswordGenerator.generate(o) != PasswordGenerator.generate(o))
        assertTrue(PasswordGenerator.entropyBits(o) > 80)
    }

    @Test fun recentsKeepOrderDedupeAndCap() {
        assertEquals(listOf("b", "a"), Prefs.addRecent(listOf("a", "b"), "b"))
        assertEquals(listOf("c", "a", "b"), Prefs.addRecent(listOf("a", "b"), "c"))
        val many = (1..20).map { "t$it" }
        val result = Prefs.addRecent(many, "new")
        assertEquals(Prefs.MAX_RECENTS, result.size)
        assertEquals("new", result.first())
        assertEquals(listOf("a", "b"), Prefs.decodeRecents(Prefs.encodeRecents(listOf("a", "b"))))
        assertTrue(Prefs.decodeRecents(null).isEmpty())
    }

    @Test fun searchFindsToolsByNameDescriptionCategoryAndKeyword() {
        assertTrue(ToolRegistry.search("qr").any { it.id == "qr_scan" })
        assertTrue(ToolRegistry.search("base64").any { it.id == "text_tools" })
        assertTrue(ToolRegistry.search("battery").any { it.id == "device_info" })
        assertTrue(ToolRegistry.search("security").any { it.id == "password_generator" })
        assertTrue(ToolRegistry.search("   ").isEmpty())
        assertTrue(ToolRegistry.search("zzzzzz").isEmpty())
    }

    @Test fun everyRegisteredToolHasUniqueIdAndText() {
        val ids = ToolRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertFalse(ToolRegistry.all.any { it.name.isBlank() || it.description.isBlank() })
    }
}

class TextJsonAndBase64Test {
    @Test fun jsonFormatKeepsNumbersExactly() {
        val out = com.omnitools.app.tools.text.TextTools.jsonMinify("{\"id\": 12345678901234567890, \"x\": 1.0, \"y\": 0.10, \"z\": 1e5}")
        assertEquals("{\"id\":12345678901234567890,\"x\":1.0,\"y\":0.10,\"z\":1e5}", out)
        assertThrows(com.omnitools.app.tools.text.TextTools.TextToolException::class.java) { com.omnitools.app.tools.text.TextTools.jsonFormat("{'a':1}") }
        assertThrows(com.omnitools.app.tools.text.TextTools.TextToolException::class.java) { com.omnitools.app.tools.text.TextTools.jsonFormat("{\"a\":1,}") }
    }

    @Test fun base64AcceptsUrlSafeUnpaddedAndSpaces() {
        val t = com.omnitools.app.tools.text.TextTools
        assertEquals("hello?>", t.base64Decode("aGVsbG8/Pg=="))
        assertEquals("hello?>", t.base64Decode("aGVsbG8_Pg"))
        assertEquals("hello?>", t.base64Decode(" aGVs bG8/\nPg== "))
        assertThrows(com.omnitools.app.tools.text.TextTools.TextToolException::class.java) { t.base64Decode("!!!") }
    }
}
