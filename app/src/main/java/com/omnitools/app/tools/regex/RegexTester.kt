package com.omnitools.app.tools.regex

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Runs a user-supplied regular expression with hard limits, so a pathological pattern
 * (catastrophic backtracking) cannot freeze the app.
 */
object RegexTester {

    const val MAX_INPUT = 20_000
    const val MAX_PATTERN = 500
    const val MAX_MATCHES = 200

    data class Match(val start: Int, val end: Int, val text: String, val groups: List<String?>)

    sealed class Result {
        data class Ok(val matches: List<Match>, val truncated: Boolean) : Result()
        data class Error(val message: String) : Result()
    }

    private class TimeoutException : RuntimeException()

    /** CharSequence wrapper that aborts matching once the deadline has passed. */
    private class Deadline(private val inner: CharSequence, private val deadlineNanos: Long) : CharSequence {
        override val length: Int get() = inner.length
        override fun get(index: Int): Char {
            if (System.nanoTime() > deadlineNanos) throw TimeoutException()
            return inner[index]
        }
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            Deadline(inner.subSequence(startIndex, endIndex), deadlineNanos)
        override fun toString(): String = inner.toString()
    }

    fun run(
        pattern: String,
        input: String,
        ignoreCase: Boolean = false,
        multiline: Boolean = false,
        dotAll: Boolean = false,
        timeoutMs: Long = 1000,
    ): Result {
        if (pattern.isEmpty()) return Result.Error("Enter a pattern.")
        if (pattern.length > MAX_PATTERN) return Result.Error("Pattern is too long (max $MAX_PATTERN characters).")
        if (input.length > MAX_INPUT) return Result.Error("Text is too long (max $MAX_INPUT characters).")

        var flags = 0
        if (ignoreCase) flags = flags or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        if (multiline) flags = flags or Pattern.MULTILINE
        if (dotAll) flags = flags or Pattern.DOTALL

        val compiled = try {
            Pattern.compile(pattern, flags)
        } catch (e: PatternSyntaxException) {
            return Result.Error("Invalid pattern: ${e.description}")
        }

        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        val matcher = compiled.matcher(Deadline(input, deadline))
        val found = ArrayList<Match>()
        var truncated = false
        try {
            while (matcher.find()) {
                if (found.size >= MAX_MATCHES) {
                    truncated = true
                    break
                }
                val groups = (1..matcher.groupCount()).map { matcher.group(it) }
                found += Match(matcher.start(), matcher.end(), matcher.group(), groups)
                // Avoid an endless loop on empty matches such as "a*".
                if (matcher.end() == matcher.start() && matcher.end() >= input.length) break
            }
        } catch (e: TimeoutException) {
            return Result.Error("Stopped: the pattern took longer than ${timeoutMs} ms. It may have catastrophic backtracking.")
        }
        return Result.Ok(found, truncated)
    }
}
