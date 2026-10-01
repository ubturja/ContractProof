package com.contractproof.data.local

import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.SyncItemKind
import com.contractproof.domain.SyncOutboxItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SqlDelightSyncQueueStoreTest {
    @Test
    fun enqueueAndSelectReady() = runBlocking {
        val database = ContractProofTestDatabase.create()
        val store = SqlDelightSyncQueueStore(database)
        store.enqueue(sampleItem())
        val ready = store.selectReady("2099-01-01T00:00:00Z")
        assertEquals(1, ready.size)
        assertEquals("dedupe-1", ready.first().dedupeKey)
    }

    private fun sampleItem(): SyncOutboxItem {
        return SyncOutboxItem(
            id = "out-1",
            organizationId = "org-1",
            kind = SyncItemKind.EvidenceUpload,
            serviceJobId = "job-1",
            requirementId = "req-1",
            dedupeKey = "dedupe-1",
            syncStatus = EvidenceSyncStatus.Pending,
            attemptCount = 0,
            nextRetryAt = "2026-10-01T00:00:00Z",
            lastError = null,
            createdAt = "2026-10-01T00:00:00Z",
            updatedAt = "2026-10-01T00:00:00Z",
        )
    }
}
