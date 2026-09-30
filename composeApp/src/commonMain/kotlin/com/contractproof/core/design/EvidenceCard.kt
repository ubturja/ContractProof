package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EvidenceCard(
    requirement: String,
    status: CpWorkStatus,
    modifier: Modifier = Modifier,
) {
    CpCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            Text(text = requirement, style = MaterialTheme.typography.titleMedium)
            CpStatusIndicator(status = status)
        }
    }
}
