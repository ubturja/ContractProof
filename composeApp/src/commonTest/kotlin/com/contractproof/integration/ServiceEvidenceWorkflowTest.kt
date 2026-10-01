package com.contractproof.integration

import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.local.ContractProofTestDatabase
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.local.SqlDelightSyncMetadataStore
import com.contractproof.data.local.SqlDelightSyncQueueStore
import com.contractproof.data.sync.SyncCoordinator
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFailure
import com.contractproof.domain.EvidenceLocation
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.EvidenceRequirementStatus
import com.contractproof.domain.EvidenceSubmitResult
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.JobCoverageSnapshot
import com.contractproof.domain.RequirementCoverageRow
import com.contractproof.domain.ServiceCompletionRules
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import com.contractproof.domain.SyncItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ServiceEvidenceWorkflowTest {
    @Test
    fun mandatoryUploadPendingAllowsFinishThenSyncRetriesUpload() = runBlocking {
        val snapshot = JobCoverageSnapshot(
            totalRequirements = 1,
            completedRequirements = 1,
            requiredEvidenceCount = 1,
            missingEvidenceCount = 0,
            coveragePercent = 100,
            evidenceState = com.contractproof.domain.ServiceJobEvidenceState.ReadyToComplete,
            requirements = listOf(
                RequirementCoverageRow(
                    requirementId = "req-1",
                    text = "Vacuum",
                    isMandatory = true,
                    requiresPhoto = true,
                    status = EvidenceRequirementStatus.UploadPending,
                    nextActionHint = "",
                ),
            ),
        )
        assertTrue(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress))

        val database = ContractProofTestDatabase.create()
        val queue = SqlDelightSyncQueueStore(database)
        val evidence = FailingThenUploadingEvidence()
        val coordinator = SyncCoordinator(
            organizations = FixedOrg(),
            queue = queue,
            metadata = SqlDelightSyncMetadataStore(database),
            evidence = evidence,
            exceptions = NoOpExceptions(),
            serviceJobs = NoOpJobs(),
            exceptionStore = SqlDelightExceptionStore(database),
            clock = { kotlinx.datetime.Instant.fromEpochMilliseconds(1_000_000) },
        )
        coordinator.enqueue(SyncItemKind.EvidenceUpload, "job-1", "req-1")
        coordinator.drain()
        assertNotNull(queue.findByDedupeKey("evidence:req-1"))
        queue.resetForImmediateRetry("evidence:req-1", "1970-01-01T00:16:40Z")
        coordinator.drain()
        assertNull(queue.findByDedupeKey("evidence:req-1"))
        assertEquals(2, evidence.uploadAttempts)
    }
}

private class FailingThenUploadingEvidence : EvidenceRepository {
    var uploadAttempts = 0

    override suspend fun getByRequirement(organizationId: String, serviceJobRequirementId: String): Evidence? = null

    override suspend fun listForJob(organizationId: String, serviceJobId: String): List<Evidence> = emptyList()

    override suspend fun upsert(evidence: Evidence) = Unit

    override suspend fun preparePhotoEvidence(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation?,
    ): Evidence = throw NotImplementedError()

    override suspend fun uploadPhotoEvidence(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        uploadAttempts += 1
        if (uploadAttempts == 1) {
            throw EvidenceFailure.Network
        }
        onProgress(100)
        return EvidenceSubmitResult(uploaded = true, pending = false)
    }

    override suspend fun retryPhotoUpload(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult = uploadPhotoEvidence(jobId, requirementId, onProgress)

    override suspend fun submitPhoto(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation?,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)

    override suspend fun submitChecklistCompletion(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)

    override suspend fun submitTimestamp(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)
}

private class FixedOrg : OrganizationGateway {
    override suspend fun currentMembership(): Membership {
        return Membership(organizationId = "org-1", organizationName = "Org", role = "cleaner", userId = "u")
    }

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        error("unused")
    }
}

private class NoOpExceptions : com.contractproof.data.ExceptionGateway {
    override suspend fun submit(
        jobId: String,
        requirementId: String,
        reason: String,
        recordedAt: String,
    ): com.contractproof.data.TaskExceptionResult {
        return com.contractproof.data.TaskExceptionResult(uploaded = true, pending = false)
    }
}

private class NoOpJobs : ServiceJobRepository {
    override suspend fun generateForApprovedVersion(contractId: String, contractVersionId: String, horizonDays: Int) =
        Unit

    override suspend fun get(jobId: String): com.contractproof.domain.ServiceJob? = null

    override suspend fun listAssignedOn(
        serviceDate: kotlinx.datetime.LocalDate,
        assigneeUserId: String,
    ): List<com.contractproof.domain.ServiceJob> = emptyList()

    override suspend fun listForOrganizationOn(
        serviceDate: kotlinx.datetime.LocalDate,
    ): List<com.contractproof.domain.ServiceJob> = emptyList()

    override suspend fun listForContract(
        contractId: String,
        fromDate: kotlinx.datetime.LocalDate,
        limit: Int,
    ): List<com.contractproof.domain.ServiceJob> = emptyList()

    override suspend fun start(jobId: String, startedAt: String): com.contractproof.domain.ServiceJob {
        error("unused")
    }

    override suspend fun complete(
        jobId: String,
        completedAt: String,
        completedBy: String,
    ): com.contractproof.domain.ServiceJob {
        error("unused")
    }

    override suspend fun markIncomplete(jobId: String): com.contractproof.domain.ServiceJob = error("unused")

    override suspend fun markDisputed(jobId: String): com.contractproof.domain.ServiceJob = error("unused")
}
