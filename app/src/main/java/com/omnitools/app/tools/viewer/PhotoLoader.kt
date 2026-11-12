package com.omnitools.app.tools.viewer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns

/** Decodes a picture from a content Uri, shrunk so large photos do not run the phone out of memory. */
object PhotoLoader {
    class Loaded(val bitmap: Bitmap?, val error: String?)

    fun load(context: Context, uri: Uri, maxDim: Int = 2400): Loaded {
        val cr = context.contentResolver
        var failure: Throwable? = null
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                val src = ImageDecoder.createSource(cr, uri)
                val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    decoder.setTargetSampleSize(ViewerLogic.sampleSize(info.size.width, info.size.height, maxDim))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                return Loaded(bmp, null)
            } catch (t: Throwable) { failure = t }
        }
        try {
            val bmp = viaFactory(cr, uri, maxDim)
            if (bmp != null) return Loaded(bmp, null)
        } catch (t: Throwable) { failure = failure ?: t }
        val why = if (failure is OutOfMemoryError) "This picture is too large for the phone's memory." else "This phone cannot open this kind of picture."
        return Loaded(null, why)
    }

    private fun viaFactory(cr: ContentResolver, uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply { inSampleSize = ViewerLogic.sampleSize(bounds.outWidth, bounds.outHeight, maxDim) }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val degrees = try {
            cr.openInputStream(uri)?.use {
                when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Throwable) { 0f }
        if (degrees == 0f) return bmp
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
    }

    fun displayName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { if (it.isNotBlank()) return it }
            }
        } catch (_: Throwable) {}
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }
}
