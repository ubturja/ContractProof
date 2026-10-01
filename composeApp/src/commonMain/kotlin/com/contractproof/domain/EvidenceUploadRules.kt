package com.contractproof.domain

class EvidenceUploadRuleViolation(message: String) : IllegalArgumentException(message)

data class EvidenceUploadIdentity(
    val recordId: String,
    val fileId: String,
    val objectPath: String,
)

object EvidenceUploadRules {
    fun canReuseExisting(existing: Evidence?): Boolean {
        if (existing == null) return false
        return when (existing.syncStatus) {
            EvidenceSyncStatus.Pending,
            EvidenceSyncStatus.Uploading,
            EvidenceSyncStatus.Failed,
            EvidenceSyncStatus.Retrying,
            -> true
            EvidenceSyncStatus.Uploaded -> false
        }
    }

    fun requireCanCapturePhoto(existing: Evidence?) {
        if (existing?.syncStatus == EvidenceSyncStatus.Uploaded) {
            throw EvidenceUploadRuleViolation("Evidence for this requirement is already uploaded.")
        }
    }

    fun identityForUpload(
        existing: Evidence?,
        newRecordId: String,
        newFileId: String,
        objectPathForNew: String,
    ): EvidenceUploadIdentity {
        if (existing != null && canReuseExisting(existing)) {
            val file = existing.file ?: throw EvidenceUploadRuleViolation("Evidence file metadata is missing.")
            return EvidenceUploadIdentity(
                recordId = existing.id,
                fileId = file.id,
                objectPath = file.objectPath,
            )
        }
        return EvidenceUploadIdentity(
            recordId = newRecordId,
            fileId = newFileId,
            objectPath = objectPathForNew,
        )
    }
}
