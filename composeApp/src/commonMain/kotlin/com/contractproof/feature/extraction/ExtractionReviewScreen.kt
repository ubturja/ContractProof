package com.contractproof.feature.extraction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.ExtractionReviewRules
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.ReviewRequirementDraft

@Composable
fun ExtractionReviewScreen(
    state: ExtractionReviewUiState,
    onDraftTaskChange: (String) -> Unit,
    onRequiresPhotoChange: (Boolean) -> Unit,
    onIsMandatoryChange: (Boolean) -> Unit,
    onSaveDraft: () -> Unit,
    onNewDraft: () -> Unit,
    onEditDraft: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteDraft: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onApprove: () -> Unit,
    onRetryExtract: () -> Unit,
    onStartExtract: () -> Unit,
    onManualEntry: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenPlans: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Review requirements", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                state.documentFileName?.let { name ->
                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                }
                Text(text = state.unapprovedBanner, style = MaterialTheme.typography.titleMedium)
                if (state.versionStatus == ContractVersionRules.Extracted) {
                    Text(
                        text = "Review extracted tasks, then approve to create service rules.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.needsUpgrade) {
                    CpButton(label = "Open plans", onClick = onOpenPlans)
                }
                if (state.usedFallback) {
                    Text(
                        text = "A backup reader was used for this extraction.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                state.warnings.forEach { warning ->
                    Text(text = warning, style = MaterialTheme.typography.bodyLarge)
                }
                when (state.phase) {
                    ExtractionReviewPhase.Loading -> {
                        CpLabeledProgress(label = "Loading contract")
                    }
                    ExtractionReviewPhase.Processing -> {
                        CpLabeledProgress(label = "Reading requirements from the contract.")
                    }
                    ExtractionReviewPhase.Error -> {
                        CpErrorState(
                            message = state.banner ?: "Something went wrong.",
                            retryLabel = "Retry",
                            onRetry = onRetryLoad,
                        )
                        if (state.canExtract) {
                            CpButton(label = "Read contract again", onClick = onRetryExtract)
                        }
                        CpButton(
                            label = "Enter requirements manually",
                            onClick = onManualEntry,
                            style = CpButtonStyle.Secondary,
                        )
                    }
                    ExtractionReviewPhase.Empty, ExtractionReviewPhase.Review -> {
                        if (state.versionStatus == "uploaded" && state.canExtract && state.drafts.isEmpty()) {
                            CpButton(
                                label = "Read from contract",
                                onClick = onStartExtract,
                                enabled = !state.saving,
                            )
                        } else if (
                            state.versionStatus == "uploaded" &&
                            !state.canExtract &&
                            state.drafts.isEmpty()
                        ) {
                            CpButton(
                                label = "Upgrade to Pro for extraction",
                                onClick = onOpenPlans,
                                enabled = !state.saving,
                            )
                        }
                        if (state.canWrite) {
                            DraftForm(
                                editing = state.editingId != null,
                                task = state.draftTask,
                                requiresPhoto = state.draftRequiresPhoto,
                                isMandatory = state.draftIsMandatory,
                                saving = state.saving,
                                canSave = state.canSaveDraft,
                                onTaskChange = onDraftTaskChange,
                                onRequiresPhotoChange = onRequiresPhotoChange,
                                onIsMandatoryChange = onIsMandatoryChange,
                                onSave = onSaveDraft,
                                onCancel = onCancelEdit,
                                onNew = onNewDraft,
                            )
                        }
                        if (state.drafts.isEmpty()) {
                            Text(
                                text = "No requirements yet. Add one or read from the contract.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            state.drafts.forEachIndexed { index, draft ->
                                DraftRow(
                                    draft = draft,
                                    canWrite = state.canWrite,
                                    saving = state.saving,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < state.drafts.lastIndex,
                                    onEdit = { onEditDraft(draft.localId) },
                                    onDelete = { onDeleteDraft(draft.localId) },
                                    onMoveUp = { onMoveUp(draft.localId) },
                                    onMoveDown = { onMoveDown(draft.localId) },
                                )
                            }
                        }
                        if (state.canWrite) {
                            CpButton(
                                label = if (state.saving) "Approving contract" else "Approve requirements",
                                onClick = onApprove,
                                enabled = state.canApprove,
                            )
                            if (state.canExtract) {
                                CpButton(
                                    label = "Read contract again",
                                    onClick = onRetryExtract,
                                    enabled = !state.saving,
                                    style = CpButtonStyle.Secondary,
                                )
                            }
                            CpButton(
                                label = "Enter requirements manually",
                                onClick = onManualEntry,
                                enabled = !state.saving,
                                style = CpButtonStyle.Secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftForm(
    editing: Boolean,
    task: String,
    requiresPhoto: Boolean,
    isMandatory: Boolean,
    saving: Boolean,
    canSave: Boolean,
    onTaskChange: (String) -> Unit,
    onRequiresPhotoChange: (Boolean) -> Unit,
    onIsMandatoryChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onNew: () -> Unit,
) {
    CpCard {
        Text(
            text = if (editing) "Edit requirement" else "Add requirement",
            style = MaterialTheme.typography.titleMedium,
        )
        if (!editing) {
            CpButton(label = "New requirement", onClick = onNew, style = CpButtonStyle.Secondary)
        }
        CpTextField(value = task, onValueChange = onTaskChange, label = "Task")
        Row(horizontalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
            CpButton(
                label = if (requiresPhoto) "Photo required" else "No photo",
                onClick = { onRequiresPhotoChange(!requiresPhoto) },
                enabled = !saving,
                style = if (requiresPhoto) CpButtonStyle.Primary else CpButtonStyle.Secondary,
            )
            CpButton(
                label = if (isMandatory) "Required" else "Optional",
                onClick = { onIsMandatoryChange(!isMandatory) },
                enabled = !saving,
                style = if (isMandatory) CpButtonStyle.Primary else CpButtonStyle.Secondary,
            )
        }
        CpButton(
            label = if (editing) "Save changes" else "Add to list",
            onClick = onSave,
            enabled = canSave,
        )
        if (editing) {
            CpButton(label = "Cancel", onClick = onCancel, style = CpButtonStyle.Secondary)
        }
    }
}

@Composable
private fun DraftRow(
    draft: ReviewRequirementDraft,
    canWrite: Boolean,
    saving: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    CpCard {
        Text(text = draft.task, style = MaterialTheme.typography.titleMedium)
        if (draft.fromExtraction) {
            Text(text = "AI suggestion", style = MaterialTheme.typography.bodyLarge)
        }
        ExtractionReviewRules.confidenceLabel(draft.confidence)?.let { label ->
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
        draft.evidenceQuote?.takeIf { it.isNotBlank() }?.let { quote ->
            Text(text = "\"$quote\"", style = MaterialTheme.typography.bodyLarge)
        }
        Text(text = RequirementRules.evidenceLabel(draft.requiresPhoto), style = MaterialTheme.typography.bodyLarge)
        Text(text = RequirementRules.requiredLabel(draft.isMandatory), style = MaterialTheme.typography.bodyLarge)
        if (canWrite) {
            Row(horizontalArrangement = Arrangement.spacedBy(CpSpacing.sm)) {
                CpButton(
                    label = "Up",
                    onClick = onMoveUp,
                    enabled = canMoveUp && !saving,
                    style = CpButtonStyle.Secondary,
                )
                CpButton(
                    label = "Down",
                    onClick = onMoveDown,
                    enabled = canMoveDown && !saving,
                    style = CpButtonStyle.Secondary,
                )
                CpButton(label = "Edit", onClick = onEdit, enabled = !saving, style = CpButtonStyle.Secondary)
                CpButton(label = "Remove", onClick = onDelete, enabled = !saving, style = CpButtonStyle.Secondary)
            }
        }
    }
}
