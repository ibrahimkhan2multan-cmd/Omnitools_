package com.omnitools.app

import com.omnitools.app.tools.html.GameKeys
import com.omnitools.app.tools.html.MarkdownLite
import com.omnitools.app.tools.html.RunnerDocs
import com.omnitools.app.tools.html.RunnerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunnerModesTest {
    @Test fun markdownBasics() {
        val html = MarkdownLite.toHtml("# Title\n\nHello **bold** and *it* and `c`\n\n- a\n- b\n\n1. x\n2. y\n\n> q\n\n---")
        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>it</em>"))
        assertTrue(html.contains("<code>c</code>"))
        assertTrue(html.contains("<ul>\n<li>a</li>\n<li>b</li>\n</ul>"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<blockquote>q</blockquote>"))
        assertTrue(html.contains("<hr>"))
    }

    @Test fun markdownEscapesHtmlAndKeepsCodeLiteral() {
        val html = MarkdownLite.toHtml("<script>alert(1)</script>\n\n```\n<b>x</b> **no**\n```")
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("<pre><code>&lt;b&gt;x&lt;/b&gt; **no**\n</code></pre>"))
        assertEquals("<code>**a**</code>", MarkdownLite.inline("`**a**`"))
    }

    @Test fun jsLiteralIsSafeInsideScript() {
        assertEquals("\"a\\\\b\\\"c\\nd\\u003c/script>\"", RunnerDocs.jsLiteral("a\\b\"c\nd</script>"))
    }

    @Test fun everyModeProducesSandboxedDocument() {
        for (m in RunnerMode.values()) {
            val doc = RunnerDocs.build(m, "<p>x</p>", "", "", m.example)
            assertTrue(m.name, doc.contains("Content-Security-Policy"))
        }
    }

    @Test fun jsModeCannotBreakOutOfScript() {
        val doc = RunnerDocs.build(RunnerMode.JS, "", "", "", "console.log('</script><b>')")
        assertEquals(1, Regex("</script>").findAll(doc).count())
    }

    @Test fun jsonModeEmbedsTextSafely() {
        val doc = RunnerDocs.build(RunnerMode.JSON, "", "", "", "{\"a\":\"</script>\"}")
        assertEquals(1, Regex("</script>").findAll(doc).count())
        assertTrue(doc.contains("JSON.parse"))
    }

    @Test fun gameKeyScriptMentionsKey() {
        val s = GameKeys.script(" ", "Space", 32)
        assertTrue(s.contains("keydown") && s.contains("32") && s.contains("Space"))
    }
}
