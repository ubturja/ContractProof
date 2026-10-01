package com.contractproof.core.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun EvidenceCapturePreview(
    localPath: String,
    modifier: Modifier,
) {
    val bitmap = remember(localPath) {
        decodePreviewBitmap(localPath)
    }
    DisposableEffect(bitmap) {
        onDispose {
            bitmap?.recycle()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured evidence preview",
            modifier = modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
    }
}
