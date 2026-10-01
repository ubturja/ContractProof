package com.contractproof.domain

enum class EvidenceRequirementStatus {
    Complete,
    Incomplete,
    EvidenceMissing,
    ExceptionReported,
    UploadPending,
    UploadFailed,
    ;

    companion object {
        const val STORAGE_COMPLETE = "complete"
        const val STORAGE_INCOMPLETE = "incomplete"
        const val STORAGE_EVIDENCE_MISSING = "evidence_missing"
        const val STORAGE_EXCEPTION_REPORTED = "exception_reported"
        const val STORAGE_UPLOAD_PENDING = "upload_pending"
        const val STORAGE_UPLOAD_FAILED = "upload_failed"
    }
}
