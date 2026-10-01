package com.contractproof.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class CapturedPhoto(
    val localPath: String,
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg",
)

sealed interface EvidencePhotoCaptureState {
    data object Initializing : EvidencePhotoCaptureState

    data object LivePreview : EvidencePhotoCaptureState

    data class FrozenPreview(val photo: CapturedPhoto) : EvidencePhotoCaptureState

    data class Error(val message: String, val canRetry: Boolean) : EvidencePhotoCaptureState

    data object PermissionDenied : EvidencePhotoCaptureState
}

@Composable
expect fun EvidencePhotoCapture(
    modifier: Modifier = Modifier,
    shutterEnabled: Boolean,
    onStateChanged: (EvidencePhotoCaptureState) -> Unit,
    onPhotoConfirmed: (CapturedPhoto) -> Unit,
    onCancel: () -> Unit,
)
