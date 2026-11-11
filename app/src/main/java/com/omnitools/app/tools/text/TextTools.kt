package com.omnitools.app.tools.text

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

    fun base64Decode(text: String): String {
        val clean = text.filter { !it.isWhitespace() }
        // Accept both the normal alphabet (+ /) and the URL-safe one (- _), with or without "=" padding.
        val bytes = try {
            Base64.getDecoder().decode(clean)
        } catch (e: IllegalArgumentException) {
            try { Base64.getUrlDecoder().decode(clean) } catch (e2: IllegalArgumentException) { throw TextToolException("This is not valid Base64 text.") }
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun urlEncode(text: String): String = URLEncoder.encode(text, "UTF-8")

    fun urlDecode(text: String): String = try {
        URLDecoder.decode(text, "UTF-8")
    } catch (e: IllegalArgumentException) {
        throw TextToolException("This is not valid URL-encoded text.")
    }

    /**
     * Formats or minifies JSON with the app's own strict parser, which keeps every number exactly as written
     * (Android's org.json would turn 1.0 into 1 and 12345678901234567890 into 1.2345678901234567E19).
     */
    private fun reprint(text: String, indent: Int): String {
        val t = text.trim()
        if (t.isEmpty()) throw TextToolException("Enter some JSON first.")
        if (t.first() != '{' && t.first() != '[') throw TextToolException("JSON must start with { or [.")
        return try {
            com.omnitools.app.tools.advanced.Json.format(t, indent)
        } catch (e: com.omnitools.app.tools.advanced.Json.JsonException) {
            throw TextToolException("Invalid JSON: ${e.message?.lineSequence()?.firstOrNull() ?: "parse error"}")
        }
    }

    fun jsonFormat(text: String): String = reprint(text, 2)

    fun jsonMinify(text: String): String = reprint(text, 0)

    /** "hELLO. wORLD" -> "Hello. World". Capitalises the first letter of the text, of each sentence and of each line. */
    fun sentenceCase(text: String): String {
        val sb = StringBuilder(text.length)
        var capitalizeNext = true
        for (ch in text.lowercase()) {
            if (capitalizeNext && ch.isLetter()) {
                sb.append(ch.uppercaseChar())
                capitalizeNext = false
            } else {
                sb.append(ch)
                if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') capitalizeNext = true
                else if (ch.isLetterOrDigit()) capitalizeNext = false
            }
        }
        return sb.toString()
    }

    /** Swaps upper and lower case of every letter: "aBc" -> "AbC". */
    fun toggleCase(text: String): String =
        text.map { if (it.isUpperCase()) it.lowercaseChar() else if (it.isLowerCase()) it.uppercaseChar() else it }.joinToString("")

    /** "hello" -> "hElLo": letters alternate lower, upper, lower... (spaces and symbols do not count). */
    fun alternatingCase(text: String): String {
        var n = 0
        return text.map {
            if (it.isLetter()) (if (n++ % 2 == 0) it.lowercaseChar() else it.uppercaseChar()) else it
        }.joinToString("")
    }

    private fun words(line: String): List<String> =
        line.replace(Regex("(?<=\\p{Ll})(?=\\p{Lu})"), " ")
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }

    /** Each line becomes one identifier: "Hello World-foo" -> "helloWorldFoo". */
    fun camelCase(text: String): String = text.lines().joinToString("\n") { line ->
        words(line).mapIndexed { i, w -> if (i == 0) w else w.replaceFirstChar { it.uppercase() } }.joinToString("")
    }

    fun snakeCase(text: String): String = text.lines().joinToString("\n") { words(it).joinToString("_") }

    fun kebabCase(text: String): String = text.lines().joinToString("\n") { words(it).joinToString("-") }

    /** Joins all lines into one, separated by single spaces. */
    fun removeLineBreaks(text: String): String =
        text.split(Regex("\\s*[\\r\\n]+\\s*")).filter { it.isNotEmpty() }.joinToString(" ")

    fun removeEmptyLines(text: String): String = text.lines().filter { it.isNotBlank() }.joinToString("\n")

    fun addBullets(text: String): String = text.lines().joinToString("\n") { if (it.isBlank()) it else "\u2022 $it" }
}
