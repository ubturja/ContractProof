package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun EvidenceCard(
    requirement: String,
    status: CpWorkStatus,
    modifier: Modifier = Modifier,
) {
    CpCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = requirement
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            Text(text = requirement, style = MaterialTheme.typography.titleMedium)
            CpStatusIndicator(status = status)
        }
    }
}
