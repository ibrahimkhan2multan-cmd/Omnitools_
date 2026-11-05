package com.omnitools.app.tools.html

/** A small, safe Markdown to HTML converter (headings, lists, quotes, code, bold, italic, links as text). Pure Kotlin. */
object MarkdownLite {
    fun escape(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private val code = Regex("`([^`]+)`")
    private val bold = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__")
    private val italic = Regex("(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])|(?<![_\\w])_(?!\\s)(.+?)(?<!\\s)_(?![_\\w])")
    private val link = Regex("\\[([^\\]]+)]\\(([^)\\s]+)\\)")

    /** Inline formatting on already-escaped text. Code spans are protected from the other rules. */
    fun inline(raw: String): String {
        val spans = ArrayList<String>()
        var t = escape(raw).replace(code) { m -> spans.add("<code>${m.groupValues[1]}</code>"); "\u0000${spans.size - 1}\u0000" }
        t = t.replace(link) { m -> "<u>${m.groupValues[1]}</u> (${m.groupValues[2]})" }
        t = t.replace(bold) { m -> "<strong>${m.groupValues[1].ifEmpty { m.groupValues[2] }}</strong>" }
        t = t.replace(italic) { m -> "<em>${m.groupValues[1].ifEmpty { m.groupValues[2] }}</em>" }
        return t.replace(Regex("\u0000(\\d+)\u0000")) { spans[it.groupValues[1].toInt()] }
    }

    fun toHtml(md: String): String {
        val out = StringBuilder()
        val lines = md.replace("\r\n", "\n").split('\n')
        var i = 0
        var list: String? = null
        fun closeList() { list?.let { out.append("</$it>\n") }; list = null }
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                trimmed.startsWith("```") -> {
                    closeList()
                    val body = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) { body.append(escape(lines[i])).append('\n'); i++ }
                    out.append("<pre><code>").append(body).append("</code></pre>\n")
                }
                trimmed.isEmpty() -> closeList()
                Regex("^#{1,6} .*").matches(trimmed) -> {
                    closeList()
                    val level = trimmed.takeWhile { it == '#' }.length
                    out.append("<h$level>").append(inline(trimmed.substring(level + 1))).append("</h$level>\n")
                }
                Regex("^([-*_])(\\s*\\1){2,}$").matches(trimmed) -> { closeList(); out.append("<hr>\n") }
                trimmed.startsWith(">") -> {
                    closeList()
                    out.append("<blockquote>").append(inline(trimmed.removePrefix(">").trim())).append("</blockquote>\n")
                }
                Regex("^[-*+] .*").matches(trimmed) -> {
                    if (list != "ul") { closeList(); out.append("<ul>\n"); list = "ul" }
                    out.append("<li>").append(inline(trimmed.substring(2))).append("</li>\n")
                }
                Regex("^\\d+[.)] .*").matches(trimmed) -> {
                    if (list != "ol") { closeList(); out.append("<ol>\n"); list = "ol" }
                    out.append("<li>").append(inline(trimmed.replaceFirst(Regex("^\\d+[.)] "), ""))).append("</li>\n")
                }
                else -> {
                    closeList()
                    out.append("<p>").append(inline(trimmed)).append("</p>\n")
                }
            }
            i++
        }
        closeList()
        return out.toString()
    }
}

/** What kind of code is being run. Every mode ends up as a page in the same locked-down sandbox. */
enum class RunnerMode(val label: String) {
    WEB("HTML+CSS+JS"), JS("JavaScript"), MARKDOWN("Markdown"), SVG("SVG"), JSON("JSON");

    val example: String get() = when (this) {
        WEB -> ""
        JS -> "const nums = [1, 2, 3, 4, 5];\nconst squares = nums.map(n => n * n);\nconsole.log('squares:', squares);\nconsole.log('sum:', squares.reduce((a, b) => a + b, 0));"
        MARKDOWN -> "# Hello\n\nSome **bold**, some *italic* and `code`.\n\n- first\n- second\n\n> A quote\n\n```\nfun main() {}\n```"
        SVG -> "<svg viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">\n  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"#4f8cff\"/>\n  <rect x=\"30\" y=\"30\" width=\"40\" height=\"40\" fill=\"#fff\" opacity=\"0.8\"/>\n</svg>"
        JSON -> "{\"name\":\"OmniTools\",\"tools\":[1,2,3],\"offline\":true}"
    }
}

object RunnerDocs {
    private const val DARK = "body{font-family:sans-serif;margin:0;padding:16px;background:#0e1220;color:#e8ebf7;line-height:1.5}" +
        "pre{white-space:pre-wrap;word-break:break-word;font-family:monospace;font-size:14px}code{background:#1f2540;padding:2px 5px;border-radius:4px}" +
        "pre code{display:block;padding:10px}blockquote{border-left:3px solid #4f8cff;margin:8px 0;padding:2px 12px;color:#b4bad4}hr{border:0;border-top:1px solid #3a4163}"

    /** A JavaScript string literal that is safe to place inside a <script>. */
    fun jsLiteral(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '<' -> sb.append("\\u003c")
            '\u2028' -> sb.append("\\u2028")
            '\u2029' -> sb.append("\\u2029")
            else -> sb.append(c)
        }
        return sb.append('"').toString()
    }

    private const val MIRROR = "(function(){var o=document.getElementById('out');['log','info','warn','error'].forEach(function(k){var f=console[k];" +
        "console[k]=function(){var s=Array.prototype.map.call(arguments,function(a){try{return typeof a==='string'?a:JSON.stringify(a)}catch(e){return String(a)}}).join(' ');" +
        "o.textContent+=(k==='log'?'':k+': ')+s+'\\n';f.apply(console,arguments)}});" +
        "window.addEventListener('error',function(e){o.textContent+='error: '+e.message+'\\n'})})();\n"

    fun build(mode: RunnerMode, html: String, css: String, js: String, code: String): String = when (mode) {
        RunnerMode.WEB -> HtmlDocument.build(html, css, js)
        RunnerMode.JS -> HtmlDocument.build("<pre id=\"out\"></pre>", DARK, MIRROR + code.replace("</script", "<\\/script"))
        RunnerMode.MARKDOWN -> HtmlDocument.build(MarkdownLite.toHtml(code), DARK, "")
        RunnerMode.SVG -> HtmlDocument.build(code, "body{margin:0;padding:16px;background:#fff;text-align:center}svg{max-width:100%;height:auto}", "")
        RunnerMode.JSON -> HtmlDocument.build(
            "<pre id=\"out\"></pre>", DARK,
            "var t=${jsLiteral(code)};try{var v=JSON.parse(t);document.getElementById('out').textContent=JSON.stringify(v,null,2);" +
                "console.log('Valid JSON');}catch(e){document.getElementById('out').textContent='Invalid JSON: '+e.message;console.error('Invalid JSON: '+e.message);}",
        )
    }
}

/** Key presses sent to a running page, for games written for a keyboard. */
object GameKeys {
    fun script(key: String, code: String, keyCode: Int): String =
        "(function(){function f(t){var e=new KeyboardEvent(t,{key:'$key',code:'$code',bubbles:true,cancelable:true});" +
            "try{Object.defineProperty(e,'keyCode',{get:function(){return $keyCode}});Object.defineProperty(e,'which',{get:function(){return $keyCode}})}catch(x){}" +
            "[document.activeElement,document,window].forEach(function(n){if(n&&n.dispatchEvent)n.dispatchEvent(e)})}" +
            "f('keydown');setTimeout(function(){f('keyup')},60)})();"
}
