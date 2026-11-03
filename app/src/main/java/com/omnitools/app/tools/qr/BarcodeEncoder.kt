package com.omnitools.app.tools.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

/** Offline 1D barcode generation with ZXing. */
object BarcodeEncoder {

    enum class Type(val label: String, val format: BarcodeFormat, val hint: String) {
        CODE_128("Code 128", BarcodeFormat.CODE_128, "Any text or numbers (plain English characters), up to 80 characters."),
        CODE_39("Code 39", BarcodeFormat.CODE_39, "Letters A-Z, digits and - . space \$ / + %. Lower case is changed to upper case."),
        EAN_13("EAN-13", BarcodeFormat.EAN_13, "12 digits (the check digit is added) or 13 digits with the check digit."),
        EAN_8("EAN-8", BarcodeFormat.EAN_8, "7 digits (the check digit is added) or 8 digits with the check digit."),
        UPC_A("UPC-A", BarcodeFormat.UPC_A, "11 digits (the check digit is added) or 12 digits with the check digit."),
        ITF("ITF", BarcodeFormat.ITF, "Digits only, an even number of them."),
    }

    class BarcodeException(message: String) : Exception(message)

    /** Returns a readable problem with [text] for [type], or null if it is acceptable. */
    fun validate(type: Type, text: String): String? {
        if (text.isEmpty()) return "Enter the content first."
        return when (type) {
            Type.CODE_128 ->
                if (text.length > 80) "At most 80 characters." else if (text.any { it.code > 127 }) "Only plain English characters (no accents or emoji)." else null
            Type.CODE_39 ->
                if (text.uppercase().any { it !in "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-. \$/+%" }) "Use letters A-Z, digits and - . space \$ / + % only." else null
            Type.EAN_13 -> digitsProblem(text, listOf(12, 13), "12 or 13")
            Type.EAN_8 -> digitsProblem(text, listOf(7, 8), "7 or 8")
            Type.UPC_A -> digitsProblem(text, listOf(11, 12), "11 or 12")
            Type.ITF ->
                if (!text.all { it.isDigit() }) "Digits only." else if (text.length % 2 != 0) "ITF needs an even number of digits." else null
        }
    }

    private fun digitsProblem(text: String, lengths: List<Int>, label: String): String? =
        if (!text.all { it in '0'..'9' }) "Digits only." else if (text.length !in lengths) "Enter $label digits (you have ${text.length})." else null

    fun matrix(type: Type, text: String, width: Int = 900, height: Int = 320): BitMatrix {
        val content = if (type == Type.CODE_39) text.uppercase() else text
        validate(type, content)?.let { throw BarcodeException(it) }
        return try {
            MultiFormatWriter().encode(content, type.format, width, height, mapOf(EncodeHintType.MARGIN to 20))
        } catch (e: WriterException) {
            throw BarcodeException("This content cannot be encoded as ${type.label}.")
        } catch (e: IllegalArgumentException) {
            throw BarcodeException(e.message?.takeIf { it.contains("checksum", ignoreCase = true) }?.let { "The check digit is wrong." }
                ?: "This content cannot be encoded as ${type.label}.")
        }
    }

    fun bitmap(type: Type, text: String): Bitmap {
        val m = matrix(type, text)
        val w = m.width
        val h = m.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) pixels[y * w + x] = if (m.get(x, y)) Color.BLACK else Color.WHITE
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
