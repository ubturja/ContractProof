package com.contractproof.domain

enum class EvidenceType {
    Photo,
    ChecklistCompletion,
    Timestamp,
    ;

    companion object {
        const val STORAGE_PHOTO = "photo"
        const val STORAGE_CHECKLIST_COMPLETION = "checklist_completion"
        const val STORAGE_TIMESTAMP = "timestamp"

        fun fromStorage(value: String): EvidenceType {
            return when (value) {
                STORAGE_PHOTO -> Photo
                STORAGE_CHECKLIST_COMPLETION -> ChecklistCompletion
                STORAGE_TIMESTAMP -> Timestamp
                else -> throw EvidenceRuleViolation("Evidence type is invalid.")
            }
        }

        fun toStorage(type: EvidenceType): String {
            return when (type) {
                Photo -> STORAGE_PHOTO
                ChecklistCompletion -> STORAGE_CHECKLIST_COMPLETION
                Timestamp -> STORAGE_TIMESTAMP
            }
        }
    }
}

data class EvidenceCoordinates(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Double?,
)

data class EvidenceLocation(
    val locationId: String?,
    val coordinates: EvidenceCoordinates?,
)

enum class EvidenceSyncStatus {
    Pending,
    Uploading,
    Uploaded,
    Failed,
    Retrying,
    ;

    companion object {
        const val STORAGE_PENDING = "pending"
        const val STORAGE_UPLOADING = "uploading"
        const val STORAGE_UPLOADED = "uploaded"
        const val STORAGE_FAILED = "failed"
        const val STORAGE_RETRYING = "retrying"

        fun fromStorage(value: String): EvidenceSyncStatus {
            return when (value) {
                STORAGE_PENDING -> Pending
                STORAGE_UPLOADING -> Uploading
                STORAGE_UPLOADED -> Uploaded
                STORAGE_FAILED -> Failed
                STORAGE_RETRYING -> Retrying
                else -> throw EvidenceRuleViolation("Evidence sync status is invalid.")
            }
        }

        fun toStorage(status: EvidenceSyncStatus): String {
            return when (status) {
                Pending -> STORAGE_PENDING
                Uploading -> STORAGE_UPLOADING
                Uploaded -> STORAGE_UPLOADED
                Failed -> STORAGE_FAILED
                Retrying -> STORAGE_RETRYING
            }
        }
    }
}

data class EvidenceFile(
    val id: String,
    val evidenceRecordId: String,
    val bucket: String,
    val objectPath: String,
    val mimeType: String,
    val byteSize: Long,
    val sha256: String,
    val syncStatus: EvidenceSyncStatus,
    val uploadedAt: String?,
    val localStagingPath: String? = null,
    val uploadPercent: Int? = null,
)

data class Evidence(
    val id: String,
    val organizationId: String,
    val serviceJobId: String,
    val serviceJobRequirementId: String,
    val capturedByUserId: String,
    val capturedAt: String,
    val type: EvidenceType,
    val location: EvidenceLocation?,
    val file: EvidenceFile?,
    val syncStatus: EvidenceSyncStatus,
    val receivedAt: String? = null,
    val isDemo: Boolean = false,
)

sealed class EvidenceFailure : Exception() {
    data object Network : EvidenceFailure()

    data object Rejected : EvidenceFailure()
}

data class EvidenceSubmitResult(
    val uploaded: Boolean,
    val pending: Boolean,
)
