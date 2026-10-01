package com.contractproof.feature.service

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.ExceptionRules

@Composable
fun ExceptionScreen(
    requirementText: String,
    state: ExceptionUiState,
    onReasonSelect: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Exception", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                Text(text = requirementText, style = MaterialTheme.typography.titleLarge)
                Text(text = "Choose a reason", style = MaterialTheme.typography.titleMedium)
                ExceptionRules.reasons.forEach { reason ->
                    CpCard(
                        modifier = Modifier
                            .clickable { onReasonSelect(reason) }
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = reason
                            },
                    ) {
                        val selected = state.selectedReason == reason
                        Text(
                            text = if (selected) "$reason (selected)" else reason,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                CpTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = "Note (optional)",
                    singleLine = false,
                )
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.saving) {
                    CpLabeledProgress(label = "Saving exception")
                }
                CpButton(
                    label = if (state.saving) "Saving exception" else "Save exception",
                    onClick = onSave,
                    enabled = state.selectedReason.isNotBlank() && !state.saving,
                )
            }
        }
    }
}
