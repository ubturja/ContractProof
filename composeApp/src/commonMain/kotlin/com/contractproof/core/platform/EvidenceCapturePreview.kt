package com.contractproof.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun EvidenceCapturePreview(
    localPath: String,
    modifier: Modifier = Modifier,
)
