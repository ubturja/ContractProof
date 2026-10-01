package com.contractproof.feature.service

import androidx.compose.foundation.clickable
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
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.core.design.EvidenceCard

@Composable
fun JobScreen(
    state: JobUiState,
    onStartService: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenCoverage: () -> Unit,
    onFinishService: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(JobTestTags.screen),
        ) {
            CpTitleBar(title = "Job", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
            ) {
                Text(text = state.locationName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = state.clientName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = state.statusLabel, style = MaterialTheme.typography.titleMedium)
                state.startedAtLabel?.let {
                    Text(text = "Started $it", style = MaterialTheme.typography.bodyLarge)
                }
                state.completedAtLabel?.let {
                    Text(text = "Completed $it", style = MaterialTheme.typography.bodyLarge)
                }
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.finishMessage != null) {
                    Text(
                        text = state.finishMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(JobTestTags.finishMessage),
                    )
                }
                if (state.loading) {
                    CpLabeledProgress(label = "Loading job")
                } else {
                    CpCoverageProgress(percent = state.coveragePercent)
                    if (state.canStart) {
                        CpButton(
                            label = if (state.saving) "Starting service" else "Start service",
                            onClick = onStartService,
                            enabled = !state.saving,
                        )
                    }
                    state.tasks.forEach { task ->
                        EvidenceCard(
                            requirement = task.text,
                            status = task.workStatus,
                            modifier = Modifier
                                .testTag("${JobTestTags.taskCard}_${task.id}")
                                .clickable { onOpenTask(task.id) },
                        )
                    }
                    CpButton(
                        label = "Coverage",
                        onClick = onOpenCoverage,
                        style = CpButtonStyle.Secondary,
                    )
                    CpButton(
                        label = if (state.saving) "Finishing service" else "Finish service",
                        onClick = onFinishService,
                        enabled = state.canFinish && !state.saving,
                    )
                }
            }
        }
    }
}
