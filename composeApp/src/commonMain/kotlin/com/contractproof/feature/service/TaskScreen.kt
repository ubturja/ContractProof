package com.contractproof.feature.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpStatusIndicator
import com.contractproof.core.design.CpTitleBar
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.core.platform.EvidenceCapturePreview
import com.contractproof.domain.TaskNextAction

@Composable
fun TaskScreen(
    state: TaskUiState,
    onPrimaryAction: () -> Unit,
    onReportException: () -> Unit,
    onRetryUpload: () -> Unit,
    onRetakePhoto: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TaskTestTags.screen),
        ) {
            CpTitleBar(title = "Task", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
            ) {
                Text(
                    text = state.requirementText,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag(TaskTestTags.requirement),
                )
                Text(text = state.evidenceHint, style = MaterialTheme.typography.bodyLarge)
                if (state.mandatoryLabel != null) {
                    Text(
                        text = state.mandatoryLabel,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag(TaskTestTags.mandatory),
                    )
                }
                CpStatusIndicator(status = state.workStatus)
                if (state.capturedLocalPath != null) {
                    EvidenceCapturePreview(
                        localPath = state.capturedLocalPath,
                        modifier = Modifier.testTag(TaskTestTags.capturedPreview),
                    )
                    Text(
                        text = "Saved on this device",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (state.workStatus == CpWorkStatus.Uploading || state.workStatus == CpWorkStatus.Retrying) {
                    val progress = state.uploadPercent?.let { it / 100f }
                    CpLabeledProgress(
                        label = if (state.workStatus == CpWorkStatus.Retrying) {
                            "Retrying upload"
                        } else {
                            "Uploading photo"
                        },
                        progress = progress,
                    )
                }
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                val primaryLabel = when (state.primaryAction) {
                    TaskNextAction.CapturePhoto -> "Take photo"
                    TaskNextAction.MarkDone -> "Mark done"
                    else -> "Open task"
                }
                if (state.primaryAction == TaskNextAction.CapturePhoto || state.primaryAction == TaskNextAction.MarkDone) {
                    CpButton(
                        label = if (state.saving) "Saving" else primaryLabel,
                        onClick = onPrimaryAction,
                        enabled = !state.saving,
                        modifier = Modifier.testTag(TaskTestTags.takePhoto),
                    )
                }
                if (state.canRetakePhoto) {
                    CpButton(
                        label = "Retake photo",
                        onClick = onRetakePhoto,
                        style = CpButtonStyle.Secondary,
                        enabled = !state.saving,
                        modifier = Modifier.testTag(TaskTestTags.retakePhoto),
                    )
                }
                if (state.canRetryUpload) {
                    CpButton(
                        label = "Retry upload",
                        onClick = onRetryUpload,
                        style = CpButtonStyle.Secondary,
                        enabled = !state.saving,
                        modifier = Modifier.testTag(TaskTestTags.retryUpload),
                    )
                }
                if (state.canReportException) {
                    CpButton(
                        label = "Report exception",
                        onClick = onReportException,
                        style = CpButtonStyle.Secondary,
                        enabled = !state.saving,
                    )
                }
            }
        }
    }
}
