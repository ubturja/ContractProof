package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CpCoverageProgress(
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val bounded = percent.coerceIn(0, 100)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.xs),
    ) {
        Text(
            text = "Evidence coverage $bounded%",
            style = MaterialTheme.typography.titleMedium,
        )
        LinearProgressIndicator(
            progress = { bounded / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun CpLabeledProgress(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.xs),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
