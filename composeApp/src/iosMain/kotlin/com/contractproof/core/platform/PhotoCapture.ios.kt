package com.contractproof.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpSpacing
import platform.UIKit.UIImagePickerControllerSourceType

@Composable
actual fun EvidencePhotoCapture(
    modifier: Modifier,
    shutterEnabled: Boolean,
    onStateChanged: (EvidencePhotoCaptureState) -> Unit,
    onPhotoConfirmed: (CapturedPhoto) -> Unit,
    onCancel: () -> Unit,
) {
    var pendingConfirm by remember { mutableStateOf<CapturedPhoto?>(null) }
    var uiState by remember {
        mutableStateOf<EvidencePhotoCaptureState>(EvidencePhotoCaptureState.LivePreview)
    }

    LaunchedEffect(uiState) {
        onStateChanged(uiState)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(CpSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (pendingConfirm == null) {
                    "Capture evidence with camera or photo library"
                } else {
                    "Photo ready — confirm to attach"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (pendingConfirm != null) {
            CpButton(
                label = "Use photo",
                onClick = {
                    pendingConfirm?.let(onPhotoConfirmed)
                    pendingConfirm = null
                },
            )
            CpButton(
                label = "Retake",
                onClick = {
                    pendingConfirm = null
                    uiState = EvidencePhotoCaptureState.LivePreview
                },
            )
        } else {
            CpButton(
                label = "Take photo",
                enabled = shutterEnabled,
                onClick = {
                    presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera) { photo ->
                        if (photo == null) {
                            uiState = EvidencePhotoCaptureState.Error(
                                message = "Camera unavailable or cancelled.",
                                canRetry = true,
                            )
                        } else {
                            pendingConfirm = photo
                            uiState = EvidencePhotoCaptureState.FrozenPreview(photo)
                        }
                    }
                },
            )
            CpButton(
                label = "Choose from library",
                enabled = shutterEnabled,
                onClick = {
                    presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary) { photo ->
                        if (photo == null) {
                            uiState = EvidencePhotoCaptureState.Error(
                                message = "Photo library unavailable or cancelled.",
                                canRetry = true,
                            )
                        } else {
                            pendingConfirm = photo
                            uiState = EvidencePhotoCaptureState.FrozenPreview(photo)
                        }
                    }
                },
            )
        }
        CpButton(label = "Cancel", onClick = onCancel)
    }
}
