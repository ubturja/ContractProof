package com.contractproof.feature.dispute

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
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.Dispute

@Composable
fun DisputesScreen(
    state: DisputesUiState,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Disputes", onBack = onBack)
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
                if (state.canWrite) {
                    CpButton(label = "File dispute", onClick = onCreate)
                }
                if (state.loading && state.items.isEmpty()) {
                    CpLabeledProgress(label = "Loading disputes")
                } else if (state.items.isEmpty() && state.banner != null && !state.loading) {
                    CpErrorState(message = state.banner, retryLabel = "Retry", onRetry = onRetry)
                } else if (state.items.isEmpty()) {
                    Text(text = "No disputes filed yet.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    state.items.forEach { dispute ->
                        DisputeRow(dispute = dispute, onOpen = { onOpen(dispute.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DisputeRow(dispute: Dispute, onOpen: () -> Unit) {
    CpCard(
        modifier = Modifier.clickable(onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(CpSpacing.md), verticalArrangement = Arrangement.spacedBy(CpSpacing.xs)) {
            Text(
                text = "${dispute.clientName} · ${dispute.locationName}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Service date ${dispute.serviceDate}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = dispute.complaint.take(120) + if (dispute.complaint.length > 120) "…" else "",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
