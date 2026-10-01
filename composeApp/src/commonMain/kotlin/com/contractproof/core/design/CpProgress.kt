package com.contractproof.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo

@Composable
fun CpCoverageProgress(
    percent: Int,
    modifier: Modifier = Modifier,
    title: String = "Evidence coverage",
) {
    val bounded = percent.coerceIn(0, 100)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.xs),
    ) {
        Text(
            text = "$title $bounded%",
            style = MaterialTheme.typography.titleMedium,
        )
        LinearProgressIndicator(
            progress = { bounded / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = bounded / 100f,
                        range = 0f..1f,
                    )
                    contentDescription = "$title $bounded percent"
                },
        )
    }
}

@Composable
fun CpLabeledProgress(
    label: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.xs),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = label
                    },
            )
        } else {
            val bounded = progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { bounded },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = bounded,
                            range = 0f..1f,
                        )
                        contentDescription = label
                    },
            )
        }
    }
}
