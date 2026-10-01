package com.contractproof.feature.dispute

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.DisputeAiSummary
import com.contractproof.domain.DisputeReconstructionBundle

@Composable
fun DisputeDetailScreen(
    state: DisputeDetailUiState,
    onRetry: () -> Unit,
    onGenerateSummary: () -> Unit,
    onOpenEvidenceReport: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(DisputeDetailTestTags.Screen),
        ) {
            CpTitleBar(title = "Dispute record", onBack = onBack)
            when {
                state.loading -> CpLabeledProgress(label = "Loading dispute", modifier = Modifier.padding(CpSpacing.md))
                state.bundle == null && state.banner != null -> {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                        modifier = Modifier.padding(CpSpacing.md),
                    )
                }
                state.bundle != null -> {
                    DisputeDetailContent(
                        bundle = state.bundle,
                        aiSummary = state.aiSummary,
                        summaryLoading = state.summaryLoading,
                        canGenerateSummary = state.canWrite || state.aiSummary == null,
                        onGenerateSummary = onGenerateSummary,
                        canOpenReport = state.canOpenReport,
                        onOpenEvidenceReport = onOpenEvidenceReport,
                        banner = state.banner,
                    )
                }
            }
        }
    }
}

@Composable
private fun DisputeDetailContent(
    bundle: DisputeReconstructionBundle,
    aiSummary: DisputeAiSummary?,
    summaryLoading: Boolean,
    canGenerateSummary: Boolean,
    onGenerateSummary: () -> Unit,
    canOpenReport: Boolean,
    onOpenEvidenceReport: () -> Unit,
    banner: String?,
) {
    val job = bundle.job
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CpSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
    ) {
        if (banner != null) {
            Text(text = banner, style = MaterialTheme.typography.bodyLarge)
        }
        CpCard {
            Column(modifier = Modifier.padding(CpSpacing.md), verticalArrangement = Arrangement.spacedBy(CpSpacing.xs)) {
                Text(text = "Recorded facts", style = MaterialTheme.typography.titleMedium)
                Text(text = "Contract: ${bundle.contract.contractTitle} (${bundle.contract.versionLabel})")
                Text(text = "Disputed requirement: ${bundle.disputedRequirementText}")
                Text(text = "Client: ${bundle.dispute.clientName} · ${bundle.dispute.locationName}")
                Text(text = "Service date: ${bundle.dispute.serviceDate}")
                Text(text = "Worker: ${job.assignee?.displayName ?: "Not recorded"}")
                Text(text = "Started: ${job.startedAt ?: "Not recorded"}")
                Text(text = "Completed: ${job.completedAt ?: "Not recorded"}")
                Text(text = "Allegation: ${bundle.dispute.complaint}")
            }
        }
        Text(text = "Timeline", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag(DisputeDetailTestTags.Timeline))
        bundle.timeline.forEach { event ->
            CpCard {
                Column(modifier = Modifier.padding(CpSpacing.md)) {
                    Text(text = event.title, style = MaterialTheme.typography.titleSmall)
                    Text(text = event.occurredAt, style = MaterialTheme.typography.labelMedium)
                    Text(text = event.body, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (bundle.missingRequirementLabels.isNotEmpty()) {
            Text(text = "Recorded gaps", style = MaterialTheme.typography.titleMedium)
            bundle.missingRequirementLabels.forEach { label ->
                Text(text = "• $label", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (canOpenReport) {
            CpButton(label = "Evidence report", onClick = onOpenEvidenceReport)
        }
        HorizontalDivider()
        Text(text = "AI-assisted summary", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Generated from recorded data. Not a legal determination.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (summaryLoading) {
            CpLabeledProgress(label = "Generating summary")
        } else if (aiSummary != null) {
            AiSummaryCard(summary = aiSummary)
        } else if (canGenerateSummary) {
            CpButton(
                label = "Generate summary",
                onClick = onGenerateSummary,
                modifier = Modifier.testTag(DisputeDetailTestTags.GenerateSummary),
            )
        }
    }
}

@Composable
private fun AiSummaryCard(summary: DisputeAiSummary) {
    CpCard(modifier = Modifier.testTag(DisputeDetailTestTags.AiSummary)) {
        Column(modifier = Modifier.padding(CpSpacing.md), verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            SummarySection(title = "Allegation", lines = listOf(summary.allegation))
            if (summary.requirements.isNotEmpty()) {
                SummarySection(
                    title = "Contractual requirement",
                    lines = summary.requirements.map { "${it.requirementText} ${it.contractualContext}".trim() },
                )
            }
            SummarySection(title = "Recorded evidence", lines = summary.recordedEvidence)
            SummarySection(title = "Missing evidence", lines = summary.missingEvidence)
            SummarySection(title = "Exceptions", lines = summary.exceptions)
            if (summary.neutralOverview.isNotBlank()) {
                SummarySection(title = "Overview", lines = listOf(summary.neutralOverview))
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    lines.forEach { line ->
        Text(text = line, style = MaterialTheme.typography.bodyLarge)
    }
}
