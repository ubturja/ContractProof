package com.contractproof.data.sync

import com.contractproof.data.ExceptionFailure
import com.contractproof.data.ExceptionGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.local.SqlDelightSyncMetadataStore
import com.contractproof.data.local.SqlDelightSyncQueueStore
import com.contractproof.domain.EvidenceFailure
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.SyncBackoffRules
import com.contractproof.domain.SyncItemKind
import com.contractproof.domain.SyncOutboxItem
import com.contractproof.domain.SyncQueueRules
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SyncCoordinator(
    private val organizations: OrganizationGateway,
    private val queue: SqlDelightSyncQueueStore,
    private val metadata: SqlDelightSyncMetadataStore,
    private val evidence: EvidenceRepository,
    private val exceptions: ExceptionGateway,
    private val serviceJobs: ServiceJobRepository,
    private val exceptionStore: SqlDelightExceptionStore,
    private val clock: () -> Instant = { Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()) },
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun enqueue(
        kind: SyncItemKind,
        jobId: String,
        requirementId: String? = null,
    ) {
        val membership = organizations.currentMembership() ?: return
        val now = nowIso()
        val dedupeKey = SyncQueueRules.dedupeKeyFor(kind, jobId, requirementId)
        if (queue.findByDedupeKey(dedupeKey) != null) {
            return
        }
        queue.enqueue(
            SyncOutboxItem(
                id = Uuid.generateV4().toString().lowercase(),
                organizationId = membership.organizationId,
                kind = kind,
                serviceJobId = jobId,
                requirementId = requirementId,
                dedupeKey = dedupeKey,
                syncStatus = EvidenceSyncStatus.Pending,
                attemptCount = 0,
                nextRetryAt = now,
                lastError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun requestImmediateRetry(dedupeKey: String) {
        queue.resetForImmediateRetry(dedupeKey, nowIso())
        drain()
    }

    suspend fun drain() {
        val now = nowIso()
        val items = queue.selectReady(now)
        for (item in items) {
            if (!SyncQueueRules.canProcess(item.syncStatus)) {
                continue
            }
            queue.updateStatus(item.id, SyncQueueRules.statusForUploadStart(), now, null)
            try {
                when (item.kind) {
                    SyncItemKind.EvidenceUpload -> {
                        val requirementId = item.requirementId ?: throw IllegalStateException("Missing requirement")
                        val result = evidence.uploadPhotoEvidence(item.serviceJobId, requirementId) { }
                        if (!result.uploaded) {
                            throw EvidenceFailure.Network
                        }
                    }
                    SyncItemKind.ExceptionSubmit -> {
                        val requirementId = item.requirementId ?: throw IllegalStateException("Missing requirement")
                        val membership = organizations.currentMembership() ?: throw ExceptionFailure.Rejected
                        val local = exceptionStore.getByRequirement(membership.organizationId, requirementId)
                            ?: throw ExceptionFailure.Rejected
                        val result = exceptions.submit(
                            jobId = item.serviceJobId,
                            requirementId = requirementId,
                            reason = local.reason,
                            recordedAt = local.recordedAt,
                        )
                        if (!result.uploaded) {
                            throw ExceptionFailure.Network
                        }
                    }
                    SyncItemKind.JobStart -> {
                        val job = serviceJobs.get(item.serviceJobId) ?: throw ServiceJobFailure.Rejected
                        val startedAt = job.startedAt ?: now
                        serviceJobs.start(item.serviceJobId, startedAt)
                    }
                    SyncItemKind.JobComplete -> {
                        val job = serviceJobs.get(item.serviceJobId) ?: throw ServiceJobFailure.Rejected
                        val completedAt = job.completedAt ?: now
                        val completedBy = job.completedBy
                            ?: organizations.currentMembership()?.userId
                            ?: throw ServiceJobFailure.Rejected
                        serviceJobs.complete(item.serviceJobId, completedAt, completedBy)
                    }
                }
                queue.deleteById(item.id)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val attempts = item.attemptCount + 1
                val delaySeconds = SyncBackoffRules.delaySecondsForAttempt(attempts)
                val nextRetry = instantPlusSeconds(clock(), delaySeconds)
                queue.updateForRetry(
                    id = item.id,
                    status = SyncQueueRules.statusAfterFailure(),
                    attemptCount = attempts,
                    nextRetryAt = nextRetry,
                    updatedAt = nowIso(),
                    lastError = error.message,
                )
            }
        }
        metadata.put(METADATA_LAST_DRAIN, nowIso())
    }

    private fun nowIso(): String {
        return clock().toString()
    }

    private fun instantPlusSeconds(instant: Instant, seconds: Long): String {
        return Instant.fromEpochMilliseconds(instant.toEpochMilliseconds() + seconds * 1000).toString()
    }

    companion object {
        const val METADATA_LAST_DRAIN = "last_outbox_drain_at"
    }
}
