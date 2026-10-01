package com.contractproof.data.local

import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.SyncItemKind
import com.contractproof.domain.SyncOutboxItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightSyncQueueStore(
    private val database: ContractProofDatabase,
) {
    private val queries = database.syncOutboxQueries

    suspend fun enqueue(item: SyncOutboxItem) {
        withContext(Dispatchers.Default) {
            queries.insertOrIgnore(
                id = item.id,
                organization_id = item.organizationId,
                kind = SyncItemKind.toStorage(item.kind),
                service_job_id = item.serviceJobId,
                requirement_id = item.requirementId,
                dedupe_key = item.dedupeKey,
                sync_status = EvidenceSyncStatus.toStorage(item.syncStatus),
                attempt_count = item.attemptCount.toLong(),
                next_retry_at = item.nextRetryAt,
                last_error = item.lastError,
                created_at = item.createdAt,
                updated_at = item.updatedAt,
            )
        }
    }

    suspend fun selectReady(nowIso: String, limit: Long = 20): List<SyncOutboxItem> {
        return withContext(Dispatchers.Default) {
            queries.selectReady(nowIso, limit).executeAsList().map { it.toDomain() }
        }
    }

    suspend fun findByDedupeKey(dedupeKey: String): SyncOutboxItem? {
        return withContext(Dispatchers.Default) {
            queries.selectByDedupeKey(dedupeKey).executeAsOneOrNull()?.toDomain()
        }
    }

    suspend fun updateStatus(id: String, status: EvidenceSyncStatus, updatedAt: String, lastError: String?) {
        withContext(Dispatchers.Default) {
            queries.updateStatus(
                sync_status = EvidenceSyncStatus.toStorage(status),
                updated_at = updatedAt,
                last_error = lastError,
                id = id,
            )
        }
    }

    suspend fun updateForRetry(
        id: String,
        status: EvidenceSyncStatus,
        attemptCount: Int,
        nextRetryAt: String,
        updatedAt: String,
        lastError: String?,
    ) {
        withContext(Dispatchers.Default) {
            queries.updateForRetry(
                sync_status = EvidenceSyncStatus.toStorage(status),
                attempt_count = attemptCount.toLong(),
                next_retry_at = nextRetryAt,
                updated_at = updatedAt,
                last_error = lastError,
                id = id,
            )
        }
    }

    suspend fun deleteById(id: String) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    suspend fun resetForImmediateRetry(dedupeKey: String, nowIso: String) {
        withContext(Dispatchers.Default) {
            queries.resetForImmediateRetry(
                next_retry_at = nowIso,
                updated_at = nowIso,
                dedupe_key = dedupeKey,
            )
        }
    }

    suspend fun countPendingOrRetrying(): Int {
        return withContext(Dispatchers.Default) {
            queries.countPendingOrRetrying().executeAsOne().toInt()
        }
    }

    suspend fun countFailed(): Int {
        return withContext(Dispatchers.Default) {
            queries.countFailed().executeAsOne().toInt()
        }
    }

    private fun Sync_outbox.toDomain(): SyncOutboxItem {
        return SyncOutboxItem(
            id = id,
            organizationId = organization_id,
            kind = SyncItemKind.fromStorage(kind),
            serviceJobId = service_job_id,
            requirementId = requirement_id,
            dedupeKey = dedupe_key,
            syncStatus = EvidenceSyncStatus.fromStorage(sync_status),
            attemptCount = attempt_count.toInt(),
            nextRetryAt = next_retry_at,
            lastError = last_error,
            createdAt = created_at,
            updatedAt = updated_at,
        )
    }
}
