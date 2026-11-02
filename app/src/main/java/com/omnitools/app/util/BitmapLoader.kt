package com.omnitools.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/** Memory-safe image decoding. Photos are downscaled while decoding, never loaded at full size first. */
object BitmapLoader {

    data class Info(val width: Int, val height: Int, val mime: String?)

    /** Reads only the image header: dimensions and MIME type. Returns null if the file is not a decodable image. */
    fun info(context: Context, uri: Uri): Info? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth > 0 && o.outHeight > 0) Info(o.outWidth, o.outHeight, o.outMimeType) else null
    } catch (e: Exception) {
        null
    }

    /** Smallest power-of-two sample size that keeps the longest side at or below [maxSide]. */
    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > maxSide) sample *= 2
        return sample
    }

    /**
     * Decodes [uri] so the longest side is at most about [maxSide] pixels (it may be up to 2x smaller,
     * because sampling uses powers of two). Applies the photo's EXIF rotation when [applyExif] is true.
     * Returns null when the file cannot be read or decoded.
     */
    fun decode(context: Context, uri: Uri, maxSide: Int, applyExif: Boolean = true): Bitmap? {
        val meta = info(context, uri) ?: return null
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(meta.width, meta.height, maxSide) }
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            if (applyExif) rotateByExif(context, uri, bmp) else bmp
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateByExif(context: Context, uri: Uri, bmp: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return bmp
        }
        return try {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).also { if (it !== bmp) bmp.recycle() }
        } catch (e: OutOfMemoryError) {
            bmp
        }
    }
}
