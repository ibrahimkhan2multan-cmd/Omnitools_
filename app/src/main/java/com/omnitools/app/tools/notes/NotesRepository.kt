package com.omnitools.app.tools.notes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Note(val id: String, val title: String, val body: String, val updated: Long)

/**
 * Stores each note as a small JSON file in the app's private storage (filesDir/notes).
 * Files are written to a temporary name first and then renamed, so a crash mid-save cannot corrupt a note.
 */
class NotesRepository(context: Context) {

    private val dir = File(context.filesDir, "notes").apply { mkdirs() }

    private fun fileFor(id: String) = File(dir, "$id.json")

    suspend fun list(): List<Note> = withContext(Dispatchers.IO) {
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .mapNotNull { read(it) }
            .sortedByDescending { it.updated }
    }

    suspend fun get(id: String): Note? = withContext(Dispatchers.IO) {
        fileFor(id).takeIf { it.exists() }?.let { read(it) }
    }

    suspend fun save(note: Note) = withContext(Dispatchers.IO) {
        val json = JSONObject()
            .put("id", note.id)
            .put("title", note.title)
            .put("body", note.body)
            .put("updated", note.updated)
        val target = fileFor(note.id)
        val tmp = File(dir, "${note.id}.tmp")
        tmp.writeText(json.toString(), Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(json.toString(), Charsets.UTF_8)
            tmp.delete()
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { fileFor(id).delete() }

    private fun read(file: File): Note? = try {
        val o = JSONObject(file.readText(Charsets.UTF_8))
        Note(o.getString("id"), o.optString("title"), o.optString("body"), o.optLong("updated"))
    } catch (e: Exception) {
        null // Skip unreadable files instead of crashing the list.
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        /** Case-insensitive match on title or body. Pure function (unit tested). */
        fun matches(note: Note, query: String): Boolean {
            val q = query.trim()
            if (q.isEmpty()) return true
            return note.title.contains(q, ignoreCase = true) || note.body.contains(q, ignoreCase = true)
        }
    }
}
