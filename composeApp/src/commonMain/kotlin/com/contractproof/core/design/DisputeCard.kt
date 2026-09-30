package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DisputeCard(
    client: String,
    location: String,
    date: String,
    complaint: String,
    modifier: Modifier = Modifier,
) {
    CpCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(CpSpacing.xs)) {
            Text(text = client, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$location · $date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = complaint, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
