package com.omnitools.app.tools.editor

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** Loading and saving helpers for the code editor. Pure JVM, unit tested. */
object EditorText {
    const val MAX_BYTES = 1_000_000

    class EditorException(message: String) : Exception(message)

    /** Reads at most [MAX_BYTES]; a bigger file is refused rather than cut off. */
    fun readLimited(input: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > MAX_BYTES) throw EditorException("This file is larger than 1 MB, which is more than the editor can handle comfortably.")
        }
        return out.toByteArray()
    }

    /** Strict UTF-8: a file with other encodings or binary data is refused instead of being corrupted on save. */
    fun decodeUtf8(bytes: ByteArray): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            throw EditorException("This is not a UTF-8 text file (it may be binary or use another encoding).")
        }
        return text.removePrefix("﻿")
    }

    /** Windows line endings are shown as plain newlines; the flag says to put them back when saving. */
    fun normalizeNewlines(text: String): Pair<String, Boolean> =
        if (text.contains("\r\n")) text.replace("\r\n", "\n") to true else text to false

    fun encode(text: String, crlf: Boolean): ByteArray =
        (if (crlf) text.replace("\n", "\r\n") else text).toByteArray(Charsets.UTF_8)

    /** Line and column (both 1-based) of the cursor at [offset]. */
    fun lineAndColumn(text: String, offset: Int): Pair<Int, Int> {
        val end = offset.coerceIn(0, text.length)
        var line = 1
        var lineStart = 0
        for (i in 0 until end) if (text[i] == '\n') { line++; lineStart = i + 1 }
        return line to (end - lineStart + 1)
    }
}
