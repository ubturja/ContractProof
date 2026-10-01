package com.contractproof.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.contractproof.core.requireApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val MAX_EDGE = 2048
private const val JPEG_QUALITY = 85

actual fun compressEvidencePhoto(sourcePath: String): CompressedEvidencePhoto {
    val context = requireApplicationContext()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(sourcePath, bounds)
    val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeFile(sourcePath, decodeOptions)
        ?: error("Could not read the photo.")
    val scaled = scaleDown(bitmap, MAX_EDGE)
    if (scaled != bitmap) {
        bitmap.recycle()
    }
    val bytes = ByteArrayOutputStream().use { stream ->
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        stream.toByteArray()
    }
    scaled.recycle()
    val file = File(context.cacheDir, "evidence-upload-${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { it.write(bytes) }
    return CompressedEvidencePhoto(localPath = file.absolutePath, bytes = bytes)
}

private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w > maxEdge * 2 || h > maxEdge * 2) {
        sample *= 2
        w /= 2
        h /= 2
    }
    return sample
}

private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val largest = maxOf(width, height)
    if (largest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / largest
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
