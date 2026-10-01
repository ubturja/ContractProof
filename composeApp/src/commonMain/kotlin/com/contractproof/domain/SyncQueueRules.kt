package com.contractproof.domain

data class SyncOutboxItem(
    val id: String,
    val organizationId: String,
    val kind: SyncItemKind,
    val serviceJobId: String,
    val requirementId: String?,
    val dedupeKey: String,
    val syncStatus: EvidenceSyncStatus,
    val attemptCount: Int,
    val nextRetryAt: String,
    val lastError: String?,
    val createdAt: String,
    val updatedAt: String,
)

object SyncQueueRules {
    fun dedupeKeyFor(kind: SyncItemKind, jobId: String, requirementId: String?): String {
        return when (kind) {
            SyncItemKind.EvidenceUpload -> "evidence:$requirementId"
            SyncItemKind.ExceptionSubmit -> "exception:$requirementId"
            SyncItemKind.JobStart -> "start:$jobId"
            SyncItemKind.JobComplete -> "complete:$jobId"
        }
    }

    fun canProcess(status: EvidenceSyncStatus): Boolean {
        return status == EvidenceSyncStatus.Pending ||
            status == EvidenceSyncStatus.Failed ||
            status == EvidenceSyncStatus.Retrying
    }

    fun statusForUploadStart(): EvidenceSyncStatus = EvidenceSyncStatus.Uploading

    fun statusAfterSuccess(): EvidenceSyncStatus = EvidenceSyncStatus.Uploaded

    fun statusAfterFailure(): EvidenceSyncStatus = EvidenceSyncStatus.Failed

    fun statusForManualRetry(): EvidenceSyncStatus = EvidenceSyncStatus.Retrying
}
