package com.omnitools.app.tools.tasks

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Task(val id: String, val title: String, val done: Boolean, val created: Long)

/** Keeps all tasks in one small JSON file in the app's private storage, written via a temp file so a crash can't corrupt it. */
class TasksRepository(context: Context) {

    private val file = File(context.filesDir, "tasks.json")

    suspend fun load(): List<Task> = withContext(Dispatchers.IO) { decode(if (file.exists()) file.readText(Charsets.UTF_8) else null) }

    suspend fun save(tasks: List<Task>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "tasks.json.tmp")
        tmp.writeText(encode(tasks), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(encode(tasks), Charsets.UTF_8)
            tmp.delete()
        }
    }

    companion object {
        fun newTask(title: String): Task = Task(UUID.randomUUID().toString(), title.trim(), false, System.currentTimeMillis())

        fun encode(tasks: List<Task>): String {
            val arr = JSONArray()
            tasks.forEach { arr.put(JSONObject().put("id", it.id).put("t", it.title).put("d", it.done).put("c", it.created)) }
            return arr.toString()
        }

        fun decode(raw: String?): List<Task> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    Task(o.getString("id"), o.getString("t"), o.optBoolean("d"), o.optLong("c"))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
