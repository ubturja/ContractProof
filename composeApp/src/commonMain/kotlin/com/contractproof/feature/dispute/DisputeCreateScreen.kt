package com.contractproof.feature.dispute

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
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.ServiceJobRequirement

@Composable
fun DisputeCreateScreen(
    state: DisputeCreateUiState,
    showServiceDateField: Boolean = true,
    onSelectJob: (String) -> Unit,
    onSelectRequirement: (String) -> Unit,
    onComplaintChange: (String) -> Unit,
    onServiceDateChange: (String) -> Unit,
    onPickAttachment: () -> Unit,
    onClearAttachment: () -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit = {},
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "File dispute", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.loading) {
                    CpLabeledProgress(label = "Loading jobs")
                } else if (state.jobs.isEmpty() && state.banner != null) {
                    CpErrorState(message = state.banner, retryLabel = "Retry", onRetry = onRetry)
                } else {
                    if (state.banner != null) {
                        Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(text = "Service job", style = MaterialTheme.typography.titleSmall)
                    state.jobs.forEach { job ->
                        val selected = job.id == state.selectedJobId
                        CpButton(
                            label = "${job.client.name} · ${job.location.name} (${job.serviceDate})",
                            onClick = { onSelectJob(job.id) },
                            enabled = !selected,
                        )
                    }
                    if (state.requirements.isNotEmpty()) {
                        Text(text = "Disputed requirement", style = MaterialTheme.typography.titleSmall)
                        state.requirements.forEach { requirement ->
                            RequirementPick(
                                requirement = requirement,
                                selected = requirement.id == state.selectedRequirementId,
                                onSelect = { onSelectRequirement(requirement.id) },
                            )
                        }
                    }
                    if (showServiceDateField) {
                        CpTextField(
                            value = state.serviceDate,
                            onValueChange = onServiceDateChange,
                            label = "Service date (YYYY-MM-DD)",
                        )
                    }
                    CpTextField(
                        value = state.complaint,
                        onValueChange = onComplaintChange,
                        label = "Issue description",
                        singleLine = false,
                    )
                    if (state.attachmentFileName != null) {
                        Text(text = "Attachment: ${state.attachmentFileName}", style = MaterialTheme.typography.bodyMedium)
                        CpButton(label = "Remove attachment", onClick = onClearAttachment)
                    } else {
                        CpButton(label = "Add attachment (optional)", onClick = onPickAttachment)
                    }
                    CpButton(
                        label = if (state.saving) "Submitting…" else "Submit dispute",
                        onClick = onSubmit,
                        enabled = state.canSubmit,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequirementPick(
    requirement: ServiceJobRequirement,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    CpButton(
        label = if (selected) "✓ ${requirement.requirementText}" else requirement.requirementText,
        onClick = onSelect,
        enabled = !selected,
    )
}
