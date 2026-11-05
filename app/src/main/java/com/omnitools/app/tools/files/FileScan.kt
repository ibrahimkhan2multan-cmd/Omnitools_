package com.omnitools.app.tools.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.InputStream
import java.security.MessageDigest

/** One file found in a folder. [id] is the document id inside the picked tree; [path] is relative to the folder. */
data class FileEntry(val path: String, val name: String, val size: Long, val id: String)

/** Statistics about a list of files. Pure Kotlin, unit tested. */
object FileStats {
    data class TypeRow(val label: String, val files: Int, val bytes: Long)
    data class Summary(val files: Int, val bytes: Long, val byType: List<TypeRow>, val largest: List<FileEntry>)

    private val groups = mapOf(
        "Images" to setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "raw", "dng"),
        "Videos" to setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "flv", "wmv", "m4v"),
        "Audio" to setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "amr"),
        "Documents" to setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv", "md", "epub"),
        "Archives" to setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz"),
        "Apps" to setOf("apk", "xapk", "aab"),
        "Code" to setOf("kt", "java", "js", "ts", "py", "c", "cpp", "h", "html", "css", "json", "xml", "sh", "gradle"),
    )

    fun extension(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0 || dot == name.length - 1) "" else name.substring(dot + 1).lowercase()
    }

    fun typeGroup(name: String): String {
        val ext = extension(name)
        return groups.entries.firstOrNull { ext in it.value }?.key ?: "Other"
    }

    fun summarize(entries: List<FileEntry>, topFiles: Int = 20): Summary {
        val byType = entries.groupBy { typeGroup(it.name) }
            .map { (label, list) -> TypeRow(label, list.size, list.sumOf { it.size }) }
            .sortedByDescending { it.bytes }
        return Summary(entries.size, entries.sumOf { it.size }, byType, entries.sortedByDescending { it.size }.take(topFiles))
    }
}

/** Finds identical files: same size, then same first 64 KB, then same full contents. */
object Duplicates {
    const val PARTIAL = 64L * 1024

    data class Group(val size: Long, val files: List<FileEntry>) {
        val wasted: Long get() = size * (files.size - 1)
    }

    /** [hash] must return a digest of the first [limit] bytes of the file (the whole file when [limit] is null). */
    fun find(
        entries: List<FileEntry>,
        minSize: Long = 1,
        hash: (FileEntry, Long?) -> String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        shouldStop: () -> Boolean = { false },
    ): List<Group> {
        val bySize = entries.filter { it.size >= minSize }.groupBy { it.size }.filter { it.value.size > 1 }
        val total = bySize.values.sumOf { it.size }
        var done = 0
        val result = ArrayList<Group>()
        for ((size, sameSize) in bySize) {
            if (shouldStop()) break
            val partial = sameSize.groupBy { hash(it, PARTIAL).also { done++; onProgress(done, total * 2) } }.values.filter { it.size > 1 }
            for (candidates in partial) {
                if (size <= PARTIAL) {
                    result.add(Group(size, candidates.sortedBy { it.path }))
                } else {
                    candidates.groupBy { hash(it, null).also { done++; onProgress(done, total * 2) } }.values.filter { it.size > 1 }
                        .forEach { result.add(Group(size, it.sortedBy { f -> f.path })) }
                }
                if (shouldStop()) break
            }
        }
        return result.sortedByDescending { it.wasted }
    }

    /** From each group, picks every file except the first (by path) as the suggested ones to delete. */
    fun suggestKeepFirst(groups: List<Group>): Set<String> = groups.flatMap { it.files.drop(1) }.map { it.id }.toSet()

    /** Never lets a group lose all of its copies: if everything in a group is selected, the first one is kept. */
    fun safeToDelete(groups: List<Group>, selected: Set<String>): Set<String> {
        val out = HashSet(selected)
        for (g in groups) if (g.files.isNotEmpty() && g.files.all { it.id in out }) out.remove(g.files.first().id)
        return out
    }
}

/** Reads a folder the user picked through the system picker. Only that folder is reachable. */
object SafWalker {
    data class Walk(val entries: List<FileEntry>, val truncated: Boolean)

    fun walk(resolver: ContentResolver, tree: Uri, maxEntries: Int, shouldStop: () -> Boolean, onCount: (Int) -> Unit): Walk {
        val out = ArrayList<FileEntry>()
        val stack = ArrayDeque<Pair<String, String>>()
        stack.add(DocumentsContract.getTreeDocumentId(tree) to "")
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE,
        )
        var truncated = false
        while (stack.isNotEmpty() && !shouldStop()) {
            val (dirId, prefix) = stack.removeLast()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId)
            val cursor = try { resolver.query(children, projection, null, null, null) } catch (e: Exception) { null } ?: continue
            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getString(0) ?: continue
                    val name = it.getString(1) ?: continue
                    val isDir = it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                    if (isDir) {
                        stack.add(id to "$prefix$name/")
                    } else {
                        if (out.size >= maxEntries) { truncated = true; break }
                        out.add(FileEntry("$prefix$name", name, if (it.isNull(3)) 0L else it.getLong(3), id))
                        if (out.size % 200 == 0) onCount(out.size)
                    }
                }
            }
            if (truncated) break
        }
        onCount(out.size)
        return Walk(out, truncated)
    }

    fun documentUri(tree: Uri, id: String): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)

    fun hash(resolver: ContentResolver, tree: Uri, entry: FileEntry, limit: Long?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val stream: InputStream = resolver.openInputStream(documentUri(tree, entry.id)) ?: return "unreadable:${entry.id}"
        stream.use { s ->
            val buf = ByteArray(64 * 1024)
            var remaining = limit ?: Long.MAX_VALUE
            while (remaining > 0) {
                val n = s.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                if (n < 0) break
                digest.update(buf, 0, n)
                remaining -= n
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    fun delete(resolver: ContentResolver, tree: Uri, id: String): Boolean =
        try { DocumentsContract.deleteDocument(resolver, documentUri(tree, id)) } catch (e: Exception) { false }
}
