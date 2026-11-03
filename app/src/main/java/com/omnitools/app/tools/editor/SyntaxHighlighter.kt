package com.omnitools.app.tools.editor

/** Very small syntax colouring: comments, strings, numbers, keywords and (for HTML) tags. Pure Kotlin, unit tested. */
object SyntaxHighlighter {

    enum class Kind { COMMENT, STRING, NUMBER, KEYWORD, TAG }

    data class Span(val start: Int, val end: Int, val kind: Kind)

    enum class Language(val label: String, val extensions: List<String>) {
        PLAIN("Plain text", listOf("txt", "text", "log", "md", "csv")),
        KOTLIN("Kotlin", listOf("kt", "kts")),
        JAVA("Java", listOf("java")),
        JAVASCRIPT("JavaScript", listOf("js", "mjs", "ts", "jsx", "tsx")),
        PYTHON("Python", listOf("py")),
        C_LIKE("C / C++ / C#", listOf("c", "h", "cpp", "hpp", "cc", "cs", "go", "rs", "swift", "dart", "php")),
        HTML("HTML / XML", listOf("html", "htm", "xml", "svg")),
        CSS("CSS", listOf("css", "scss")),
        JSON("JSON", listOf("json")),
        SHELL("Shell", listOf("sh", "bash", "yml", "yaml", "toml", "ini", "properties", "gradle")),
    }

    fun languageFor(fileName: String?): Language {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return Language.values().firstOrNull { ext in it.extensions } ?: Language.PLAIN
    }

    private class Rules(
        val line: List<String> = emptyList(),
        val blockStart: String? = null,
        val blockEnd: String? = null,
        val quotes: String = "\"'",
        val keywords: Set<String> = emptySet(),
    )

    private fun words(s: String) = s.split(" ").toSet()

    private val rules: Map<Language, Rules> = mapOf(
        Language.KOTLIN to Rules(listOf("//"), "/*", "*/", "\"'", words(
            "fun val var class object interface if else when for while do return break continue in is as null true false " +
                "this super package import private public protected internal override open abstract data sealed enum companion " +
                "suspend try catch finally throw const lateinit by init typealias inline")),
        Language.JAVA to Rules(listOf("//"), "/*", "*/", "\"'", words(
            "class interface enum extends implements public private protected static final void int long double float boolean " +
                "char byte short if else for while do switch case default return break continue new this super null true false " +
                "try catch finally throw throws import package abstract synchronized instanceof var")),
        Language.JAVASCRIPT to Rules(listOf("//"), "/*", "*/", "\"'`", words(
            "function const let var class extends return if else for while do switch case default break continue new this " +
                "null undefined true false try catch finally throw typeof instanceof import export from async await of in yield " +
                "interface type enum implements public private static")),
        Language.PYTHON to Rules(listOf("#"), null, null, "\"'", words(
            "def class return if elif else for while in not and or is None True False import from as with try except finally " +
                "raise pass break continue lambda yield global nonlocal async await del assert")),
        Language.C_LIKE to Rules(listOf("//"), "/*", "*/", "\"'", words(
            "int long double float char void bool boolean string if else for while do switch case default return break " +
                "continue struct class enum union typedef static const new delete null nullptr true false this namespace using " +
                "public private protected virtual override fn let mut pub impl use mod match func package import var " +
                "foreach in try catch finally throw echo")),
        Language.CSS to Rules(emptyList(), "/*", "*/", "\"'", emptySet()),
        Language.JSON to Rules(emptyList(), null, null, "\"", words("true false null")),
        Language.SHELL to Rules(listOf("#"), null, null, "\"'", words(
            "if then else elif fi for while do done case esac function in echo export return exit true false null")),
    )

    /** Spans in text order, never overlapping. Plain text has none. */
    fun highlight(text: String, language: Language): List<Span> = when (language) {
        Language.PLAIN -> emptyList()
        Language.HTML -> html(text)
        else -> generic(text, rules.getValue(language), language == Language.CSS)
    }

    private fun generic(text: String, r: Rules, css: Boolean): List<Span> {
        val spans = ArrayList<Span>()
        val n = text.length
        var i = 0
        while (i < n) {
            val c = text[i]
            // block comment
            if (r.blockStart != null && text.startsWith(r.blockStart, i)) {
                val close = text.indexOf(r.blockEnd!!, i + r.blockStart.length)
                val end = if (close < 0) n else close + r.blockEnd.length
                spans += Span(i, end, Kind.COMMENT)
                i = end
                continue
            }
            // line comment
            val lc = r.line.firstOrNull { text.startsWith(it, i) }
            if (lc != null) {
                var end = text.indexOf('\n', i)
                if (end < 0) end = n
                spans += Span(i, end, Kind.COMMENT)
                i = end
                continue
            }
            // string (ends at the matching quote or the end of the line; backslash escapes the next char)
            if (c in r.quotes) {
                var j = i + 1
                val multiline = c == '`'
                while (j < n) {
                    val d = text[j]
                    if (d == '\\') { j += 2; continue }
                    if (d == c) { j++; break }
                    if (d == '\n' && !multiline) break
                    j++
                }
                val end = j.coerceAtMost(n)
                spans += Span(i, end, Kind.STRING)
                i = end
                continue
            }
            // number (not part of a word)
            if (c.isDigit() && (i == 0 || !(text[i - 1].isLetterOrDigit() || text[i - 1] == '_'))) {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '.' || text[j] == '_')) j++
                spans += Span(i, j, Kind.NUMBER)
                i = j
                continue
            }
            // word
            if (c.isLetter() || c == '_') {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '_')) j++
                if (!css && text.substring(i, j) in r.keywords) spans += Span(i, j, Kind.KEYWORD)
                i = j
                continue
            }
            i++
        }
        return spans
    }

    private fun html(text: String): List<Span> {
        val spans = ArrayList<Span>()
        val n = text.length
        var i = 0
        while (i < n) {
            if (text.startsWith("<!--", i)) {
                val close = text.indexOf("-->", i + 4)
                val end = if (close < 0) n else close + 3
                spans += Span(i, end, Kind.COMMENT)
                i = end
            } else if (text[i] == '<' && i + 1 < n && (text[i + 1].isLetter() || text[i + 1] == '/' || text[i + 1] == '!' || text[i + 1] == '?')) {
                // tag name
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '/' || text[j] == '!' || text[j] == '?' || text[j] == '-' || text[j] == ':')) j++
                spans += Span(i, j, Kind.TAG)
                // attributes up to the closing >
                while (j < n && text[j] != '>') {
                    val c = text[j]
                    if (c == '"' || c == '\'') {
                        var k = j + 1
                        while (k < n && text[k] != c) k++
                        val end = (k + 1).coerceAtMost(n)
                        spans += Span(j, end, Kind.STRING)
                        j = end
                    } else {
                        j++
                    }
                }
                if (j < n) {
                    spans += Span(j, j + 1, Kind.TAG)
                    j++
                }
                i = j
            } else {
                i++
            }
        }
        return spans
    }
}
