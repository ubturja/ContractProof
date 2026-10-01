package com.contractproof.feature.dashboard

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
import androidx.compose.ui.platform.testTag
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpCoverageProgress
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.feature.service.TodayJobCardUi

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    hasLocations: Boolean,
    canAddLocation: Boolean,
    canOpenLocations: Boolean,
    canOpenContracts: Boolean,
    canOpenDisputes: Boolean,
    onOpenJob: (String) -> Unit,
    onOpenDispute: (String) -> Unit,
    onOpenLocations: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenDisputes: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(DashboardTestTags.screen),
        ) {
            CpTitleBar(
                title = "Dashboard",
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(text = "Settings", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
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
                    state.loading && state.todayJobs.isEmpty() && state.banner == null -> {
                        CpLabeledProgress(label = "Loading dashboard")
                    }
                    state.banner != null && state.todayJobs.isEmpty() && !state.loading -> {
                        CpErrorState(
                            message = state.banner,
                            retryLabel = "Retry",
                            onRetry = onRetry,
                            modifier = Modifier.testTag(DashboardTestTags.retry),
                        )
                    }
                    else -> {
                        if (!hasLocations) {
                            CpCard(modifier = Modifier.testTag(DashboardTestTags.noLocations)) {
                                Text(text = "No locations yet.", style = MaterialTheme.typography.bodyLarge)
                                if (canAddLocation) {
                                    Text(
                                        text = "Add a location to start scheduling work.",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                if (canOpenLocations) {
                                    CpButton(label = "Open locations", onClick = onOpenLocations)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
                            Text(
                                text = state.attentionLine,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag(DashboardTestTags.hero),
                            )
                            if (!state.loading) {
                                TextButton(
                                    onClick = onRefresh,
                                    modifier = Modifier.testTag(DashboardTestTags.refresh),
                                ) {
                                    Text(text = "Refresh", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        DashboardSection(
                            title = "Today's services",
                            testTag = DashboardTestTags.sectionToday,
                        ) {
                            if (state.jobsEmpty && hasLocations) {
                                Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
                                    Text(
                                        text = "Nothing scheduled today.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.testTag(DashboardTestTags.jobsEmpty),
                                    )
                                    if (canOpenContracts) {
                                        TextButton(
                                            onClick = onOpenContracts,
                                            modifier = Modifier.testTag(DashboardTestTags.openContracts),
                                        ) {
                                            Text(text = "View contracts")
                                        }
                                    }
                                }
                            } else {
                                state.todayJobs.forEach { job ->
                                    DashboardJobRow(job = job, onOpenJob = { onOpenJob(job.jobId) })
                                }
                            }
                        }
                        DashboardSection(
                            title = "Evidence gaps",
                            testTag = DashboardTestTags.sectionGaps,
                        ) {
                            if (state.evidenceGaps.isEmpty()) {
                                Text(text = "No evidence gaps for today's services.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                state.evidenceGaps.forEach { gap ->
                                    Text(
                                        text = "${gap.locationName} · ${gap.coveragePercent}% coverage",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.clickable { onOpenJob(gap.jobId) },
                                    )
                                }
                            }
                        }
                        DashboardSection(
                            title = "Open disputes",
                            testTag = DashboardTestTags.sectionDisputes,
                        ) {
                            if (state.openDisputes.isEmpty()) {
                                Text(text = "No open disputes.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                state.openDisputes.forEach { row ->
                                    Text(
                                        text = "${row.clientName} · ${row.locationName} · ${row.serviceDate}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .testTag(DashboardTestTags.disputeRow(row.disputeId))
                                            .clickable { onOpenDispute(row.disputeId) },
                                    )
                                }
                            }
                        }
                        DashboardSection(title = "Today's completion") {
                            Text(text = state.completionLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                        DashboardSection(title = "Coverage snapshot") {
                            Text(text = state.coverageLine, style = MaterialTheme.typography.bodyLarge)
                        }
                        DashboardSection(title = "Recent activity") {
                            if (state.recentActivity.isEmpty()) {
                                Text(text = "No recent activity yet.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                state.recentActivity.forEach { item ->
                                    Text(text = item.label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
                            if (canOpenContracts) {
                                CpButton(
                                    label = "Open contracts",
                                    onClick = onOpenContracts,
                                    style = CpButtonStyle.Secondary,
                                )
                            }
                            if (canOpenDisputes) {
                                CpButton(
                                    label = "Open disputes",
                                    onClick = onOpenDisputes,
                                    style = CpButtonStyle.Secondary,
                                )
                            }
                            if (canOpenLocations) {
                                CpButton(
                                    label = "Open locations",
                                    onClick = onOpenLocations,
                                    style = CpButtonStyle.Secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    testTag: String? = null,
    content: @Composable () -> Unit,
) {
    CpCard(modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun DashboardJobRow(
    job: TodayJobCardUi,
    onOpenJob: () -> Unit,
) {
    Column(
        modifier = Modifier
            .testTag(DashboardTestTags.jobCard(job.jobId))
            .clickable(onClick = onOpenJob)
            .padding(vertical = CpSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.xs),
    ) {
        Text(text = job.locationName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${job.clientName} · ${job.serviceTimeLabel} · ${job.statusLabel}",
            style = MaterialTheme.typography.bodyLarge,
        )
        CpCoverageProgress(percent = job.coveragePercent)
    }
}
