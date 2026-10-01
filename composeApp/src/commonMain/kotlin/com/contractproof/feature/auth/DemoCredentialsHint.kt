package com.contractproof.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpSpacing
import com.contractproof.demo.clearLineDemoCredentials

@Composable
fun DemoCredentialsHint(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CpSpacing.xs)) {
        Text(
            text = "Hackathon demo accounts",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Password is in docs/development/demo-data.md (not stored in the app).",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        clearLineDemoCredentials.forEach { line ->
            Text(
                text = "${line.role}: ${line.email}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
