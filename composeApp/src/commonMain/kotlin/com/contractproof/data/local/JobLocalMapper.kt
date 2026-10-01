package com.contractproof.data.local

import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRequirement
import kotlinx.datetime.LocalDate

object JobLocalMapper {
    fun toDomain(job: Service_job, requirements: List<Service_job_requirement>): ServiceJob {
        val assignee = job.assignee_user_id?.let { userId ->
            ServiceJobAssignee(
                userId = userId,
                displayName = job.assignee_display_name ?: userId,
            )
        }
        return ServiceJob(
            id = job.id,
            organizationId = job.organization_id,
            client = ServiceJobClientRef(job.client_id, job.client_name),
            location = ServiceJobLocationRef(
                job.location_id,
                job.location_name,
                job.location_timezone,
            ),
            serviceDate = LocalDate.parse(job.service_date),
            scheduledStart = job.scheduled_start,
            scheduledEnd = job.scheduled_end,
            assignee = assignee,
            status = job.status,
            requirements = requirements.map { toDomainRequirement(it) },
            evidenceStatus = ServiceJobEvidenceStatus(
                mandatoryTotal = job.mandatory_total.toInt(),
                mandatorySatisfied = job.mandatory_satisfied.toInt(),
                coveragePercent = job.coverage_percent.toInt(),
                state = evidenceStateFromStorage(job.evidence_state),
            ),
            contractId = job.contract_id,
            contractVersionId = job.contract_version_id,
            scheduleId = job.schedule_id,
            startedAt = job.started_at,
            completedAt = job.completed_at,
            completedBy = job.completed_by,
            syncStatus = EvidenceSyncStatus.fromStorage(job.sync_status),
        )
    }

    fun toDomainRequirement(row: Service_job_requirement): ServiceJobRequirement {
        return ServiceJobRequirement(
            id = row.id,
            contractRequirementId = row.contract_requirement_id,
            requirementText = row.requirement_text,
            requiresPhoto = row.requires_photo != 0L,
            isMandatory = row.is_mandatory != 0L,
            sortOrder = row.sort_order.toInt(),
            status = JobRequirementStatus.fromStorage(row.status),
        )
    }

    fun jobValues(job: ServiceJob): ServiceJobValues {
        return ServiceJobValues(
            id = job.id,
            organizationId = job.organizationId,
            clientId = job.client.id,
            clientName = job.client.name,
            locationId = job.location.id,
            locationName = job.location.name,
            locationTimezone = job.location.timezone,
            serviceDate = job.serviceDate.toString(),
            scheduledStart = job.scheduledStart,
            scheduledEnd = job.scheduledEnd,
            assigneeUserId = job.assignee?.userId,
            assigneeDisplayName = job.assignee?.displayName,
            status = job.status,
            startedAt = job.startedAt,
            completedAt = job.completedAt,
            completedBy = job.completedBy,
            contractId = job.contractId,
            contractVersionId = job.contractVersionId,
            scheduleId = job.scheduleId,
            mandatoryTotal = job.evidenceStatus.mandatoryTotal.toLong(),
            mandatorySatisfied = job.evidenceStatus.mandatorySatisfied.toLong(),
            coveragePercent = job.evidenceStatus.coveragePercent.toLong(),
            evidenceState = evidenceStateToStorage(job.evidenceStatus.state),
            syncStatus = EvidenceSyncStatus.toStorage(job.syncStatus),
            isDemo = 0L,
        )
    }

    fun requirementValues(
        organizationId: String,
        jobId: String,
        requirement: ServiceJobRequirement,
    ): ServiceJobRequirementValues {
        return ServiceJobRequirementValues(
            id = requirement.id,
            organizationId = organizationId,
            serviceJobId = jobId,
            contractRequirementId = requirement.contractRequirementId,
            requirementText = requirement.requirementText,
            requiresPhoto = if (requirement.requiresPhoto) 1L else 0L,
            isMandatory = if (requirement.isMandatory) 1L else 0L,
            sortOrder = requirement.sortOrder.toLong(),
            status = JobRequirementStatus.toStorage(requirement.status),
        )
    }

    private fun evidenceStateFromStorage(value: String): ServiceJobEvidenceState {
        return when (value) {
            "not_started" -> ServiceJobEvidenceState.NotStarted
            "partial" -> ServiceJobEvidenceState.Partial
            "ready_to_complete" -> ServiceJobEvidenceState.ReadyToComplete
            else -> ServiceJobEvidenceState.NotStarted
        }
    }

    private fun evidenceStateToStorage(state: ServiceJobEvidenceState): String {
        return when (state) {
            ServiceJobEvidenceState.NotStarted -> "not_started"
            ServiceJobEvidenceState.Partial -> "partial"
            ServiceJobEvidenceState.ReadyToComplete -> "ready_to_complete"
        }
    }
}

data class ServiceJobValues(
    val id: String,
    val organizationId: String,
    val clientId: String,
    val clientName: String,
    val locationId: String,
    val locationName: String,
    val locationTimezone: String,
    val serviceDate: String,
    val scheduledStart: String,
    val scheduledEnd: String,
    val assigneeUserId: String?,
    val assigneeDisplayName: String?,
    val status: String,
    val startedAt: String?,
    val completedAt: String?,
    val completedBy: String?,
    val contractId: String,
    val contractVersionId: String,
    val scheduleId: String?,
    val mandatoryTotal: Long,
    val mandatorySatisfied: Long,
    val coveragePercent: Long,
    val evidenceState: String,
    val syncStatus: String,
    val isDemo: Long,
)

data class ServiceJobRequirementValues(
    val id: String,
    val organizationId: String,
    val serviceJobId: String,
    val contractRequirementId: String,
    val requirementText: String,
    val requiresPhoto: Long,
    val isMandatory: Long,
    val sortOrder: Long,
    val status: String,
)
