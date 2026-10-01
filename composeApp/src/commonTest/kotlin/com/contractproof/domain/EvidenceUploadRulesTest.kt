package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvidenceUploadRulesTest {
    @Test
    fun reusesIdentityWhenUploadFailed() {
        val existing = sampleEvidence(EvidenceSyncStatus.Failed)
        assertTrue(EvidenceUploadRules.canReuseExisting(existing))
        val identity = EvidenceUploadRules.identityForUpload(
            existing = existing,
            newRecordId = "new-record",
            newFileId = "new-file",
            objectPathForNew = "org/job/req/new-record",
        )
        assertEquals(existing.id, identity.recordId)
        assertEquals(existing.file!!.id, identity.fileId)
        assertEquals(existing.file!!.objectPath, identity.objectPath)
    }

    @Test
    fun createsNewIdentityWhenNoExistingRow() {
        val identity = EvidenceUploadRules.identityForUpload(
            existing = null,
            newRecordId = "new-record",
            newFileId = "new-file",
            objectPathForNew = "org/job/req/new-record",
        )
        assertEquals("new-record", identity.recordId)
        assertEquals("new-file", identity.fileId)
    }

    @Test
    fun blocksCaptureWhenAlreadyUploaded() {
        assertFailsWith<EvidenceUploadRuleViolation> {
            EvidenceUploadRules.requireCanCapturePhoto(sampleEvidence(EvidenceSyncStatus.Uploaded))
        }
    }

    @Test
    fun uploadedEvidenceIsNotReusable() {
        assertFalse(EvidenceUploadRules.canReuseExisting(sampleEvidence(EvidenceSyncStatus.Uploaded)))
    }

    private fun sampleEvidence(status: EvidenceSyncStatus): Evidence {
        return Evidence(
            id = "ev-1",
            organizationId = "org-1",
            serviceJobId = "job-1",
            serviceJobRequirementId = "req-1",
            capturedByUserId = "user-1",
            capturedAt = "2026-10-01T09:00:00Z",
            type = EvidenceType.Photo,
            location = null,
            file = EvidenceFile(
                id = "file-1",
                evidenceRecordId = "ev-1",
                bucket = "evidence",
                objectPath = "org/job/req/ev-1",
                mimeType = "image/jpeg",
                byteSize = 10,
                sha256 = "a".repeat(64),
                syncStatus = status,
                uploadedAt = null,
            ),
            syncStatus = status,
        )
    }
}
