package com.contractproof.data.local

import com.contractproof.domain.EvidenceSyncStatus

data class LocalTaskException(
    val id: String,
    val organizationId: String,
    val serviceJobId: String,
    val serviceJobRequirementId: String,
    val recordedBy: String,
    val recordedAt: String,
    val reason: String,
    val syncStatus: EvidenceSyncStatus,
)

object ExceptionLocalMapper {
    fun toDomain(row: Task_exception): LocalTaskException {
        return LocalTaskException(
            id = row.id,
            organizationId = row.organization_id,
            serviceJobId = row.service_job_id,
            serviceJobRequirementId = row.service_job_requirement_id,
            recordedBy = row.recorded_by,
            recordedAt = row.recorded_at,
            reason = row.reason,
            syncStatus = EvidenceSyncStatus.fromStorage(row.sync_status),
        )
    }

    fun values(exception: LocalTaskException): TaskExceptionValues {
        return TaskExceptionValues(
            id = exception.id,
            organizationId = exception.organizationId,
            serviceJobId = exception.serviceJobId,
            serviceJobRequirementId = exception.serviceJobRequirementId,
            recordedBy = exception.recordedBy,
            recordedAt = exception.recordedAt,
            reason = exception.reason,
            syncStatus = EvidenceSyncStatus.toStorage(exception.syncStatus),
        )
    }
}

data class TaskExceptionValues(
    val id: String,
    val organizationId: String,
    val serviceJobId: String,
    val serviceJobRequirementId: String,
    val recordedBy: String,
    val recordedAt: String,
    val reason: String,
    val syncStatus: String,
)
