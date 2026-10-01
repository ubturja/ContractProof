package com.contractproof.data

import com.contractproof.data.local.LocalTaskException
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.sync.SyncCoordinator
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.SyncItemKind
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class OfflineFirstExceptionGateway(
    private val remote: ExceptionGateway,
    private val organizations: OrganizationGateway,
    private val auth: AuthGateway,
    private val local: SqlDelightExceptionStore,
    private val syncCoordinator: SyncCoordinator,
) : ExceptionGateway {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun submit(
        jobId: String,
        requirementId: String,
        reason: String,
        recordedAt: String,
    ): TaskExceptionResult {
        val membership = organizations.currentMembership() ?: throw ExceptionFailure.Rejected
        val user = auth.currentUser() ?: throw ExceptionFailure.Rejected
        val existing = local.getByRequirement(membership.organizationId, requirementId)
        val exceptionId = existing?.id ?: Uuid.generateV4().toString().lowercase()
        val draft = LocalTaskException(
            id = exceptionId,
            organizationId = membership.organizationId,
            serviceJobId = jobId,
            serviceJobRequirementId = requirementId,
            recordedBy = user.id,
            recordedAt = recordedAt,
            reason = reason,
            syncStatus = EvidenceSyncStatus.Pending,
        )
        local.upsert(draft)
        syncCoordinator.enqueue(SyncItemKind.ExceptionSubmit, jobId, requirementId)
        return try {
            val result = remote.submit(jobId, requirementId, reason, recordedAt)
            if (result.uploaded) {
                local.upsert(draft.copy(syncStatus = EvidenceSyncStatus.Uploaded))
            }
            result
        } catch (error: ExceptionFailure) {
            if (error == ExceptionFailure.Network) {
                TaskExceptionResult(uploaded = false, pending = true)
            } else {
                throw error
            }
        }
    }
}
