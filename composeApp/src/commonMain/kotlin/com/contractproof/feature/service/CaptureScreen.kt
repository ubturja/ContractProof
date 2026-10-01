package com.contractproof.feature.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.contractproof.core.design.CpConfirmDialog
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.core.platform.EvidencePhotoCapture
import com.contractproof.core.platform.EvidencePhotoCaptureState

@Composable
fun CaptureScreen(
    requirementText: String,
    mandatoryLabel: String?,
    saving: Boolean,
    banner: String?,
    onPhotoConfirmed: (CapturedPhoto) -> Unit,
    onBack: () -> Unit,
    captureContent: (@Composable (onPhotoConfirmed: (CapturedPhoto) -> Unit) -> Unit)? = null,
) {
    var captureState by remember { mutableStateOf<EvidencePhotoCaptureState>(EvidencePhotoCaptureState.Initializing) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    fun handleBack() {
        if (captureState is EvidencePhotoCaptureState.FrozenPreview) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(CaptureTestTags.screen),
        ) {
            CpTitleBar(title = "Capture", onBack = { handleBack() })
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CpSpacing.md, vertical = CpSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.sm),
            ) {
                Text(
                    text = requirementText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(CaptureTestTags.requirement),
                )
                if (mandatoryLabel != null) {
                    Text(text = mandatoryLabel, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = "This photo applies only to this requirement.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (banner != null) {
                    Text(text = banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (saving) {
                    Text(text = "Saving photo…", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (captureContent != null) {
                captureContent(onPhotoConfirmed)
            } else {
                EvidencePhotoCapture(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = CpSpacing.md)
                        .padding(bottom = CpSpacing.md),
                    shutterEnabled = !saving,
                    onStateChanged = { captureState = it },
                    onPhotoConfirmed = onPhotoConfirmed,
                    onCancel = { handleBack() },
                )
            }
        }
    }

    if (showDiscardDialog) {
        CpConfirmDialog(
            title = "Discard photo?",
            body = "The photo you took will not be saved for this requirement.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}
