package com.omnitools.app.tools.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import java.io.ByteArrayOutputStream

/** Bitmap operations for the image editor. Each returns a NEW bitmap and never modifies or recycles its input. */
object ImageOps {

    /** The longest side of the working copy. Larger photos are reduced so editing fits in a budget phone's memory. */
    const val MAX_WORKING_SIDE = 3000

    enum class Format(val label: String, val mime: String, val extension: String, val lossy: Boolean, val keepsTransparency: Boolean) {
        JPEG("JPEG", "image/jpeg", "jpg", true, false),
        PNG("PNG", "image/png", "png", false, true),
        WEBP("WebP", "image/webp", "webp", true, true),
    }

    fun rotate(src: Bitmap, degrees: Float): Bitmap {
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun flip(src: Bitmap, horizontal: Boolean): Bitmap {
        val m = Matrix().apply { if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun crop(src: Bitmap, box: ImageMath.CropBox): Bitmap =
        Bitmap.createBitmap(src, box.x, box.y, box.width, box.height)

    fun scale(src: Bitmap, percent: Int): Bitmap {
        if (percent == 100) return src
        val (w, h) = ImageMath.scaledSize(src.width, src.height, percent)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    /**
     * Encodes [src] after scaling it by [scalePercent]. JPEG cannot store transparency, so transparent pixels are
     * flattened onto white first. [quality] (1..100) only affects lossy formats.
     */
    fun encode(src: Bitmap, format: Format, quality: Int, scalePercent: Int = 100): ByteArray {
        var work = scale(src, scalePercent)
        if (!format.keepsTransparency && work.hasAlpha()) {
            val flat = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply {
                drawColor(Color.WHITE)
                drawBitmap(work, 0f, 0f, null)
            }
            work = flat
        }
        val compress = when (format) {
            Format.JPEG -> Bitmap.CompressFormat.JPEG
            Format.PNG -> Bitmap.CompressFormat.PNG
            Format.WEBP ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
                else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }
        val out = ByteArrayOutputStream()
        val ok = work.compress(compress, quality.coerceIn(1, 100), out)
        check(ok) { "The image could not be encoded." }
        return out.toByteArray()
    }
}
