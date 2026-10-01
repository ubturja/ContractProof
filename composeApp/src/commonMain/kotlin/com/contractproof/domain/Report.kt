package com.contractproof.domain

import kotlinx.datetime.LocalDate

enum class ReportStatus {
    Generating,
    Ready,
    Failed,
    ;

    companion object {
        const val STORAGE_GENERATING = "generating"
        const val STORAGE_READY = "ready"
        const val STORAGE_FAILED = "failed"

        fun fromStorage(value: String): ReportStatus {
            return when (value) {
                STORAGE_GENERATING -> Generating
                STORAGE_READY -> Ready
                STORAGE_FAILED -> Failed
                else -> throw ReportRuleViolation("Report status is invalid.")
            }
        }

        fun toStorage(status: ReportStatus): String {
            return when (status) {
                Generating -> STORAGE_GENERATING
                Ready -> STORAGE_READY
                Failed -> STORAGE_FAILED
            }
        }
    }
}

data class EvidenceReportRecord(
    val id: String,
    val organizationId: String,
    val disputeId: String,
    val status: ReportStatus,
    val bucket: String,
    val objectPath: String?,
    val generatedBy: String?,
    val generatedAt: String?,
    val failureReason: String?,
)

data class ReportEntityRefs(
    val disputeId: String,
    val organizationId: String,
    val contractId: String,
    val contractVersionId: String,
    val serviceJobId: String,
    val disputedServiceJobRequirementId: String?,
    val disputeItemIds: List<String>,
    val serviceJobRequirementIds: List<String>,
    val evidenceRecordIds: List<String>,
    val exceptionIds: List<String>,
    val complaintAttachmentObjectPath: String?,
)

data class ReportGenerationRequest(
    val disputeId: String,
    val forceRegenerate: Boolean = false,
)

data class ReportGenerationResult(
    val report: EvidenceReportRecord,
    val signedUrl: String?,
)

data class ServiceEvidenceReportHeader(
    val clientName: String,
    val locationName: String,
    val serviceDate: LocalDate,
    val complaint: String,
    val disputedRequirementText: String,
    val contractTitle: String,
    val contractVersionLabel: String,
)

data class ReportContractRequirementLine(
    val serviceJobRequirementId: String,
    val requirementText: String,
    val isDisputed: Boolean,
)

data class ReportScheduledService(
    val scheduledStart: String,
    val scheduledEnd: String,
    val serviceDate: LocalDate,
)

data class ReportAssignedPersonnel(
    val displayName: String,
)

data class ReportEvidenceLine(
    val evidenceRecordId: String,
    val serviceJobRequirementId: String,
    val requirementText: String,
    val evidenceType: EvidenceType,
    val capturedAt: String,
    val isPhoto: Boolean,
    val objectPath: String?,
    val mimeType: String?,
)

data class ReportExceptionLine(
    val exceptionId: String,
    val serviceJobRequirementId: String,
    val requirementText: String,
    val reason: String,
    val recordedAt: String,
)

data class ReportAcknowledgementLine(
    val label: String,
    val occurredAt: String,
    val detail: String,
)

data class ReportCoverageLine(
    val serviceJobRequirementId: String,
    val requirementText: String,
    val outcome: DisputeOutcome,
)

data class ReportAttachmentLine(
    val label: String,
    val objectPath: String,
    val mimeType: String?,
)

data class ServiceEvidenceReport(
    val refs: ReportEntityRefs,
    val header: ServiceEvidenceReportHeader,
    val contractRequirements: List<ReportContractRequirementLine>,
    val scheduledService: ReportScheduledService,
    val assignedPersonnel: ReportAssignedPersonnel,
    val timeline: List<DisputeTimelineEvent>,
    val evidence: List<ReportEvidenceLine>,
    val exceptions: List<ReportExceptionLine>,
    val acknowledgements: List<ReportAcknowledgementLine>,
    val coverage: List<ReportCoverageLine>,
    val attachments: List<ReportAttachmentLine>,
)

class ReportRuleViolation(message: String) : Exception(message)
