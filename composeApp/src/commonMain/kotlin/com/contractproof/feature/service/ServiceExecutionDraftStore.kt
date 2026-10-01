package com.contractproof.feature.service

import com.contractproof.domain.EvidenceSyncStatus

data class EvidenceDraft(
    val requirementId: String,
    val localPath: String,
    val recordId: String,
    val syncStatus: EvidenceSyncStatus,
    val uploadPercent: Int? = null,
)

data class ExceptionDraft(
    val requirementId: String,
    val reason: String,
    val note: String,
    val pending: Boolean,
    val failed: Boolean = false,
)

class ServiceExecutionDraftStore {
    private val evidence = mutableMapOf<String, EvidenceDraft>()
    private val exceptions = mutableMapOf<String, ExceptionDraft>()
    var pendingCompletion: Boolean = false

    fun evidenceDraft(requirementId: String): EvidenceDraft? = evidence[requirementId]

    fun exceptionDraft(requirementId: String): ExceptionDraft? = exceptions[requirementId]

    fun allEvidenceDrafts(): Map<String, EvidenceDraft> = evidence.toMap()

    fun allExceptionDrafts(): Map<String, ExceptionDraft> = exceptions.toMap()

    fun putEvidence(draft: EvidenceDraft) {
        evidence[draft.requirementId] = draft
        exceptions.remove(draft.requirementId)
    }

    fun putException(draft: ExceptionDraft) {
        exceptions[draft.requirementId] = draft
        evidence.remove(draft.requirementId)
    }

    fun clearRequirement(requirementId: String) {
        evidence.remove(requirementId)
        exceptions.remove(requirementId)
    }

    fun clearJob() {
        evidence.clear()
        exceptions.clear()
        pendingCompletion = false
    }
}
