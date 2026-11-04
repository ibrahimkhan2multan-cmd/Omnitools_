package com.omnitools.app.tools.everyday

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** One small text file in the app's private storage, written through a temp file so a crash can't corrupt it. */
class EverydayStore(context: Context, name: String) {
    private val file = File(context.filesDir, name)

    suspend fun read(): String? = withContext(Dispatchers.IO) { if (file.exists()) file.readText(Charsets.UTF_8) else null }

    suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text, Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(text, Charsets.UTF_8)
            tmp.delete()
        }
    }
}
