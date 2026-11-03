package com.omnitools.app.tools.zip

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import java.io.IOException
import java.io.OutputStream

/**
 * Writes extracted files into a folder the user picked with the system folder picker. Folders are reused if they
 * already exist; an existing file is never overwritten (Android gives the new one a "(1)" name).
 */
class SafSink(private val resolver: ContentResolver, private val tree: Uri) : ZipEngine.Sink {
    private val rootId = DocumentsContract.getTreeDocumentId(tree)
    private val rootDoc: Uri = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
    private val dirs = HashMap<String, Uri>()

    private fun dirUri(path: List<String>): Uri {
        var current = rootDoc
        var key = ""
        for (segment in path) {
            key += "/$segment"
            val parent = current
            current = dirs.getOrPut(key) { findChildDirectory(parent, segment) ?: createDirectory(parent, segment) }
        }
        return current
    }

    private fun findChildDirectory(parent: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name && c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                }
            }
        }
        return null
    }

    private fun createDirectory(parent: Uri, name: String): Uri =
        DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: throw IOException("Could not create the folder \"$name\".")

    override fun directory(path: List<String>) {
        dirUri(path)
    }

    override fun file(path: List<String>): OutputStream {
        val parent = if (path.size > 1) dirUri(path.dropLast(1)) else rootDoc
        val name = path.last()
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        val uri = DocumentsContract.createDocument(resolver, parent, mime, name)
            ?: throw IOException("Could not create the file \"$name\".")
        return resolver.openOutputStream(uri) ?: throw IOException("Could not write the file \"$name\".")
    }
}
