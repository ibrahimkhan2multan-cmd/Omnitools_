package com.omnitools.app.tools.zip

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Decides which ZIP entry names are safe to write to disk (blocks "Zip Slip" path tricks). */
object ZipSafety {
    const val MAX_ENTRIES = 20_000
    const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024
    const val MAX_NAME_LENGTH = 255

    /**
     * Folder and file names that make up [name], or null if the entry must not be extracted: absolute paths, drive
     * letters, any ".." part, control characters, or an over-long name. Backslashes count as separators.
     */
    fun safePath(name: String): List<String>? {
        if (name.isEmpty() || name.any { it.code < 32 }) return null
        val n = name.replace('\\', '/')
        if (n.startsWith("/")) return null
        if (Regex("^[A-Za-z]:").containsMatchIn(n)) return null
        val parts = n.split('/').filter { it.isNotEmpty() && it != "." }
        if (parts.isEmpty()) return null
        if (parts.any { it == ".." || it.length > MAX_NAME_LENGTH }) return null
        return parts
    }
}

/** Reading, extracting and creating ZIP files. Pure java.util.zip, so it runs (and is tested) on the JVM. */
object ZipEngine {

    class ZipLimitException(message: String) : Exception(message)

    data class Entry(val name: String, val size: Long, val compressedSize: Long, val isDirectory: Boolean, val safe: Boolean)

    /** Where extracted files go. Implemented over Android folders in the app and over a temp folder in tests. */
    interface Sink {
        fun directory(path: List<String>)
        fun file(path: List<String>): OutputStream
    }

    data class ExtractResult(val files: Int, val folders: Int, val skippedUnsafe: Int, val bytes: Long)

    fun list(file: File): List<Entry> = ZipFile(file).use { zip ->
        val entries = zip.entries().toList()
        if (entries.size > ZipSafety.MAX_ENTRIES) throw ZipLimitException("This ZIP has more than ${ZipSafety.MAX_ENTRIES} entries.")
        entries.map { Entry(it.name, it.size, it.compressedSize, it.isDirectory, ZipSafety.safePath(it.name) != null) }
    }

    /**
     * Extracts every safe entry into [sink]. Unsafe names are skipped and counted. The number of bytes actually
     * written is limited by [maxBytes] (declared sizes in a ZIP can lie), which stops "zip bombs".
     */
    fun extract(
        file: File,
        sink: Sink,
        maxBytes: Long = ZipSafety.MAX_TOTAL_BYTES,
        isCancelled: () -> Boolean = { false },
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): ExtractResult = ZipFile(file).use { zip ->
        val entries = zip.entries().toList()
        if (entries.size > ZipSafety.MAX_ENTRIES) throw ZipLimitException("This ZIP has more than ${ZipSafety.MAX_ENTRIES} entries.")
        var files = 0
        var folders = 0
        var skipped = 0
        var total = 0L
        val buffer = ByteArray(64 * 1024)
        entries.forEachIndexed { index, entry ->
            if (isCancelled()) throw InterruptedException("cancelled")
            val path = ZipSafety.safePath(entry.name)
            if (path == null) {
                skipped++
            } else if (entry.isDirectory) {
                sink.directory(path)
                folders++
            } else {
                zip.getInputStream(entry).use { input ->
                    sink.file(path).use { out ->
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > maxBytes) throw ZipLimitException("Stopped: the files inside are larger than the safety limit.")
                            out.write(buffer, 0, n)
                        }
                    }
                }
                files++
            }
            onProgress(index + 1, entries.size)
        }
        ExtractResult(files, folders, skipped, total)
    }

    class Input(val name: String, val open: () -> InputStream)

    /** "a.txt", "a.txt" -> "a.txt", "a (1).txt". Slashes in names are replaced so nothing becomes a folder. */
    fun uniqueNames(names: List<String>): List<String> {
        val used = HashSet<String>()
        return names.map { raw ->
            val clean = raw.replace('/', '_').replace('\\', '_').ifBlank { "file" }
            var candidate = clean
            var n = 1
            val dot = clean.lastIndexOf('.').takeIf { it > 0 }
            while (!used.add(candidate.lowercase())) {
                candidate = if (dot != null) "${clean.substring(0, dot)} ($n)${clean.substring(dot)}" else "$clean ($n)"
                n++
            }
            candidate
        }
    }

    /** Writes [inputs] into a new ZIP on [out] (which is closed). Returns the number of files added. */
    fun create(inputs: List<Input>, out: OutputStream, onProgress: (Int, Int) -> Unit = { _, _ -> }): Int {
        val names = uniqueNames(inputs.map { it.name })
        val buffer = ByteArray(64 * 1024)
        ZipOutputStream(out).use { zip ->
            inputs.forEachIndexed { i, input ->
                zip.putNextEntry(ZipEntry(names[i]))
                input.open().use { stream ->
                    while (true) {
                        val n = stream.read(buffer)
                        if (n < 0) break
                        zip.write(buffer, 0, n)
                    }
                }
                zip.closeEntry()
                onProgress(i + 1, inputs.size)
            }
        }
        return inputs.size
    }
}
