package com.omnitools.app.tools.qr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.omnitools.app.util.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Finds QR codes and barcodes in a picture the user chose. Runs fully on-device. */
object ImageBarcodeScanner {

    data class Found(val format: String, val text: String)

    sealed class Outcome {
        data class Codes(val codes: List<Found>) : Outcome()
        object NoCode : Outcome()
        object CouldNotRead : Outcome()
        data class Failed(val message: String) : Outcome()
    }

    /** Tries a large version first, then a smaller one: some pictures are detected better after downscaling. */
    suspend fun scan(context: Context, uri: Uri): Outcome = withContext(Dispatchers.Default) {
        var decodedAny = false
        for (side in intArrayOf(2400, 1200)) {
            val bmp = BitmapLoader.decode(context, uri, side) ?: continue
            decodedAny = true
            try {
                val codes = detect(bmp)
                if (codes.isNotEmpty()) return@withContext Outcome.Codes(codes)
            } catch (e: Exception) {
                return@withContext Outcome.Failed(e.message ?: "The scanner failed.")
            } finally {
                bmp.recycle()
            }
        }
        if (decodedAny) Outcome.NoCode else Outcome.CouldNotRead
    }

    private suspend fun detect(bitmap: Bitmap): List<Found> = suspendCancellableCoroutine { cont ->
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        )
        scanner.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { list ->
                scanner.close()
                val found = list.mapNotNull { b ->
                    b.rawValue?.takeIf { it.isNotEmpty() }?.let { Found(barcodeFormatName(b.format), it) }
                }.distinctBy { it.text }
                if (cont.isActive) cont.resume(found)
            }
            .addOnFailureListener {
                scanner.close()
                if (cont.isActive) cont.resumeWith(Result.failure(it))
            }
        cont.invokeOnCancellation { scanner.close() }
    }
}
