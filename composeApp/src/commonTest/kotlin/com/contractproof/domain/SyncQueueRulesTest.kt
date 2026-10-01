package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncQueueRulesTest {
    @Test
    fun dedupeKeysAreStable() {
        assertEquals(
            "evidence:req-1",
            SyncQueueRules.dedupeKeyFor(SyncItemKind.EvidenceUpload, "job-1", "req-1"),
        )
        assertEquals(
            "complete:job-1",
            SyncQueueRules.dedupeKeyFor(SyncItemKind.JobComplete, "job-1", null),
        )
    }

    @Test
    fun pendingFailedAndRetryingCanProcess() {
        assertTrue(SyncQueueRules.canProcess(EvidenceSyncStatus.Pending))
        assertTrue(SyncQueueRules.canProcess(EvidenceSyncStatus.Failed))
        assertTrue(SyncQueueRules.canProcess(EvidenceSyncStatus.Retrying))
    }
}
