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
import com.contractproof.core.design.CpCoverageProgress
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

@Composable
fun CompleteScreen(
    coveragePercent: Int,
    canFinish: Boolean,
    completionBlockerLabels: List<String>,
    saving: Boolean,
    banner: String?,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Complete", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
            ) {
                CpCoverageProgress(percent = coveragePercent)
                if (!canFinish) {
                    Text(
                        text = "Finish when every mandatory requirement has evidence, a pending upload, or a documented exception.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (completionBlockerLabels.isNotEmpty()) {
                        Text(text = "Still blocking finish:", style = MaterialTheme.typography.titleMedium)
                        completionBlockerLabels.forEach { item ->
                            Text(text = "· $item", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    CpButton(label = "Back to coverage", onClick = onBack, style = CpButtonStyle.Secondary)
                } else {
                    Text(
                        text = "Confirm that this service is complete.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (banner != null) {
                        Text(text = banner, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (saving) {
                        CpLabeledProgress(label = "Finishing service")
                    }
                    CpButton(
                        label = if (saving) "Finishing service" else "Finish service",
                        onClick = onFinish,
                        enabled = !saving,
                    )
                }
            }
        }
    }
}
