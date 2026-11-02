package com.omnitools.app.tools.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Offline QR generation with ZXing. No network, no external services. */
object QrEncoder {

    const val MAX_CHARS = 2000

    class QrException(message: String) : Exception(message)

    /** Pure JVM step (unit tested): text -> module matrix. Throws [QrException] with a readable message. */
    fun matrix(
        text: String,
        size: Int = 512,
        level: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    ): BitMatrix {
        if (text.isEmpty()) throw QrException("Enter some content first.")
        if (text.length > MAX_CHARS) throw QrException("Too long: $MAX_CHARS characters maximum (you have ${text.length}).")
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to level,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2,
        )
        return try {
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        } catch (e: WriterException) {
            throw QrException("This content cannot be encoded as a QR code.")
        }
    }

    fun bitmap(
        text: String,
        size: Int = 768,
        level: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    ): Bitmap {
        val m = matrix(text, size, level)
        val w = m.width
        val h = m.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) pixels[row + x] = if (m.get(x, y)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
