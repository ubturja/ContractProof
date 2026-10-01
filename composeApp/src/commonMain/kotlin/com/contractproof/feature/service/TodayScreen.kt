package com.contractproof.feature.service

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
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpCoverageProgress
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpenJob: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TodayTestTags.screen),
        ) {
            CpTitleBar(
                title = "Today",
                actions = {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag(TodayTestTags.settings),
                    ) {
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
                    state.loading && state.jobs.isEmpty() -> {
                        CpLabeledProgress(label = "Loading today's jobs")
                    }
                    state.jobs.isEmpty() && state.banner != null && !state.loading -> {
                        CpErrorState(
                            message = state.banner,
                            retryLabel = "Retry",
                            onRetry = onRetry,
                            modifier = Modifier.testTag(TodayTestTags.retry),
                        )
                    }
                    state.jobs.isEmpty() && !state.loading -> {
                        Text(
                            text = "No job assigned today.",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag(TodayTestTags.empty),
                        )
                    }
                    else -> {
                        state.jobs.forEach { job ->
                            TodayJobCard(
                                job = job,
                                onOpenJob = { onOpenJob(job.jobId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayJobCard(
    job: TodayJobCardUi,
    onOpenJob: () -> Unit,
) {
    CpCard(
        modifier = Modifier.testTag(TodayTestTags.jobCard(job.jobId)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            Text(text = job.locationName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = job.clientName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = job.serviceTimeLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                text = job.statusLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
            CpCoverageProgress(percent = job.coveragePercent)
            CpButton(
                label = "Open job",
                onClick = onOpenJob,
                modifier = Modifier.testTag(TodayTestTags.openJob(job.jobId)),
            )
        }
    }
}
