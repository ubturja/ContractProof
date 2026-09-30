package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TaskCard(
    requirement: String,
    nextAction: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CpCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.md)) {
            Text(text = requirement, style = MaterialTheme.typography.titleMedium)
            CpButton(label = nextAction, onClick = onAction)
        }
    }
}
