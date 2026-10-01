package com.contractproof.domain

enum class SyncItemKind {
    EvidenceUpload,
    ExceptionSubmit,
    JobStart,
    JobComplete,
    ;

    companion object {
        const val STORAGE_EVIDENCE_UPLOAD = "evidence_upload"
        const val STORAGE_EXCEPTION_SUBMIT = "exception_submit"
        const val STORAGE_JOB_START = "job_start"
        const val STORAGE_JOB_COMPLETE = "job_complete"

        fun fromStorage(value: String): SyncItemKind {
            return when (value) {
                STORAGE_EVIDENCE_UPLOAD -> EvidenceUpload
                STORAGE_EXCEPTION_SUBMIT -> ExceptionSubmit
                STORAGE_JOB_START -> JobStart
                STORAGE_JOB_COMPLETE -> JobComplete
                else -> throw IllegalArgumentException("Unknown sync item kind: $value")
            }
        }

        fun toStorage(kind: SyncItemKind): String {
            return when (kind) {
                EvidenceUpload -> STORAGE_EVIDENCE_UPLOAD
                ExceptionSubmit -> STORAGE_EXCEPTION_SUBMIT
                JobStart -> STORAGE_JOB_START
                JobComplete -> STORAGE_JOB_COMPLETE
            }
        }
    }
}
