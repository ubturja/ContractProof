package com.contractproof.core.platform

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun EvidenceCapturePreview(
    localPath: String,
    modifier: Modifier,
) {
    Text(
        text = "Photo saved on this device",
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
    )
}
