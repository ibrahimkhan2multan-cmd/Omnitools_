package com.omnitools.app.tools.hash

import java.io.InputStream
import java.security.MessageDigest

/** Message digests for text and streams. Streams are read in 64 KiB chunks, so file size never matters for memory. */
object HashTool {

    enum class Algo(val label: String, val jca: String) {
        MD5("MD5", "MD5"),
        SHA1("SHA-1", "SHA-1"),
        SHA256("SHA-256", "SHA-256"),
        SHA512("SHA-512", "SHA-512"),
    }

    fun hex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append("0123456789abcdef"[v shr 4]).append("0123456789abcdef"[v and 0x0F])
        }
        return sb.toString()
    }

    fun digestText(algo: Algo, text: String): String =
        hex(MessageDigest.getInstance(algo.jca).digest(text.toByteArray(Charsets.UTF_8)))

    /**
     * Computes every algorithm in a single pass over [input]. [onProgress] receives the number of bytes read so far
     * and may be called often, so keep it cheap. [isCancelled] is polled between chunks.
     */
    fun digestAll(
        input: InputStream,
        onProgress: (Long) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): Map<Algo, String> {
        val digests = Algo.values().associateWith { MessageDigest.getInstance(it.jca) }
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            if (isCancelled()) throw InterruptedException("cancelled")
            val n = input.read(buffer)
            if (n < 0) break
            digests.values.forEach { it.update(buffer, 0, n) }
            total += n
            onProgress(total)
        }
        return digests.mapValues { hex(it.value.digest()) }
    }

    /** Compares an expected checksum typed by the user with a computed one, ignoring case and spaces. */
    fun matches(expected: String, actual: String): Boolean =
        expected.filterNot { it.isWhitespace() }.equals(actual, ignoreCase = true)
}
