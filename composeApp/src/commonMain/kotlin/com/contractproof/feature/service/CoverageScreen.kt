package com.contractproof.feature.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpCoverageProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

@Composable
fun CoverageScreen(
    state: CoverageUiState,
    onOpenTask: (String) -> Unit,
    onRetryUpload: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(CoverageTestTags.screen),
        ) {
            CpTitleBar(title = state.title, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                CpCoverageProgress(
                    percent = state.coveragePercent,
                    title = state.title,
                    modifier = Modifier.testTag(CoverageTestTags.percent),
                )
                Text(
                    text = state.summaryLine,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(CoverageTestTags.summary),
                )
                if (state.missingLines.isNotEmpty()) {
                    Text(text = "Missing:", style = MaterialTheme.typography.titleMedium)
                    state.missingLines.forEach { line ->
                        Text(
                            text = "· $line",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(CoverageTestTags.missingLine),
                        )
                    }
                }
                if (state.exceptionLines.isNotEmpty()) {
                    Text(text = "Exceptions:", style = MaterialTheme.typography.titleMedium)
                    state.exceptionLines.forEach { line ->
                        Text(text = "· $line", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (state.uploadPendingLines.isNotEmpty()) {
                    Text(text = "Upload pending:", style = MaterialTheme.typography.titleMedium)
                    state.uploadPendingLines.forEach { line ->
                        Text(text = "· $line", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (state.uploadFailedLines.isNotEmpty()) {
                    Text(text = "Upload failed:", style = MaterialTheme.typography.titleMedium)
                    state.uploadFailedLines.forEach { line ->
                        Text(text = "· $line", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                val (primaryLabel, primaryClick) = when (val action = state.primaryAction) {
                    is CoveragePrimaryAction.OpenTask -> action.label to { onOpenTask(action.requirementId) }
                    is CoveragePrimaryAction.RetryUpload -> action.label to { onRetryUpload(action.requirementId) }
                    CoveragePrimaryAction.FinishService -> "Finish service" to onFinish
                    CoveragePrimaryAction.BackToJob -> "Back to job" to onBack
                }
                CpButton(
                    label = primaryLabel,
                    onClick = primaryClick,
                    modifier = Modifier.testTag(CoverageTestTags.primaryAction),
                )
                if (state.primaryAction !is CoveragePrimaryAction.BackToJob) {
                    CpButton(
                        label = "Back",
                        onClick = onBack,
                        style = CpButtonStyle.Secondary,
                    )
                }
            }
        }
    }
}
