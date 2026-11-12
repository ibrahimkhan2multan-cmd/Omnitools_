package com.omnitools.app

import com.omnitools.app.tools.html.GameKeys
import com.omnitools.app.tools.html.MarkdownLite
import com.omnitools.app.tools.html.RunnerDocs
import com.omnitools.app.tools.html.RunnerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

class RunnerLanguagesTest {
    private val required = com.omnitools.app.tools.html.RuntimeAssets.files

    @Test fun languageModesFillTheirTemplatesSafely() {
        val evil = "print('</script><b>')  /*CODE*/ /*STDIN*/"
        for (m in listOf(RunnerMode.PYTHON, RunnerMode.TYPESCRIPT, RunnerMode.SQL, RunnerMode.LUA)) {
            val doc = RunnerDocs.build(m, "", "", "", evil, "a\nb")
            assertFalse(m.name, doc.contains("This language is missing"))
            assertEquals(m.name, 1, Regex("</script>").findAll(doc).count() - Regex("<script src=").findAll(doc).count() )
            assertTrue(m.name, doc.contains("default-src 'none'"))
            assertTrue(m.name, doc.contains(RunnerDocs.jsLiteral(evil)))
            assertFalse(m.name, doc.contains("<script>/*CODE*/"))
        }
    }

    @Test fun onlyKnownRuntimeFilesAreReferenced() {
        for (name in listOf("python.html", "sql.html", "lua.html", "typescript.html")) {
            val text = javaClass.classLoader!!.getResourceAsStream("runner/$name")!!.use { it.readBytes().toString(Charsets.UTF_8) }
            Regex("https://omnitools\\.local/rt/([A-Za-z0-9._-]+)").findAll(text).forEach { assertTrue("$name -> ${it.groupValues[1]}", it.groupValues[1] in required) }
            assertFalse(name, Regex("https?://(?!omnitools\\.local)").containsMatchIn(text.replace("http://www.w3.org", "")))
            assertTrue(name, text.contains("/*CODE*/"))
        }
    }

    @Test fun runtimeUrlsAreAllowListed() {
        val a = com.omnitools.app.tools.html.RuntimeAssets
        assertEquals("pyodide.js", a.nameOf("https://omnitools.local/rt/pyodide.js"))
        assertEquals("sql-wasm.wasm", a.nameOf("https://omnitools.local/rt/sql-wasm.wasm?x=1"))
        for (bad in listOf("https://omnitools.local/rt/../secret", "https://omnitools.local/rt/AndroidManifest.xml", "https://evil.com/rt/pyodide.js", "https://omnitools.local/rt/", "https://omnitools.local/rt/%2e%2e/x")) {
            assertNull(bad, a.nameOf(bad))
        }
        assertEquals("application/wasm", a.mime("pyodide.asm.wasm")); assertEquals("text/javascript", a.mime("pyodide.asm.mjs"))
    }

    @Test fun languageModesHaveExamplesAndRuntimeFlag() {
        for (m in listOf(RunnerMode.PYTHON, RunnerMode.TYPESCRIPT, RunnerMode.SQL, RunnerMode.LUA)) {
            assertTrue(m.name, m.runtime); assertTrue(m.name, m.example.isNotBlank())
        }
        assertFalse(RunnerMode.JS.runtime); assertFalse(RunnerMode.WEB.runtime)
    }

    @Test fun changelogIsCopyable() {
        val t = com.omnitools.app.tools.html.RunnerChangelog.asText()
        assertTrue(t.contains("Python") && t.contains("TypeScript") && t.contains("SQL") && t.contains("Lua"))
        assertTrue(t.contains("Not supported"))
        assertTrue(t.contains("Gaming & Modding") && t.contains("28 new tools") && t.contains("Subnet Calculator"))
        val log = com.omnitools.app.tools.html.RunnerChangelog.entries
        assertEquals("Build 59", log.first().version)
        assertTrue(log.all { it.items.isNotEmpty() && it.title.isNotBlank() })
    }
}

class SvgPreviewTest {
    private fun template() = javaClass.classLoader!!.getResourceAsStream("runner/svg.html")!!.use { it.readBytes().toString(Charsets.UTF_8) }

    @Test fun templateUsesTheSameSecurityPolicyAsEveryOtherPage() {
        val t = template()
        assertTrue(t.contains("content=\"${com.omnitools.app.tools.html.HtmlDocument.POLICY}\""))
        assertEquals(1, Regex("<!--SVG-->").findAll(t).count())
    }

    @Test fun userSvgIsInsertedOnceAndNeverReprocessed() {
        val code = "<svg viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\"/><!--SVG--></svg>"
        val doc = RunnerDocs.build(RunnerMode.SVG, "", "", "", code)
        assertEquals(1, Regex(Regex.escape(code)).findAll(doc).count())
        assertEquals(1, Regex("<!--SVG-->").findAll(doc).count())
        assertTrue(doc.contains("Content-Security-Policy"))
    }

    @Test fun pageHasFitAndZoomControls() {
        val doc = RunnerDocs.build(RunnerMode.SVG, "", "", "", RunnerMode.SVG.example)
        for (id in listOf("fit", "one", "in", "out")) assertTrue(id, doc.contains("id=\"$id\""))
        assertTrue(doc.contains("viewBox"))
    }
}
