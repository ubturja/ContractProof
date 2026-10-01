package com.contractproof.domain

import kotlinx.datetime.LocalDate

data class ServiceJobClientRef(
    val id: String,
    val name: String,
)

data class ServiceJobLocationRef(
    val id: String,
    val name: String,
    val timezone: String,
)

data class ServiceJobAssignee(
    val userId: String,
    val displayName: String,
)

enum class JobRequirementStatus {
    Missing,
    Satisfied,
    Exception,
    ;

    companion object {
        const val STORAGE_MISSING = "missing"
        const val STORAGE_SATISFIED = "satisfied"
        const val STORAGE_EXCEPTION = "exception"

        fun fromStorage(value: String): JobRequirementStatus {
            return when (value) {
                STORAGE_MISSING -> Missing
                STORAGE_SATISFIED -> Satisfied
                STORAGE_EXCEPTION -> Exception
                else -> throw ServiceJobRuleViolation("Job requirement status is invalid.")
            }
        }

        fun toStorage(status: JobRequirementStatus): String {
            return when (status) {
                Missing -> STORAGE_MISSING
                Satisfied -> STORAGE_SATISFIED
                Exception -> STORAGE_EXCEPTION
            }
        }
    }
}

data class ServiceJobRequirement(
    val id: String,
    val contractRequirementId: String,
    val requirementText: String,
    val requiresPhoto: Boolean,
    val isMandatory: Boolean,
    val sortOrder: Int,
    val status: JobRequirementStatus,
)

enum class ServiceJobEvidenceState {
    NotStarted,
    Partial,
    ReadyToComplete,
}

data class ServiceJobEvidenceStatus(
    val mandatoryTotal: Int,
    val mandatorySatisfied: Int,
    val coveragePercent: Int,
    val state: ServiceJobEvidenceState,
)

data class ServiceJob(
    val id: String,
    val organizationId: String,
    val client: ServiceJobClientRef,
    val location: ServiceJobLocationRef,
    val serviceDate: LocalDate,
    val scheduledStart: String,
    val scheduledEnd: String,
    val assignee: ServiceJobAssignee?,
    val status: String,
    val requirements: List<ServiceJobRequirement>,
    val evidenceStatus: ServiceJobEvidenceStatus,
    val contractId: String,
    val contractVersionId: String,
    val scheduleId: String?,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val completedBy: String? = null,
    val syncStatus: EvidenceSyncStatus = EvidenceSyncStatus.Uploaded,
)
