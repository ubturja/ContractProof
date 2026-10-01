package com.contractproof.feature.requirement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.contractproof.domain.RequirementRecord
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.ScheduleFrequency
import com.contractproof.domain.ScheduleRecord
import com.contractproof.domain.ScheduleRules

@Composable
fun RequirementsScreen(
    state: RequirementsUiState,
    onZoneCodeChange: (String) -> Unit,
    onSaveZone: () -> Unit,
    onFrequencyChange: (ScheduleFrequency) -> Unit,
    onWeekdayChange: (Int) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onStartsOnChange: (String) -> Unit,
    onEndsOnChange: (String) -> Unit,
    onSaveSchedule: () -> Unit,
    onTaskChange: (String) -> Unit,
    onRequiresPhotoChange: (Boolean) -> Unit,
    onIsMandatoryChange: (Boolean) -> Unit,
    onSaveRequirement: () -> Unit,
    onNewRequirement: () -> Unit,
    onEditRequirement: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteRequirement: (String) -> Unit,
    onMoveRequirement: (String, Int) -> Unit,
    onPauseSchedule: (String) -> Unit,
    onResumeSchedule: (String) -> Unit,
    onApprove: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Requirements", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.loading && state.items.isEmpty()) {
                    CpLabeledProgress(label = "Loading requirements")
                } else if (state.items.isEmpty() && state.banner != null && !state.loading && state.locationName.isEmpty()) {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                    )
                } else {
                    LocationSection(
                        locationName = state.locationName,
                        zoneCode = state.zoneCode,
                        editable = state.editable,
                        saving = state.saving,
                        canSaveZone = state.canSaveZone,
                        onZoneCodeChange = onZoneCodeChange,
                        onSaveZone = onSaveZone,
                    )
                    ScheduleSection(
                        state = state,
                        onFrequencyChange = onFrequencyChange,
                        onWeekdayChange = onWeekdayChange,
                        onStartTimeChange = onStartTimeChange,
                        onEndTimeChange = onEndTimeChange,
                        onStartsOnChange = onStartsOnChange,
                        onEndsOnChange = onEndsOnChange,
                        onSaveSchedule = onSaveSchedule,
                        onPauseSchedule = onPauseSchedule,
                        onResumeSchedule = onResumeSchedule,
                    )
                    if (state.showScheduleWarning) {
                        Text(
                            text = "No active visit schedule. Jobs may not be generated until a visit is active.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Text(text = "Tasks", style = MaterialTheme.typography.titleMedium)
                    if (state.editable && state.canWrite) {
                        CpButton(
                            label = if (state.saving) "Approving contract" else "Approve contract",
                            onClick = onApprove,
                            enabled = state.items.isNotEmpty() && !state.saving,
                        )
                    }
                    if (state.editable) {
                        RequirementForm(
                            editing = state.editingId != null,
                            task = state.draftTask,
                            requiresPhoto = state.draftRequiresPhoto,
                            isMandatory = state.draftIsMandatory,
                            saving = state.saving,
                            canSave = state.canSaveRequirement,
                            onTaskChange = onTaskChange,
                            onRequiresPhotoChange = onRequiresPhotoChange,
                            onIsMandatoryChange = onIsMandatoryChange,
                            onSave = onSaveRequirement,
                            onCancel = onCancelEdit,
                            onNew = onNewRequirement,
                        )
                    }
                    val sorted = state.items.sortedBy { it.sortOrder }
                    if (sorted.isEmpty()) {
                        Text(text = "No requirements yet.", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        sorted.forEachIndexed { index, record ->
                            RequirementRow(
                                record = record,
                                editable = state.editable,
                                saving = state.saving,
                                canMoveUp = index > 0,
                                canMoveDown = index < sorted.lastIndex,
                                onEdit = { onEditRequirement(record.id) },
                                onDelete = { onDeleteRequirement(record.id) },
                                onMoveUp = { onMoveRequirement(record.id, -1) },
                                onMoveDown = { onMoveRequirement(record.id, 1) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    locationName: String,
    zoneCode: String,
    editable: Boolean,
    saving: Boolean,
    canSaveZone: Boolean,
    onZoneCodeChange: (String) -> Unit,
    onSaveZone: () -> Unit,
) {
    CpCard {
        Text(text = "Location", style = MaterialTheme.typography.titleMedium)
        Text(text = locationName, style = MaterialTheme.typography.bodyLarge)
        if (editable) {
            CpTextField(value = zoneCode, onValueChange = onZoneCodeChange, label = "Zone")
            CpButton(
                label = if (saving) "Saving zone" else "Save zone",
                onClick = onSaveZone,
                enabled = canSaveZone,
                style = CpButtonStyle.Secondary,
            )
        } else if (zoneCode.isNotEmpty()) {
            Text(text = "Zone $zoneCode", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ScheduleSection(
    state: RequirementsUiState,
    onFrequencyChange: (ScheduleFrequency) -> Unit,
    onWeekdayChange: (Int) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onStartsOnChange: (String) -> Unit,
    onEndsOnChange: (String) -> Unit,
    onSaveSchedule: () -> Unit,
    onPauseSchedule: (String) -> Unit,
    onResumeSchedule: (String) -> Unit,
) {
    CpCard {
        Text(text = "Visit schedule", style = MaterialTheme.typography.titleMedium)
        if (state.editable) {
            Text(text = "Frequency", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CpSpacing.xs),
            ) {
                listOf(
                    ScheduleFrequency.Daily,
                    ScheduleFrequency.Weekdays,
                    ScheduleFrequency.Weekly,
                ).forEach { frequency ->
                    val selected = state.draftFrequency == frequency
                    CpButton(
                        label = ScheduleRules.frequencyLabel(frequency),
                        onClick = { onFrequencyChange(frequency) },
                        enabled = !state.saving,
                        style = if (selected) CpButtonStyle.Primary else CpButtonStyle.Secondary,
                    )
                }
            }
            if (state.draftFrequency == ScheduleFrequency.Weekly) {
                Text(text = "Day", style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CpSpacing.xs),
                ) {
                    (1..7).forEach { day ->
                        val selected = state.draftWeekday == day
                        CpButton(
                            label = RequirementRules.weekdayLabel(day).take(3),
                            onClick = { onWeekdayChange(day) },
                            enabled = !state.saving,
                            style = if (selected) CpButtonStyle.Primary else CpButtonStyle.Secondary,
                        )
                    }
                }
            }
            CpTextField(
                value = state.draftStartTime,
                onValueChange = onStartTimeChange,
                label = "Start time (HH:MM)",
            )
            CpTextField(
                value = state.draftEndTime,
                onValueChange = onEndTimeChange,
                label = "End time (HH:MM)",
            )
            CpTextField(
                value = state.draftStartsOn,
                onValueChange = onStartsOnChange,
                label = "Schedule starts (YYYY-MM-DD)",
            )
            CpTextField(
                value = state.draftEndsOn,
                onValueChange = onEndsOnChange,
                label = "Schedule ends (optional)",
            )
            CpButton(
                label = if (state.saving) "Saving schedule" else "Save visit schedule",
                onClick = onSaveSchedule,
                enabled = state.canSaveSchedule,
            )
        }
        state.schedules.forEach { schedule ->
            ScheduleSummary(
                schedule = schedule,
                editable = state.editable,
                saving = state.saving,
                onPause = { onPauseSchedule(schedule.id) },
                onResume = { onResumeSchedule(schedule.id) },
            )
        }
    }
}

@Composable
private fun ScheduleSummary(
    schedule: ScheduleRecord,
    editable: Boolean,
    saving: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val visit = schedule.visit
    Text(
        text = "${ScheduleRules.frequencyLabel(ScheduleFrequency.fromStorageValue(schedule.frequency))} · " +
            "${RequirementRules.weekdayLabel(visit.weekday)} ${visit.startTime.take(5)}–${visit.endTime.take(5)}",
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(text = "Status: ${visit.status}", style = MaterialTheme.typography.bodyLarge)
    if (editable) {
        if (visit.status == RequirementRules.ScheduleActive) {
            CpButton(
                label = if (saving) "Pausing" else "Pause visits",
                onClick = onPause,
                enabled = !saving,
                style = CpButtonStyle.Secondary,
            )
        } else if (visit.status == RequirementRules.SchedulePaused) {
            CpButton(
                label = if (saving) "Resuming" else "Resume visits",
                onClick = onResume,
                enabled = !saving,
                style = CpButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun RequirementForm(
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
            label = if (saving) "Saving requirement" else if (editing) "Save changes" else "Add requirement",
            onClick = onSave,
            enabled = canSave,
        )
        if (editing) {
            CpButton(
                label = "Cancel",
                onClick = onCancel,
                enabled = !saving,
                style = CpButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun RequirementRow(
    record: RequirementRecord,
    editable: Boolean,
    saving: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    CpCard {
        Text(text = record.task, style = MaterialTheme.typography.titleMedium)
        Text(text = RequirementRules.evidenceLabel(record.requiresPhoto), style = MaterialTheme.typography.bodyLarge)
        Text(text = RequirementRules.requiredLabel(record.isMandatory), style = MaterialTheme.typography.bodyLarge)
        if (record.source != RequirementRules.SourceManual) {
            Text(text = "From contract extraction", style = MaterialTheme.typography.bodyLarge)
        }
        if (editable && record.source == RequirementRules.SourceManual) {
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
