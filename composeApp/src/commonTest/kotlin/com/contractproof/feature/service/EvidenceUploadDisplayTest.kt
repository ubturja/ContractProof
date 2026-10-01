package com.contractproof.feature.service

import com.contractproof.core.design.CpWorkStatus
import com.contractproof.domain.EvidenceSyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class EvidenceUploadDisplayTest {
    @Test
    fun mapsDraftSyncStatusToWorkStatus() {
        val draft = EvidenceDraft(
            requirementId = "req-1",
            localPath = "/tmp/photo.jpg",
            recordId = "ev-1",
            syncStatus = EvidenceSyncStatus.Uploading,
            uploadPercent = 40,
        )
        assertEquals(CpWorkStatus.Uploading, ServiceExecutionDisplay.workStatusForEvidenceDraft(draft))
    }
}
