package com.omnitools.app.tools.advanced

import java.time.Instant
import java.util.Base64

/** A strict JSON parser and printer (no comments, no single quotes, no trailing commas). Key order is preserved. */
object Json {
    sealed class Node {
        data class Obj(val entries: List<Pair<String, Node>>) : Node()
        data class Arr(val items: List<Node>) : Node()
        data class Str(val value: String) : Node()
        data class Num(val text: String) : Node()
        data class Bool(val value: Boolean) : Node()
        object Null : Node()
    }

    class JsonException(message: String) : Exception(message)

    const val MAX_DEPTH = 200
    const val MAX_LENGTH = 2_000_000

    fun parse(text: String): Node {
        if (text.length > MAX_LENGTH) throw JsonException("The text is too long (limit $MAX_LENGTH characters).")
        val p = P(text)
        p.ws()
        if (p.end()) throw JsonException("Enter some JSON.")
        val v = p.value(0)
        p.ws()
        if (!p.end()) throw p.fail("Unexpected text after the JSON value")
        return v
    }

    private val NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

    private class P(val s: String) {
        var i = 0
        fun end() = i >= s.length
        fun ws() { while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++ }
        fun fail(msg: String): JsonException {
            var line = 1; var col = 1
            for (k in 0 until minOf(i, s.length)) { if (s[k] == '\n') { line++; col = 1 } else col++ }
            return JsonException("$msg (line $line, column $col)")
        }

        fun value(depth: Int): Node {
            if (depth > MAX_DEPTH) throw fail("Nested too deeply")
            ws()
            if (end()) throw fail("Unexpected end of text")
            return when (val c = s[i]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> Node.Str(str())
                't' -> lit("true", Node.Bool(true))
                'f' -> lit("false", Node.Bool(false))
                'n' -> lit("null", Node.Null)
                else -> if (c == '-' || c.isDigit()) num() else throw fail("Unexpected character '$c'")
            }
        }

        fun lit(word: String, node: Node): Node {
            if (!s.startsWith(word, i)) throw fail("Unexpected text")
            i += word.length
            return node
        }

        fun num(): Node {
            val m = NUMBER.matchAt(s, i) ?: throw fail("Invalid number")
            i += m.value.length
            return Node.Num(m.value)
        }

        fun str(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (end()) throw fail("Unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (end()) throw fail("Unterminated string")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length || !s.substring(i, i + 4).all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) throw fail("Invalid \\u escape")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4
                            }
                            else -> throw fail("Invalid escape \\$e")
                        }
                    }
                    c < ' ' -> throw fail("Control character inside a string")
                    else -> sb.append(c)
                }
            }
        }

        fun arr(depth: Int): Node {
            i++
            val items = ArrayList<Node>()
            ws()
            if (!end() && s[i] == ']') { i++; return Node.Arr(items) }
            while (true) {
                items.add(value(depth + 1))
                ws()
                if (end()) throw fail("Missing ] at the end")
                when (s[i++]) {
                    ',' -> continue
                    ']' -> return Node.Arr(items)
                    else -> { i--; throw fail("Expected , or ]") }
                }
            }
        }

        fun obj(depth: Int): Node {
            i++
            val entries = ArrayList<Pair<String, Node>>()
            ws()
            if (!end() && s[i] == '}') { i++; return Node.Obj(entries) }
            while (true) {
                ws()
                if (end() || s[i] != '"') throw fail("Expected a property name in quotes")
                val key = str()
                ws()
                if (end() || s[i] != ':') throw fail("Expected :")
                i++
                entries.add(key to value(depth + 1))
                ws()
                if (end()) throw fail("Missing } at the end")
                when (s[i++]) {
                    ',' -> continue
                    '}' -> return Node.Obj(entries)
                    else -> { i--; throw fail("Expected , or }") }
                }
            }
        }
    }

    fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append("\\u").append(Integer.toHexString(c.code).padStart(4, '0'))
            else -> sb.append(c)
        }
        return sb.append('"').toString()
    }

    /** [indent] 0 gives the compact form. */
    fun print(node: Node, indent: Int = 2, sortKeys: Boolean = false): String {
        val sb = StringBuilder()
        fun nl(level: Int) { if (indent > 0) { sb.append('\n'); repeat(level * indent) { sb.append(' ') } } }
        fun write(n: Node, level: Int) {
            when (n) {
                is Node.Str -> sb.append(quote(n.value))
                is Node.Num -> sb.append(n.text)
                is Node.Bool -> sb.append(n.value)
                Node.Null -> sb.append("null")
                is Node.Arr -> {
                    if (n.items.isEmpty()) { sb.append("[]"); return }
                    sb.append('[')
                    n.items.forEachIndexed { k, item -> if (k > 0) sb.append(','); nl(level + 1); write(item, level + 1) }
                    nl(level); sb.append(']')
                }
                is Node.Obj -> {
                    if (n.entries.isEmpty()) { sb.append("{}"); return }
                    val list = if (sortKeys) n.entries.sortedBy { it.first } else n.entries
                    sb.append('{')
                    list.forEachIndexed { k, (key, v) ->
                        if (k > 0) sb.append(',')
                        nl(level + 1); sb.append(quote(key)).append(if (indent > 0) ": " else ":"); write(v, level + 1)
                    }
                    nl(level); sb.append('}')
                }
            }
        }
        write(node, 0)
        return sb.toString()
    }

    fun format(text: String, indent: Int = 2, sortKeys: Boolean = false): String = print(parse(text), indent, sortKeys)
}

/** Line-by-line comparison of two texts. */
object TextDiff {
    enum class Type { SAME, ADDED, REMOVED }
    data class Line(val type: Type, val text: String)
    data class Result(val lines: List<Line>, val added: Int, val removed: Int, val same: Int)

    const val MAX_LINES = 1500

    fun diff(a: String, b: String, ignoreCase: Boolean = false, ignoreSpace: Boolean = false): Result {
        val x = a.replace("\r\n", "\n").split('\n')
        val y = b.replace("\r\n", "\n").split('\n')
        require(x.size <= MAX_LINES && y.size <= MAX_LINES) { "Each text can have at most $MAX_LINES lines." }
        fun key(s: String): String {
            var k = s
            if (ignoreSpace) k = k.trim().replace(Regex("\\s+"), " ")
            if (ignoreCase) k = k.lowercase()
            return k
        }
        val kx = x.map(::key); val ky = y.map(::key)
        var start = 0
        while (start < kx.size && start < ky.size && kx[start] == ky[start]) start++
        var ex = kx.size; var ey = ky.size
        while (ex > start && ey > start && kx[ex - 1] == ky[ey - 1]) { ex--; ey-- }
        val n = ex - start; val m = ey - start
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            lcs[i][j] = if (kx[start + i] == ky[start + j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val out = ArrayList<Line>()
        for (i in 0 until start) out.add(Line(Type.SAME, x[i]))
        var i = 0; var j = 0
        while (i < n && j < m) {
            when {
                kx[start + i] == ky[start + j] -> { out.add(Line(Type.SAME, x[start + i])); i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> { out.add(Line(Type.REMOVED, x[start + i])); i++ }
                else -> { out.add(Line(Type.ADDED, y[start + j])); j++ }
            }
        }
        while (i < n) { out.add(Line(Type.REMOVED, x[start + i])); i++ }
        while (j < m) { out.add(Line(Type.ADDED, y[start + j])); j++ }
        for (k in ex until kx.size) out.add(Line(Type.SAME, x[k]))
        return Result(out, out.count { it.type == Type.ADDED }, out.count { it.type == Type.REMOVED }, out.count { it.type == Type.SAME })
    }
}

/** Reads a JSON Web Token. The signature is NOT verified: that needs the secret or public key. */
object Jwt {
    data class Decoded(val header: String, val payload: String, val times: List<Pair<String, String>>, val signatureBytes: Int)

    fun decode(token: String): Decoded {
        val parts = token.trim().removePrefix("Bearer ").trim().split('.')
        require(parts.size == 3) { "A token has three parts separated by dots." }
        fun part(p: String, name: String): String {
            val bytes = try { Base64.getUrlDecoder().decode(p) } catch (e: IllegalArgumentException) { throw IllegalArgumentException("The $name is not valid Base64.") }
            val text = try {
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            } catch (e: java.nio.charset.CharacterCodingException) { throw IllegalArgumentException("The $name is not readable text.") }
            return try { Json.format(text) } catch (e: Json.JsonException) { throw IllegalArgumentException("The $name is not valid JSON.") }
        }
        val header = part(parts[0], "header")
        val payload = part(parts[1], "payload")
        val sig = try { Base64.getUrlDecoder().decode(parts[2]).size } catch (e: IllegalArgumentException) { throw IllegalArgumentException("The signature is not valid Base64.") }
        val obj = Json.parse(payload) as? Json.Node.Obj
        val times = ArrayList<Pair<String, String>>()
        for ((key, label) in listOf("iat" to "Issued at", "nbf" to "Not before", "exp" to "Expires")) {
            val n = obj?.entries?.firstOrNull { it.first == key }?.second as? Json.Node.Num ?: continue
            val secs = n.text.toDoubleOrNull()?.toLong() ?: continue
            if (secs in 0..253402300799L) times.add(label to Instant.ofEpochSecond(secs).toString())
        }
        return Decoded(header, payload, times, sig)
    }
}

/** CSV and JSON conversion for flat tables. */
object CsvJson {
    fun parseCsv(text: String, delimiter: Char = ','): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        val s = text
        var any = false
        while (i < s.length) {
            val c = s[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { cell.append('"'); i++ } else quoted = false
                } else cell.append(c)
            } else when {
                c == '"' && cell.isEmpty() -> { quoted = true; any = true }
                c == delimiter -> { row.add(cell.toString()); cell.setLength(0); any = true }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
                    if (any || cell.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
                    row = ArrayList(); cell.setLength(0); any = false
                }
                else -> { cell.append(c); any = true }
            }
            i++
        }
        require(!quoted) { "A quoted value is missing its closing quote." }
        if (any || cell.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }

    fun csvToJson(text: String, delimiter: Char = ','): String {
        val rows = parseCsv(text.removePrefix("\uFEFF"), delimiter)
        require(rows.isNotEmpty()) { "Enter some CSV with a header row." }
        val rawHeader = rows[0].map { it.trim() }
        require(rawHeader.all { it.isNotEmpty() }) { "Every column in the header row needs a name." }
        val seen = HashMap<String, Int>()
        val header = rawHeader.map { h ->
            val n = (seen[h] ?: 0) + 1
            seen[h] = n
            if (n == 1) h else "${h}_$n"
        }
        rows.drop(1).forEachIndexed { k, r -> require(r.size <= header.size || r.drop(header.size).all { it.isBlank() }) { "Row ${k + 2} has ${r.size} values but the header has only ${header.size} columns." } }
        val objs = rows.drop(1).map { r -> Json.Node.Obj(header.mapIndexed { k, h -> h to (Json.Node.Str(r.getOrElse(k) { "" }) as Json.Node) }) }
        return Json.print(Json.Node.Arr(objs))
    }

    private fun cellOf(n: Json.Node): String = when (n) {
        is Json.Node.Str -> n.value
        is Json.Node.Num -> n.text
        is Json.Node.Bool -> n.value.toString()
        Json.Node.Null -> ""
        else -> Json.print(n, 0)
    }

    fun quoteCell(v: String, delimiter: Char): String =
        if (v.any { it == '"' || it == delimiter || it == '\n' || it == '\r' } || v.startsWith(" ") || v.endsWith(" ")) "\"" + v.replace("\"", "\"\"") + "\"" else v

    fun jsonToCsv(text: String, delimiter: Char = ','): String {
        val root = Json.parse(text)
        val arr = root as? Json.Node.Arr ?: throw IllegalArgumentException("The JSON must be an array of objects, like [{\"a\":1}].")
        require(arr.items.isNotEmpty()) { "The array is empty." }
        val objs = arr.items.map { it as? Json.Node.Obj ?: throw IllegalArgumentException("Every item in the array must be an object.") }
        val columns = LinkedHashSet<String>()
        objs.forEach { o -> o.entries.forEach { columns.add(it.first) } }
        require(columns.isNotEmpty()) { "The objects have no fields to turn into columns." }
        val sb = StringBuilder()
        sb.append(columns.joinToString(delimiter.toString()) { quoteCell(it, delimiter) })
        for (o in objs) {
            sb.append('\n')
            sb.append(columns.joinToString(delimiter.toString()) { c -> quoteCell(o.entries.lastOrNull { it.first == c }?.second?.let { cellOf(it) }.orEmpty(), delimiter) })
        }
        return sb.toString()
    }
}

/** Unix file permissions: octal <-> rwx. */
object Chmod {
    data class Result(val octal: String, val symbolic: String, val lines: List<String>)

    private fun triple(v: Int) = (if ((v and 4) != 0) "r" else "-") + (if ((v and 2) != 0) "w" else "-") + (if ((v and 1) != 0) "x" else "-")

    fun fromOctal(text: String): Result {
        val t = text.trim()
        require(t.length in 3..4 && t.all { it in '0'..'7' }) { "Use three or four octal digits, like 755 or 4755." }
        val padded = t.padStart(4, '0')
        val special = padded[0] - '0'
        val u = padded[1] - '0'; val g = padded[2] - '0'; val o = padded[3] - '0'
        val chars = StringBuilder(triple(u) + triple(g) + triple(o))
        if ((special and 4) != 0) chars[2] = if ((u and 1) != 0) 's' else 'S'
        if ((special and 2) != 0) chars[5] = if ((g and 1) != 0) 's' else 'S'
        if ((special and 1) != 0) chars[8] = if ((o and 1) != 0) 't' else 'T'
        val lines = listOf("Owner: ${triple(u)}", "Group: ${triple(g)}", "Others: ${triple(o)}") +
            listOfNotNull(
                if ((special and 4) != 0) "setuid: runs as the file's owner" else null,
                if ((special and 2) != 0) "setgid: runs as the file's group" else null,
                if ((special and 1) != 0) "sticky: only the owner can delete files inside" else null,
            )
        return Result(if (special == 0) padded.substring(1) else padded, chars.toString(), lines)
    }

    /** Bits are [owner r,w,x, group r,w,x, others r,w,x]. */
    fun fromBits(bits: List<Boolean>): String {
        require(bits.size == 9) { "Nine permission bits are needed." }
        return (0..2).joinToString("") { k -> ((if (bits[k * 3]) 4 else 0) + (if (bits[k * 3 + 1]) 2 else 0) + (if (bits[k * 3 + 2]) 1 else 0)).toString() }
    }

    fun toBits(octal3: String): List<Boolean> = octal3.padStart(3, '0').takeLast(3).flatMap { d -> val v = d - '0'; listOf((v and 4) != 0, (v and 2) != 0, (v and 1) != 0) }
}
