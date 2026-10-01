package com.contractproof.feature.service

import com.contractproof.data.local.ContractProofTestDatabase
import com.contractproof.data.local.SqlDelightEvidenceStore
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFile
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRequirement
import com.contractproof.domain.ServiceJobRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class ServiceExecutionDraftHydratorTest {
    @Test
    fun hydratesPendingEvidenceDraft() = runBlocking {
        val database = ContractProofTestDatabase.create()
        val evidenceStore = SqlDelightEvidenceStore(database)
        val exceptionStore = SqlDelightExceptionStore(database)
        val hydrator = ServiceExecutionDraftHydrator(evidenceStore, exceptionStore)
        val drafts = ServiceExecutionDraftStore()
        val job = sampleJob()
        evidenceStore.upsert(
            Evidence(
                id = "ev-1",
                organizationId = "org-1",
                serviceJobId = "job-1",
                serviceJobRequirementId = "req-1",
                capturedByUserId = "user-1",
                capturedAt = "2026-10-01T09:30:00Z",
                type = EvidenceType.Photo,
                location = null,
                file = EvidenceFile(
                    id = "file-1",
                    evidenceRecordId = "ev-1",
                    bucket = "evidence",
                    objectPath = "org/job/req/ev",
                    mimeType = "image/jpeg",
                    byteSize = 10,
                    sha256 = "abc",
                    syncStatus = EvidenceSyncStatus.Pending,
                    uploadedAt = null,
                    localStagingPath = "/tmp/photo.jpg",
                ),
                syncStatus = EvidenceSyncStatus.Pending,
            ),
        )
        hydrator.hydrate(drafts, job)
        val draft = drafts.evidenceDraft("req-1")
        assertNotNull(draft)
        assertEquals("/tmp/photo.jpg", draft.localPath)
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("user-1", "Cleaner"),
            status = ServiceJobRules.InProgress,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "req-1",
                    contractRequirementId = "cr-1",
                    requirementText = "Vacuum",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.NotStarted),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = "schedule-1",
        )
    }
}
