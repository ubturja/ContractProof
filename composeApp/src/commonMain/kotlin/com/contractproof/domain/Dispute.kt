package com.contractproof.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject

enum class DisputeStatus {
    Open,
    Closed,
    ;

    companion object {
        const val STORAGE_OPEN = "open"
        const val STORAGE_CLOSED = "closed"

        fun fromStorage(value: String): DisputeStatus {
            return when (value) {
                STORAGE_OPEN -> Open
                STORAGE_CLOSED -> Closed
                else -> throw DisputeRuleViolation("Dispute status is invalid.")
            }
        }

        fun toStorage(status: DisputeStatus): String {
            return when (status) {
                Open -> STORAGE_OPEN
                Closed -> STORAGE_CLOSED
            }
        }
    }
}

enum class DisputeOutcome {
    Satisfied,
    Missing,
    Exception,
    ;

    companion object {
        const val STORAGE_SATISFIED = "satisfied"
        const val STORAGE_MISSING = "missing"
        const val STORAGE_EXCEPTION = "exception"

        fun fromStorage(value: String): DisputeOutcome {
            return when (value) {
                STORAGE_SATISFIED -> Satisfied
                STORAGE_MISSING -> Missing
                STORAGE_EXCEPTION -> Exception
                else -> throw DisputeRuleViolation("Dispute outcome is invalid.")
            }
        }

        fun toStorage(outcome: DisputeOutcome): String {
            return when (outcome) {
                Satisfied -> STORAGE_SATISFIED
                Missing -> STORAGE_MISSING
                Exception -> STORAGE_EXCEPTION
            }
        }
    }
}

data class DisputeItem(
    val id: String,
    val disputeId: String,
    val serviceJobId: String,
    val serviceJobRequirementId: String,
    val evidenceRecordId: String?,
    val exceptionId: String?,
    val outcome: DisputeOutcome,
    val createdAt: String,
)

data class DisputeItemDraft(
    val serviceJobRequirementId: String,
    val evidenceRecordId: String?,
    val exceptionId: String?,
    val outcome: DisputeOutcome,
)

data class Dispute(
    val id: String,
    val organizationId: String,
    val clientId: String,
    val clientName: String,
    val locationId: String,
    val locationName: String,
    val serviceDate: LocalDate,
    val complaint: String,
    val serviceJobId: String?,
    val disputedServiceJobRequirementId: String?,
    val complaintAttachmentObjectPath: String?,
    val complaintAttachmentMimeType: String?,
    val recordedBy: String,
    val status: DisputeStatus,
    val syncStatus: EvidenceSyncStatus,
    val createdAt: String,
    val aiSummaryJson: JsonObject? = null,
    val aiSummaryGeneratedAt: String? = null,
)

data class DisputeDraft(
    val serviceJobId: String,
    val disputedServiceJobRequirementId: String,
    val complaint: String,
    val serviceDate: LocalDate,
    val attachmentLocalPath: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentBytes: ByteArray? = null,
)

data class JobExceptionRecord(
    val id: String,
    val serviceJobRequirementId: String,
    val reason: String,
    val recordedAt: String,
    val recordedBy: String,
    val syncStatus: EvidenceSyncStatus,
)

class DisputeRuleViolation(message: String) : Exception(message)
