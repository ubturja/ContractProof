package com.contractproof.feature.report

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
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.DisputeOutcome
import com.contractproof.domain.ServiceEvidenceReport

@Composable
fun ReportPreviewScreen(
    state: ReportPreviewUiState,
    onGenerate: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onReload: () -> Unit,
    onOpenPlans: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ReportPreviewTestTags.Screen),
        ) {
            CpTitleBar(title = "Evidence report", onBack = onBack)
            when {
                state.loading -> CpLabeledProgress(
                    label = "Building the evidence report from the service record.",
                    modifier = Modifier.padding(CpSpacing.md),
                )
                state.preview == null && state.banner != null -> {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onReload,
                        modifier = Modifier.padding(CpSpacing.md),
                    )
                }
                else -> {
                    ReportPreviewContent(
                        state = state,
                        preview = state.preview,
                        onGenerate = onGenerate,
                        onRetry = onRetry,
                        onOpen = onOpen,
                        onShare = onShare,
                        onOpenPlans = onOpenPlans,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportPreviewContent(
    state: ReportPreviewUiState,
    preview: ServiceEvidenceReport?,
    onGenerate: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CpSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
    ) {
        if (state.banner != null) {
            Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
        }
        if (state.isGenerating) {
            CpLabeledProgress(label = "Generating PDF report…")
        }
        state.report?.failureReason?.let { reason ->
            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
        }
        if (preview != null) {
            ReportSectionCard(title = "Summary") {
                Text(text = "Client: ${preview.header.clientName}")
                Text(text = "Location: ${preview.header.locationName}")
                Text(text = "Service date: ${preview.header.serviceDate}")
                Text(text = "Disputed requirement: ${preview.header.disputedRequirementText}")
            }
            ReportSectionCard(title = "1. Contract requirement") {
                preview.contractRequirements.forEach { line ->
                    val tag = if (line.isDisputed) " (disputed)" else ""
                    Text(text = "• ${line.requirementText}$tag")
                }
            }
            ReportSectionCard(title = "2. Scheduled service") {
                Text(text = "${preview.scheduledService.scheduledStart} – ${preview.scheduledService.scheduledEnd}")
            }
            ReportSectionCard(title = "3. Assigned personnel") {
                Text(text = preview.assignedPersonnel.displayName)
            }
            ReportSectionCard(title = "4. Service timeline") {
                preview.timeline.forEach { event ->
                    Text(text = "${event.occurredAt} — ${event.title}")
                    Text(text = event.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
            ReportSectionCard(title = "5. Evidence") {
                if (preview.evidence.isEmpty()) {
                    Text(text = "No uploaded evidence recorded.")
                } else {
                    preview.evidence.forEach { line ->
                        Text(text = "${line.requirementText} (${line.evidenceType}) at ${line.capturedAt}")
                    }
                }
            }
            ReportSectionCard(title = "6. Exceptions") {
                if (preview.exceptions.isEmpty()) {
                    Text(text = "No exceptions recorded.")
                } else {
                    preview.exceptions.forEach { line ->
                        Text(text = "${line.requirementText}: ${line.reason}")
                    }
                }
            }
            ReportSectionCard(title = "7. Client acknowledgement") {
                preview.acknowledgements.forEach { line ->
                    Text(text = "${line.label} at ${line.occurredAt}")
                }
            }
            ReportSectionCard(title = "8. Evidence coverage") {
                preview.coverage.forEach { line ->
                    val label = when (line.outcome) {
                        DisputeOutcome.Missing -> "Missing"
                        DisputeOutcome.Satisfied -> "Satisfied"
                        DisputeOutcome.Exception -> "Exception"
                    }
                    Text(text = "${line.requirementText}: $label")
                }
            }
            ReportSectionCard(title = "9. Attachments") {
                if (preview.attachments.isEmpty()) {
                    Text(text = "No attachments recorded.")
                } else {
                    preview.attachments.forEach { line ->
                        Text(text = "${line.label}: ${line.objectPath}")
                    }
                }
            }
        }
        if (state.needsUpgrade) {
            CpButton(
                label = "Open plans to generate report",
                onClick = onOpenPlans,
            )
        }
        if (state.canGenerate && state.report == null && !state.isGenerating) {
            CpButton(
                label = "Generate report",
                onClick = onGenerate,
                modifier = Modifier.testTag(ReportPreviewTestTags.Generate),
            )
        }
        if (state.canRetry) {
            CpButton(label = "Retry generation", onClick = onRetry)
        }
        if (state.canOpen) {
            CpButton(
                label = "Open report",
                onClick = onOpen,
                modifier = Modifier.testTag(ReportPreviewTestTags.Open),
            )
            CpButton(
                label = "Share report link",
                onClick = onShare,
                style = CpButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun ReportSectionCard(title: String, content: @Composable () -> Unit) {
    CpCard {
        Column(
            modifier = Modifier.padding(CpSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CpSpacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}
