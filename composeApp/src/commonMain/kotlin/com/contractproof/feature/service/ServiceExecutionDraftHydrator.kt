package com.contractproof.feature.service

import com.contractproof.data.local.SqlDelightEvidenceStore
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRules

class ServiceExecutionDraftHydrator(
    private val evidenceStore: SqlDelightEvidenceStore,
    private val exceptionStore: SqlDelightExceptionStore,
) {
    suspend fun hydrate(drafts: ServiceExecutionDraftStore, job: ServiceJob) {
        drafts.clearJob()
        val evidenceItems = evidenceStore.listForJob(job.organizationId, job.id)
        evidenceItems.forEach { evidence ->
            if (evidence.syncStatus == EvidenceSyncStatus.Uploaded) {
                return@forEach
            }
            val path = evidence.file?.localStagingPath ?: evidence.id
            drafts.putEvidence(
                EvidenceDraft(
                    requirementId = evidence.serviceJobRequirementId,
                    localPath = path,
                    recordId = evidence.id,
                    syncStatus = evidence.syncStatus,
                    uploadPercent = evidence.file?.uploadPercent,
                ),
            )
        }
        val exceptions = exceptionStore.listForJob(job.organizationId, job.id)
        exceptions.forEach { exception ->
            if (exception.syncStatus == EvidenceSyncStatus.Uploaded) {
                return@forEach
            }
            drafts.putException(
                ExceptionDraft(
                    requirementId = exception.serviceJobRequirementId,
                    reason = exception.reason,
                    note = "",
                    pending = exception.syncStatus != EvidenceSyncStatus.Failed,
                    failed = exception.syncStatus == EvidenceSyncStatus.Failed,
                ),
            )
        }
        drafts.pendingCompletion =
            job.status == ServiceJobRules.InProgress &&
            job.completedAt != null &&
            job.syncStatus != EvidenceSyncStatus.Uploaded
    }
}
