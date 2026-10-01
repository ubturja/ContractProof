package com.contractproof.feature.clientportal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpCoverageProgress
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.ServiceRecordSnapshot

@Composable
fun ClientHomeScreen(
    state: ClientServiceListUiState,
    onOpenRecord: (String) -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(
                title = "Services",
                actions = {
                    TextButton(onClick = onSignOut) {
                        Text(text = "Sign out", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.organizationName.isNotEmpty()) {
                    Text(
                        text = state.organizationName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                when {
                    state.loading && state.items.isEmpty() -> {
                        CpLabeledProgress(label = "Loading service records")
                    }
                    state.items.isEmpty() && state.banner != null && !state.loading -> {
                        CpErrorState(message = state.banner, retryLabel = "Retry", onRetry = onRetry)
                    }
                    state.items.isEmpty() && !state.loading -> {
                        Text(text = "No completed services yet.", style = MaterialTheme.typography.titleLarge)
                    }
                    else -> {
                        state.items.forEach { item ->
                            CpCard(
                                modifier = Modifier
                                    .clickable { onOpenRecord(item.jobId) }
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        contentDescription =
                                            "Service at ${item.locationName} on ${item.serviceDate}"
                                    },
                            ) {
                                Text(text = item.locationName, style = MaterialTheme.typography.titleMedium)
                                Text(text = item.serviceDate, style = MaterialTheme.typography.bodyLarge)
                                CpCoverageProgress(percent = item.coveragePercent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientServiceRecordScreen(
    state: ClientServiceRecordUiState,
    onReportIssue: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Service record", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                when {
                    state.loading && state.record == null -> {
                        CpLabeledProgress(label = "Loading service record")
                    }
                    state.banner != null && state.record == null -> {
                        CpErrorState(message = state.banner, retryLabel = "Retry", onRetry = onRetry)
                    }
                    state.record != null -> {
                        val record = state.record
                        RecordSection(title = "Location") {
                            Text(text = record.locationName, style = MaterialTheme.typography.bodyLarge)
                        }
                        RecordSection(title = "Service date") {
                            Text(text = record.serviceDate, style = MaterialTheme.typography.bodyLarge)
                        }
                        RecordSection(title = "Tasks") {
                            record.completedTasks.forEach { task ->
                                val statusLabel = when (task.status) {
                                    JobRequirementStatus.Satisfied -> "Completed"
                                    JobRequirementStatus.Exception -> "Exception"
                                    JobRequirementStatus.Missing -> "Missing"
                                }
                                Text(
                                    text = "${task.requirementText} · $statusLabel",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            record.missingMandatoryLabels.forEach { label ->
                                Text(
                                    text = "$label · Missing",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        RecordSection(title = "Evidence") {
                            if (record.evidenceLines.isEmpty()) {
                                Text(text = "No evidence recorded.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                record.evidenceLines.forEach { line ->
                                    Text(
                                        text = "${line.label} · ${line.requirementText} · ${line.capturedAt}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        RecordSection(title = "Exceptions") {
                            if (record.exceptionLines.isEmpty()) {
                                Text(text = "No exceptions recorded.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                record.exceptionLines.forEach { line ->
                                    Text(
                                        text = "${line.requirementText} · ${line.reason} · ${line.recordedAt}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        RecordSection(title = "Acknowledgements") {
                            if (record.acknowledgements.isEmpty()) {
                                Text(text = "No checklist or timestamp acknowledgements.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                record.acknowledgements.forEach { ack ->
                                    Text(
                                        text = "${ack.label} · ${ack.detail} · ${ack.occurredAt}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        CpButton(label = "Report an issue", onClick = onReportIssue)
                    }
                }
            }
        }
    }
}

@Composable
fun ClientDisputeConfirmationScreen(
    onDone: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Dispute submitted")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                Text(
                    text = "Your dispute was received.",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "The service provider will review the service record and your description. You do not need to do anything else right now.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                CpButton(label = "Back to services", onClick = onDone)
            }
        }
    }
}

@Composable
private fun RecordSection(
    title: String,
    content: @Composable () -> Unit,
) {
    CpCard {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
