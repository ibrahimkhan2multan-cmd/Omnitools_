package com.omnitools.app

import com.omnitools.app.tools.editor.SyntaxHighlighter
import com.omnitools.app.tools.editor.SyntaxHighlighter.Kind
import com.omnitools.app.tools.editor.SyntaxHighlighter.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    private fun spans(text: String, lang: Language) =
        SyntaxHighlighter.highlight(text, lang).map { Triple(text.substring(it.start, it.end), it.kind, it.start) }

    @Test fun languageFromFileName() {
        assertEquals(Language.KOTLIN, SyntaxHighlighter.languageFor("Main.kt"))
        assertEquals(Language.HTML, SyntaxHighlighter.languageFor("index.HTML"))
        assertEquals(Language.PLAIN, SyntaxHighlighter.languageFor("notes"))
        assertEquals(Language.PLAIN, SyntaxHighlighter.languageFor(null))
        assertEquals(Language.JSON, SyntaxHighlighter.languageFor("a.b.json"))
    }

    @Test fun kotlinKeywordsStringsNumbersComments() {
        val s = spans("val x = \"hi\" + 42 // note", Language.KOTLIN)
        assertEquals(listOf("val", "\"hi\"", "42", "// note"), s.map { it.first })
        assertEquals(listOf(Kind.KEYWORD, Kind.STRING, Kind.NUMBER, Kind.COMMENT), s.map { it.second })
    }

    @Test fun keywordsInsideWordsAreNotHighlighted() {
        assertTrue(spans("value interval x1", Language.KOTLIN).isEmpty())
    }

    @Test fun stringsHandleEscapesAndStopAtLineEnd() {
        val s = spans("a = \"q\\\"uote\"\nb = 'open\nc", Language.PYTHON)
        assertEquals("\"q\\\"uote\"", s[0].first)
        assertEquals("'open", s[1].first)
    }

    @Test fun blockCommentSpansLinesAndUnclosedRunsToEnd() {
        val text = "a /* one\ntwo */ b /* never closed"
        val s = spans(text, Language.JAVA)
        assertEquals("/* one\ntwo */", s[0].first)
        assertEquals("/* never closed", s[1].first)
    }

    @Test fun commentMarkerInsideStringIsPartOfTheString() {
        val s = spans("x = \"http://a\" // real", Language.JAVASCRIPT)
        assertEquals(listOf("\"http://a\"", "// real"), s.map { it.first })
    }

    @Test fun jsonValues() {
        val s = spans("{\"a\": [1, 2.5, true, null]}", Language.JSON)
        assertEquals(listOf("\"a\"", "1", "2.5", "true", "null"), s.map { it.first })
    }

    @Test fun htmlTagsAttributesAndComments() {
        val s = spans("<!-- c --><a href=\"x\">t</a>", Language.HTML)
        assertEquals(listOf("<!-- c -->", "<a", "\"x\"", ">", "</a", ">"), s.map { it.first })
        assertEquals(Kind.COMMENT, s[0].second)
        assertEquals(Kind.STRING, s[2].second)
    }

    @Test fun neverOverlapsAndStaysInBounds() {
        val samples = listOf("", "\"", "/*", "<", "<a", "<a \"", "'", "`multi\nline`", "9" + "x".repeat(50), "\\")
        for (lang in Language.values()) for (t in samples) {
            var last = 0
            for (sp in SyntaxHighlighter.highlight(t, lang)) {
                assertTrue("$lang '$t' $sp", sp.start >= last && sp.end <= t.length && sp.start < sp.end)
                last = sp.end
            }
        }
    }
}

class UndoAndHtmlTest {
    @org.junit.Test fun undoRedoWalksTheHistory() {
        val h = com.omnitools.app.tools.editor.UndoHistory("a")
        org.junit.Assert.assertFalse(h.canUndo)
        org.junit.Assert.assertTrue(h.record("ab"))
        org.junit.Assert.assertFalse(h.record("ab"))
        h.record("abc")
        org.junit.Assert.assertEquals("ab", h.undo())
        org.junit.Assert.assertEquals("a", h.undo())
        org.junit.Assert.assertNull(h.undo())
        org.junit.Assert.assertEquals("ab", h.redo())
        // typing after an undo throws away the old redo steps
        h.record("abX")
        org.junit.Assert.assertFalse(h.canRedo)
        org.junit.Assert.assertEquals("ab", h.undo())
    }

    @org.junit.Test fun undoHistoryIsCapped() {
        val h = com.omnitools.app.tools.editor.UndoHistory("0", limit = 5)
        for (i in 1..20) h.record("$i")
        var steps = 0
        while (h.undo() != null) steps++
        org.junit.Assert.assertEquals(4, steps)
        h.reset("new")
        org.junit.Assert.assertEquals("new", h.current)
        org.junit.Assert.assertFalse(h.canUndo)
    }

    @org.junit.Test fun htmlFragmentIsWrappedWithPolicyFirst() {
        val doc = com.omnitools.app.tools.html.HtmlDocument.build("<p>hi</p>", "p{color:red}", "console.log(1)")
        org.junit.Assert.assertTrue(doc.indexOf("Content-Security-Policy") < doc.indexOf("<style>"))
        org.junit.Assert.assertTrue(doc.indexOf("Content-Security-Policy") < doc.indexOf("<p>hi</p>"))
        org.junit.Assert.assertTrue(doc.indexOf("<p>hi</p>") < doc.indexOf("<script>"))
        org.junit.Assert.assertTrue(doc.contains("connect-src 'none'"))
    }

    @org.junit.Test fun fullDocumentGetsPolicyAtTopOfHead() {
        val src = "<!DOCTYPE html><HTML><HEAD><title>t</title></HEAD><BODY><b>x</b></BODY></HTML>"
        val doc = com.omnitools.app.tools.html.HtmlDocument.build(src, "b{}", "var a=1")
        val head = doc.indexOf("<HEAD>") + "<HEAD>".length
        org.junit.Assert.assertEquals(head, doc.indexOf("<meta http-equiv=\"Content-Security-Policy\""))
        org.junit.Assert.assertTrue(doc.indexOf("<style>") in doc.indexOf("<title>") until doc.indexOf("</HEAD>"))
        org.junit.Assert.assertTrue(doc.indexOf("<script>") < doc.indexOf("</BODY>"))
        org.junit.Assert.assertTrue(doc.indexOf("<script>") > doc.indexOf("<b>x</b>"))
    }

    @org.junit.Test fun htmlWithoutHeadStillGetsPolicy() {
        val doc = com.omnitools.app.tools.html.HtmlDocument.build("<html><body>x</body></html>", "", "")
        org.junit.Assert.assertTrue(doc.indexOf("Content-Security-Policy") in 0 until doc.indexOf("<body>"))
    }
}

class EditorTextTest {
    @org.junit.Test fun utf8DecodingIsStrict() {
        val ok = com.omnitools.app.tools.editor.EditorText.decodeUtf8("héllo ✓".toByteArray())
        org.junit.Assert.assertEquals("héllo ✓", ok)
        org.junit.Assert.assertEquals("x", com.omnitools.app.tools.editor.EditorText.decodeUtf8(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(), 'x'.code.toByte())))
        try {
            com.omnitools.app.tools.editor.EditorText.decodeUtf8(byteArrayOf(0xC3.toByte(), 0x28))
            org.junit.Assert.fail("accepted bad UTF-8")
        } catch (e: com.omnitools.app.tools.editor.EditorText.EditorException) { }
    }

    @org.junit.Test fun newlinesRoundTrip() {
        val (text, crlf) = com.omnitools.app.tools.editor.EditorText.normalizeNewlines("a\r\nb\r\n")
        org.junit.Assert.assertEquals("a\nb\n", text)
        org.junit.Assert.assertTrue(crlf)
        org.junit.Assert.assertEquals("a\r\nb\r\n", String(com.omnitools.app.tools.editor.EditorText.encode(text, true)))
        org.junit.Assert.assertEquals("a\nb\n", String(com.omnitools.app.tools.editor.EditorText.encode(text, false)))
        org.junit.Assert.assertFalse(com.omnitools.app.tools.editor.EditorText.normalizeNewlines("a\nb").second)
    }

    @org.junit.Test fun sizeLimitIsEnforced() {
        val big = ByteArray(com.omnitools.app.tools.editor.EditorText.MAX_BYTES + 10)
        try {
            com.omnitools.app.tools.editor.EditorText.readLimited(big.inputStream())
            org.junit.Assert.fail("limit not enforced")
        } catch (e: com.omnitools.app.tools.editor.EditorText.EditorException) { }
        org.junit.Assert.assertEquals(100, com.omnitools.app.tools.editor.EditorText.readLimited(ByteArray(100).inputStream()).size)
    }

    @org.junit.Test fun lineAndColumn() {
        val t = "ab\ncde\n\nf"
        org.junit.Assert.assertEquals(1 to 1, com.omnitools.app.tools.editor.EditorText.lineAndColumn(t, 0))
        org.junit.Assert.assertEquals(1 to 3, com.omnitools.app.tools.editor.EditorText.lineAndColumn(t, 2))
        org.junit.Assert.assertEquals(2 to 1, com.omnitools.app.tools.editor.EditorText.lineAndColumn(t, 3))
        org.junit.Assert.assertEquals(2 to 4, com.omnitools.app.tools.editor.EditorText.lineAndColumn(t, 6))
        org.junit.Assert.assertEquals(3 to 1, com.omnitools.app.tools.editor.EditorText.lineAndColumn(t, 7))
        org.junit.Assert.assertEquals(4 to 2, com.omnitools.app.tools.editor.EditorText.lineAndColumn(t, 99))
    }
}
