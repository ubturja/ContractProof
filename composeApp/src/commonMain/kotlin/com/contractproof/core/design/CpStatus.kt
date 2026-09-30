package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class CpWorkStatus {
    Pending,
    Uploading,
    Uploaded,
    Failed,
    Retrying,
    Missing,
    Exception,
}

@Composable
fun CpStatusIndicator(
    status: CpWorkStatus,
    modifier: Modifier = Modifier,
) {
    val colors = statusColors(status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CpRadius.pill),
        color = colors.container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CpSpacing.sm, vertical = CpSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(CpSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.content,
            )
            Text(
                text = status.label(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.content,
            )
        }
    }
}

private data class StatusColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun statusColors(status: CpWorkStatus): StatusColors {
    return when (status) {
        CpWorkStatus.Pending,
        CpWorkStatus.Retrying,
        CpWorkStatus.Exception,
        -> StatusColors(CpStatusColors.warningContainer, CpStatusColors.warning)
        CpWorkStatus.Uploading -> StatusColors(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
        )
        CpWorkStatus.Uploaded -> StatusColors(CpStatusColors.successContainer, CpStatusColors.success)
        CpWorkStatus.Failed,
        CpWorkStatus.Missing,
        -> StatusColors(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
        )
    }
}

private fun CpWorkStatus.label(): String = when (this) {
    CpWorkStatus.Pending -> "Pending upload"
    CpWorkStatus.Uploading -> "Uploading"
    CpWorkStatus.Uploaded -> "Uploaded"
    CpWorkStatus.Failed -> "Upload failed"
    CpWorkStatus.Retrying -> "Retrying"
    CpWorkStatus.Missing -> "Missing"
    CpWorkStatus.Exception -> "Exception"
}

private fun CpWorkStatus.icon(): ImageVector = when (this) {
    CpWorkStatus.Pending -> Icons.Filled.Info
    CpWorkStatus.Uploading -> Icons.Filled.Refresh
    CpWorkStatus.Uploaded -> Icons.Filled.Check
    CpWorkStatus.Failed -> Icons.Filled.Close
    CpWorkStatus.Retrying -> Icons.Filled.Refresh
    CpWorkStatus.Missing -> Icons.Filled.Close
    CpWorkStatus.Exception -> Icons.Filled.Warning
}
