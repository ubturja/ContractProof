package com.contractproof.domain

data class ServiceRecordTaskLine(
    val requirementText: String,
    val status: JobRequirementStatus,
)

data class ServiceRecordEvidenceLine(
    val requirementText: String,
    val label: String,
    val capturedAt: String,
)

data class ServiceRecordExceptionLine(
    val requirementText: String,
    val reason: String,
    val recordedAt: String,
)

data class ServiceRecordAcknowledgementLine(
    val label: String,
    val detail: String,
    val occurredAt: String,
)

data class ServiceRecordSnapshot(
    val jobId: String,
    val locationName: String,
    val serviceDate: String,
    val coveragePercent: Int,
    val completedTasks: List<ServiceRecordTaskLine>,
    val missingMandatoryLabels: List<String>,
    val evidenceLines: List<ServiceRecordEvidenceLine>,
    val exceptionLines: List<ServiceRecordExceptionLine>,
    val acknowledgements: List<ServiceRecordAcknowledgementLine>,
)

object ServiceRecordAssemblyRules {
    fun assemble(
        job: ServiceJob,
        evidence: List<Evidence>,
        exceptions: List<JobExceptionRecord>,
    ): ServiceRecordSnapshot {
        val requirementTextById = job.requirements.associate { it.id to it.requirementText }
        val uploadedEvidence = evidence.filter { it.syncStatus == EvidenceSyncStatus.Uploaded }
        val uploadedExceptions = exceptions.filter { it.syncStatus == EvidenceSyncStatus.Uploaded }

        val tasks = job.requirements
            .sortedBy { it.sortOrder }
            .map { requirement ->
                ServiceRecordTaskLine(
                    requirementText = requirement.requirementText,
                    status = requirement.status,
                )
            }

        val missingMandatory = job.requirements
            .filter { it.isMandatory && it.status == JobRequirementStatus.Missing }
            .map { it.requirementText }

        val evidenceLines = uploadedEvidence.map { record ->
            val requirementText = requirementTextById[record.serviceJobRequirementId] ?: record.serviceJobRequirementId
            val label = when (record.type) {
                EvidenceType.Photo -> "Photo recorded"
                EvidenceType.ChecklistCompletion -> "Checklist recorded"
                EvidenceType.Timestamp -> "Timestamp recorded"
            }
            ServiceRecordEvidenceLine(
                requirementText = requirementText,
                label = label,
                capturedAt = record.capturedAt,
            )
        }

        val exceptionLines = uploadedExceptions.map { ex ->
            ServiceRecordExceptionLine(
                requirementText = requirementTextById[ex.serviceJobRequirementId] ?: ex.serviceJobRequirementId,
                reason = ex.reason,
                recordedAt = ex.recordedAt,
            )
        }

        val acknowledgements = uploadedEvidence.mapNotNull { record ->
            val requirementText = requirementTextById[record.serviceJobRequirementId] ?: record.serviceJobRequirementId
            when (record.type) {
                EvidenceType.ChecklistCompletion -> ServiceRecordAcknowledgementLine(
                    label = "Checklist recorded",
                    detail = requirementText,
                    occurredAt = record.capturedAt,
                )
                EvidenceType.Timestamp -> ServiceRecordAcknowledgementLine(
                    label = "Timestamp recorded",
                    detail = requirementText,
                    occurredAt = record.capturedAt,
                )
                EvidenceType.Photo -> null
            }
        }

        return ServiceRecordSnapshot(
            jobId = job.id,
            locationName = job.location.name,
            serviceDate = job.serviceDate.toString(),
            coveragePercent = job.evidenceStatus.coveragePercent,
            completedTasks = tasks,
            missingMandatoryLabels = missingMandatory,
            evidenceLines = evidenceLines,
            exceptionLines = exceptionLines,
            acknowledgements = acknowledgements,
        )
    }
}
