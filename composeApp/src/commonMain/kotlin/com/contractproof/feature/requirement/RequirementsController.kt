package com.contractproof.feature.requirement

import com.contractproof.data.ContractFailure
import com.contractproof.data.ContractGateway
import com.contractproof.data.LocationFailure
import com.contractproof.data.LocationGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.RequirementFailure
import com.contractproof.data.RequirementGateway
import com.contractproof.data.ScheduleFailure
import com.contractproof.data.ScheduleGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.RequirementRecord
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.RequirementVisit
import com.contractproof.domain.ScheduleFrequency
import com.contractproof.domain.ScheduleRecord
import com.contractproof.domain.ScheduleRuleViolation
import com.contractproof.domain.ScheduleRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RequirementsUiState(
    val contractId: String = "",
    val versionId: String = "",
    val locationName: String = "",
    val locationId: String = "",
    val zoneCode: String = "",
    val contractStartsOn: String = "",
    val versionStatus: String = "",
    val items: List<RequirementRecord> = emptyList(),
    val schedules: List<ScheduleRecord> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
    val editingId: String? = null,
    val draftTask: String = "",
    val draftRequiresPhoto: Boolean = true,
    val draftIsMandatory: Boolean = true,
    val draftWeekday: Int = 1,
    val draftStartTime: String = "08:00",
    val draftEndTime: String = "10:00",
    val draftTimezone: String = "",
    val draftStartsOn: String = "",
    val draftEndsOn: String = "",
    val draftScheduleId: String? = null,
    val draftFrequency: ScheduleFrequency = ScheduleFrequency.Weekly,
) {
    val editable: Boolean
        get() = canWrite && ContractVersionRules.isInReviewStatus(versionStatus)

    val showScheduleWarning: Boolean
        get() = items.isNotEmpty() && schedules.none { it.visit.status == RequirementRules.ScheduleActive }

    val canSaveRequirement: Boolean
        get() = editable && draftTask.trim().isNotEmpty() && !loading && !saving

    val canSaveSchedule: Boolean
        get() = editable && draftTimezone.trim().isNotEmpty() &&
            draftStartsOn.trim().isNotEmpty() && !loading && !saving

    val canSaveZone: Boolean
        get() = editable && !loading && !saving
}

class RequirementsController(
    private val organizations: OrganizationGateway,
    private val requirements: RequirementGateway,
    private val schedules: ScheduleGateway,
    private val locations: LocationGateway,
    private val contracts: ContractGateway,
) {
    private val ui = MutableStateFlow(RequirementsUiState())
    val state: StateFlow<RequirementsUiState> = ui.asStateFlow()

    fun updateZoneCode(value: String) {
        ui.update { it.copy(zoneCode = value) }
    }

    fun updateDraftTask(value: String) {
        ui.update { it.copy(draftTask = value) }
    }

    fun updateDraftRequiresPhoto(value: Boolean) {
        ui.update { it.copy(draftRequiresPhoto = value) }
    }

    fun updateDraftIsMandatory(value: Boolean) {
        ui.update { it.copy(draftIsMandatory = value) }
    }

    fun updateDraftWeekday(value: Int) {
        ui.update { it.copy(draftWeekday = value) }
    }

    fun updateDraftFrequency(value: ScheduleFrequency) {
        ui.update { it.copy(draftFrequency = value) }
    }

    fun updateDraftStartTime(value: String) {
        ui.update { it.copy(draftStartTime = value) }
    }

    fun updateDraftEndTime(value: String) {
        ui.update { it.copy(draftEndTime = value) }
    }

    fun updateDraftStartsOn(value: String) {
        ui.update { it.copy(draftStartsOn = value) }
    }

    fun updateDraftEndsOn(value: String) {
        ui.update { it.copy(draftEndsOn = value) }
    }

    fun startNewRequirement() {
        ui.update {
            it.copy(
                editingId = null,
                draftTask = "",
                draftRequiresPhoto = true,
                draftIsMandatory = true,
            )
        }
    }

    fun startEditRequirement(id: String) {
        val record = ui.value.items.firstOrNull { it.id == id } ?: return
        if (record.source != RequirementRules.SourceManual) {
            ui.update { it.copy(banner = "Extracted requirements cannot be edited here.") }
            return
        }
        ui.update {
            it.copy(
                editingId = id,
                draftTask = record.task,
                draftRequiresPhoto = record.requiresPhoto,
                draftIsMandatory = record.isMandatory,
                banner = null,
            )
        }
    }

    fun cancelEdit() {
        ui.update {
            it.copy(
                editingId = null,
                draftTask = "",
                draftRequiresPhoto = true,
                draftIsMandatory = true,
            )
        }
    }

    suspend fun refresh(contractId: String, versionId: String) {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenContracts) {
            ui.update {
                it.copy(
                    loading = false,
                    canWrite = false,
                    banner = "Requirements are not available.",
                    items = emptyList(),
                )
            }
            return
        }
        ui.update {
            it.copy(
                loading = true,
                banner = null,
                canWrite = access.canWriteContracts,
                contractId = contractId,
                versionId = versionId,
            )
        }
        try {
            val header = requirements.headerForVersion(versionId)
            val items = requirements.listForVersion(versionId)
            val scheduleItems = schedules.listForContract(header.contractId)
            val location = locations.list().firstOrNull { it.id == header.locationId }
            val primary = scheduleItems.firstOrNull()
            ui.update { latest ->
                val timezone = location?.timezone ?: latest.draftTimezone
                latest.copy(
                    loading = false,
                    items = items,
                    schedules = scheduleItems,
                    locationName = header.locationName,
                    locationId = header.locationId,
                    zoneCode = header.zoneCode.orEmpty(),
                    contractStartsOn = header.contractStartsOn,
                    versionStatus = header.versionStatus,
                    draftTimezone = if (latest.draftTimezone.isEmpty()) timezone else latest.draftTimezone,
                    draftStartsOn = if (latest.draftStartsOn.isEmpty()) {
                        header.contractStartsOn
                    } else {
                        latest.draftStartsOn
                    },
                    draftScheduleId = primary?.id,
                    draftFrequency = primary?.let {
                        ScheduleFrequency.fromStorageValue(it.frequency)
                    } ?: latest.draftFrequency,
                    draftWeekday = primary?.visit?.weekday ?: latest.draftWeekday,
                    draftStartTime = primary?.visit?.startTime?.take(5) ?: latest.draftStartTime,
                    draftEndTime = primary?.visit?.endTime?.take(5) ?: latest.draftEndTime,
                    draftEndsOn = primary?.visit?.endsOn.orEmpty(),
                    banner = if (!ContractVersionRules.isInReviewStatus(header.versionStatus)) {
                        "This version is no longer in review. Requirements are read-only."
                    } else {
                        null
                    },
                )
            }
        } catch (failure: RequirementFailure) {
            ui.update { latest ->
                latest.copy(
                    loading = false,
                    banner = when (failure) {
                        RequirementFailure.Network ->
                            if (latest.items.isEmpty()) {
                                "You need a connection to load requirements."
                            } else {
                                "You are offline. Showing the last loaded requirements."
                            }
                        RequirementFailure.Rejected -> "Requirements could not be loaded."
                    },
                )
            }
        }
    }

    suspend fun saveZone() {
        val current = ui.value
        if (!current.editable) return
        val location = locations.list().firstOrNull { it.id == current.locationId } ?: return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            locations.update(
                id = location.id,
                name = location.name,
                timezone = location.timezone,
                address = location.address.orEmpty(),
                zoneCode = current.zoneCode,
                status = location.status,
            )
            ui.update { it.copy(saving = false, banner = null) }
            refresh(current.contractId, current.versionId)
        } catch (failure: LocationFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        LocationFailure.Network -> "You need a connection to save the zone."
                        LocationFailure.Rejected -> "The zone could not be saved."
                    },
                )
            }
        }
    }

    suspend fun saveSchedule() {
        val current = ui.value
        if (!current.canSaveSchedule) return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val weekday = ScheduleRules.requireWeekdayForFrequency(current.draftFrequency, current.draftWeekday)
            val visit = RequirementRules.requireVisit(
                RequirementVisit(
                    weekday = weekday,
                    startTime = current.draftStartTime,
                    endTime = current.draftEndTime,
                    timezone = current.draftTimezone,
                    startsOn = current.draftStartsOn,
                    endsOn = current.draftEndsOn.trim().takeIf { it.isNotEmpty() },
                    status = RequirementRules.ScheduleActive,
                ),
            )
            val saved = schedules.upsertSchedule(
                contractVersionId = current.versionId,
                frequency = current.draftFrequency,
                scheduleId = current.draftScheduleId,
                visit = visit,
            )
            ui.update {
                it.copy(
                    saving = false,
                    draftScheduleId = saved.id,
                    banner = null,
                )
            }
            refresh(current.contractId, current.versionId)
        } catch (_: ScheduleRuleViolation) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = "The visit schedule could not be saved.",
                )
            }
        } catch (failure: ScheduleFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ScheduleFailure.Network -> "You need a connection to save the visit schedule."
                        ScheduleFailure.Rejected -> "The visit schedule could not be saved."
                    },
                )
            }
        }
    }

    suspend fun saveRequirement() {
        val current = ui.value
        if (!current.canSaveRequirement) return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val task = RequirementRules.requireTask(current.draftTask)
            val order = if (current.editingId != null) {
                current.items.first { it.id == current.editingId }.sortOrder
            } else {
                RequirementRules.nextSortOrder(current.items)
            }
            if (current.editingId != null) {
                requirements.update(
                    id = current.editingId,
                    contractVersionId = current.versionId,
                    task = task,
                    requiresPhoto = current.draftRequiresPhoto,
                    isMandatory = current.draftIsMandatory,
                    sortOrder = order,
                )
            } else {
                requirements.create(
                    contractVersionId = current.versionId,
                    task = task,
                    requiresPhoto = current.draftRequiresPhoto,
                    isMandatory = current.draftIsMandatory,
                    sortOrder = order,
                )
            }
            cancelEdit()
            ui.update { it.copy(saving = false) }
            refresh(current.contractId, current.versionId)
        } catch (failure: RequirementFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        RequirementFailure.Network -> "You need a connection to save the requirement."
                        RequirementFailure.Rejected -> "The requirement could not be saved."
                    },
                )
            }
        }
    }

    suspend fun deleteRequirement(id: String) {
        val current = ui.value
        if (!current.editable) return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            requirements.delete(id, current.versionId)
            if (current.editingId == id) {
                cancelEdit()
            }
            ui.update { it.copy(saving = false) }
            refresh(current.contractId, current.versionId)
        } catch (failure: RequirementFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        RequirementFailure.Network -> "You need a connection to remove the requirement."
                        RequirementFailure.Rejected -> "The requirement could not be removed."
                    },
                )
            }
        }
    }

    suspend fun moveRequirement(id: String, delta: Int) {
        val current = ui.value
        if (!current.editable) return
        val ordered = current.items.sortedBy { it.sortOrder }.map { it.id }
        val moved = RequirementRules.moveInOrder(ordered, id, delta)
        if (moved == ordered) return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            requirements.reorder(current.versionId, moved)
            ui.update { it.copy(saving = false) }
            refresh(current.contractId, current.versionId)
        } catch (failure: RequirementFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        RequirementFailure.Network -> "You need a connection to reorder requirements."
                        RequirementFailure.Rejected -> "Requirements could not be reordered."
                    },
                )
            }
        }
    }

    suspend fun deactivateSchedule(scheduleId: String) {
        val current = ui.value
        if (!current.editable) return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            schedules.setStatus(
                contractVersionId = current.versionId,
                scheduleId = scheduleId,
                status = RequirementRules.SchedulePaused,
            )
            ui.update { it.copy(saving = false) }
            refresh(current.contractId, current.versionId)
        } catch (failure: ScheduleFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ScheduleFailure.Network -> "You need a connection to pause the visit schedule."
                        ScheduleFailure.Rejected -> "The visit schedule could not be paused."
                    },
                )
            }
        }
    }

    suspend fun approve(contractId: String, versionId: String): Boolean {
        val current = ui.value
        if (!current.editable || current.saving) {
            return false
        }
        if (current.items.isEmpty()) {
            ui.update { it.copy(banner = "Add at least one requirement before approving.") }
            return false
        }
        ui.update { it.copy(saving = true, banner = null) }
        return try {
            contracts.approveVersion(contractId, versionId)
            ui.update { it.copy(saving = false) }
            true
        } catch (failure: ContractFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to approve this contract."
                        else -> "The contract was not approved."
                    },
                )
            }
            false
        }
    }

    suspend fun reactivateSchedule(scheduleId: String) {
        val current = ui.value
        if (!current.editable) return
        ui.update { it.copy(saving = true, banner = null) }
        try {
            schedules.setStatus(
                contractVersionId = current.versionId,
                scheduleId = scheduleId,
                status = RequirementRules.ScheduleActive,
            )
            ui.update { it.copy(saving = false) }
            refresh(current.contractId, current.versionId)
        } catch (failure: ScheduleFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ScheduleFailure.Network -> "You need a connection to resume the visit schedule."
                        ScheduleFailure.Rejected -> "The visit schedule could not be resumed."
                    },
                )
            }
        }
    }
}
