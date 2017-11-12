package org.ligi.passandroid.functions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.google.zxing.MultiFormatWriter
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.tracedroid.logging.Log
import java.nio.ByteBuffer


fun generateBitmapDrawable(resources: Resources, data: String, type: PassBarCodeFormat): BitmapDrawable? {
    val bitmap = generateBarCodeBitmap(data, type) ?: return null

    return BitmapDrawable(resources, bitmap).apply {
        isFilterBitmap = false
        setAntiAlias(false)
    }
}

fun generateBarCodeBitmap(data: String, type: PassBarCodeFormat): Bitmap? {

    if (data.isEmpty()) {
        return null
    }

    try {
        val matrix = getBitMatrix(data, type)
        val is1D = matrix.height == 1

        // generate an image from the byte matrix
        val width = matrix.width
        val height = if (is1D) width / 5 else matrix.height

        // create buffered image to draw to
        // NTFS Bitmap.Config.ALPHA_8 sounds like an awesome idea - been there - done that ..
        val barcodeImage = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

        // iterate through the matrix and draw the pixels to the image
        val black = 0xFF.toByte()
        val white = 0x0.toByte()
        val buffer = ByteBuffer.allocateDirect(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                buffer.put(if (matrix.get(x, if (is1D) 0 else y)) black else white)
            }
        }

        barcodeImage.copyPixelsFromBuffer(buffer.rewind())

        return barcodeImage
    } catch (e: com.google.zxing.WriterException) {
        Log.w("could not write image " + e)
        // TODO check if we should better return some rescue Image here
        return null
    } catch (e: IllegalArgumentException) {
        Log.w("could not write image " + e)
        return null
    } catch (e: ArrayIndexOutOfBoundsException) {
        // happens for ITF barcode on certain inputs
        Log.w("could not write image " + e)
        return null
    }

}

fun getBitMatrix(data: String, type: PassBarCodeFormat)
        = MultiFormatWriter().encode(data, type.zxingBarCodeFormat(), 0, 0)!!

