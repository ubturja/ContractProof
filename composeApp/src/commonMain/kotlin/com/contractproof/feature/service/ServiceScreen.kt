package com.contractproof.feature.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

@Composable
fun ServiceScreen(
    onOpenDashboard: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Service history", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
            ) {
                Text(
                    text = "No completed services yet",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Completed work appears on the owner dashboard. Open a job from Today or the dashboard to capture evidence.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                CpButton(
                    label = "Open dashboard",
                    onClick = onOpenDashboard,
                    style = CpButtonStyle.Secondary,
                )
            }
        }
    }
}
