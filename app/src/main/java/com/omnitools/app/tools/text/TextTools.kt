package com.omnitools.app.tools.text

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

/** Pure text utilities used by the Text Tools screen. Every function either returns a result or throws [TextToolException]. */
object TextTools {

    class TextToolException(message: String) : Exception(message)

    data class Stats(val characters: Int, val charactersNoSpaces: Int, val words: Int, val sentences: Int, val paragraphs: Int, val lines: Int)

    fun stats(text: String): Stats {
        if (text.isEmpty()) return Stats(0, 0, 0, 0, 0, 0)
        val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        val sentences = Regex("[^.!?]+[.!?]+|[^.!?]+$").findAll(text).count { it.value.isNotBlank() }
        val paragraphs = text.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }
        return Stats(
            characters = text.length,
            charactersNoSpaces = text.count { !it.isWhitespace() },
            words = words,
            sentences = sentences,
            paragraphs = paragraphs,
            lines = text.lines().size,
        )
    }

    fun upper(text: String) = text.uppercase()
    fun lower(text: String) = text.lowercase()

    fun titleCase(text: String): String =
        text.lowercase().split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    /** Collapses runs of spaces/tabs to one space and trims every line; also drops blank lines at both ends. */
    fun removeExtraWhitespace(text: String): String =
        text.lines().joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }.trim()

    /** Keeps the first occurrence of each line, preserving order. */
    fun removeDuplicateLines(text: String): String = text.lines().distinct().joinToString("\n")

    fun sortLines(text: String, descending: Boolean = false): String {
        val sorted = text.lines().sortedWith(String.CASE_INSENSITIVE_ORDER)
        return (if (descending) sorted.reversed() else sorted).joinToString("\n")
    }

    fun reverseText(text: String): String = StringBuilder(text).reverse().toString()

    fun numberLines(text: String): String =
        text.lines().mapIndexed { i, line -> "${i + 1}. $line" }.joinToString("\n")

    fun findReplace(text: String, find: String, replace: String, ignoreCase: Boolean): Pair<String, Int> {
        if (find.isEmpty()) throw TextToolException("Enter text to find.")
        var count = 0
        var index = text.indexOf(find, 0, ignoreCase)
        while (index >= 0) {
            count++
            index = text.indexOf(find, index + find.length, ignoreCase)
        }
        return text.replace(find, replace, ignoreCase) to count
    }

    fun base64Encode(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    fun base64Decode(text: String): String = try {
        String(Base64.getDecoder().decode(text.trim().replace("\n", "").replace("\r", "")), Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        throw TextToolException("This is not valid Base64 text.")
    }

    fun urlEncode(text: String): String = URLEncoder.encode(text, "UTF-8")

    fun urlDecode(text: String): String = try {
        URLDecoder.decode(text, "UTF-8")
    } catch (e: IllegalArgumentException) {
        throw TextToolException("This is not valid URL-encoded text.")
    }

    private fun parseJson(text: String): Any {
        val t = text.trim()
        if (t.isEmpty()) throw TextToolException("Enter some JSON first.")
        return try {
            when (t.first()) {
                '{' -> JSONObject(t)
                '[' -> JSONArray(t)
                else -> throw TextToolException("JSON must start with { or [.")
            }
        } catch (e: JSONException) {
            throw TextToolException("Invalid JSON: ${e.message?.lineSequence()?.firstOrNull() ?: "parse error"}")
        }
    }

    fun jsonFormat(text: String): String = when (val j = parseJson(text)) {
        is JSONObject -> j.toString(2)
        is JSONArray -> j.toString(2)
        else -> throw TextToolException("Invalid JSON.")
    }

    fun jsonMinify(text: String): String = when (val j = parseJson(text)) {
        is JSONObject -> j.toString()
        is JSONArray -> j.toString()
        else -> throw TextToolException("Invalid JSON.")
    }
}
