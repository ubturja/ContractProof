package com.contractproof.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory

private const val PREVIEW_MAX_EDGE = 1024

internal fun decodePreviewBitmap(sourcePath: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(sourcePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }
    val sampleSize = calculatePreviewSampleSize(bounds.outWidth, bounds.outHeight, PREVIEW_MAX_EDGE)
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = BitmapFactory.decodeFile(sourcePath, decodeOptions) ?: return null
    return scaleDownPreview(decoded, PREVIEW_MAX_EDGE)
}

private fun calculatePreviewSampleSize(width: Int, height: Int, maxEdge: Int): Int {
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

private fun scaleDownPreview(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val largest = maxOf(width, height)
    if (largest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / largest
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    if (scaled != bitmap) {
        bitmap.recycle()
    }
    return scaled
}
