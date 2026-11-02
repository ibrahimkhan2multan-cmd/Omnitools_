package com.omnitools.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Basic details about a file the user picked with the system file picker. Any field may be unavailable. */
data class FileDetails(
    val name: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
    val lastModified: Long?,
) {
    val extension: String? get() = name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it != name }
}

object FileInfo {
    fun query(context: Context, uri: Uri): FileDetails {
        var name: String? = null
        var size: Long? = null
        var modified: Long? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    val m = c.getColumnIndex("last_modified") // DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                    if (m >= 0 && !c.isNull(m)) modified = c.getLong(m)
                }
            }
        } catch (e: Exception) {
            // Some providers refuse the query; fall through with whatever we have.
        }
        val mime = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
        return FileDetails(name, size, mime, modified)
    }
}
