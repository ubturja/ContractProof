package com.contractproof.data.sync

import com.contractproof.data.Membership
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.local.ContractProofTestDatabase
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.local.SqlDelightSyncMetadataStore
import com.contractproof.data.local.SqlDelightSyncQueueStore
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.EvidenceSubmitResult
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.SyncItemKind
import com.contractproof.data.ExceptionGateway
import com.contractproof.data.TaskExceptionResult
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceLocation
import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.domain.EvidenceFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class SyncCoordinatorTest {
    @Test
    fun drainRemovesUploadedEvidenceItem() = runBlocking {
        val database = ContractProofTestDatabase.create()
        val queue = SqlDelightSyncQueueStore(database)
        val coordinator = SyncCoordinator(
            organizations = FixedOrg(),
            queue = queue,
            metadata = SqlDelightSyncMetadataStore(database),
            evidence = UploadingEvidence(),
            exceptions = NoOpExceptions(),
            serviceJobs = NoOpJobs(),
            exceptionStore = SqlDelightExceptionStore(database),
        )
        coordinator.enqueue(SyncItemKind.EvidenceUpload, "job-1", "req-1")
        coordinator.drain()
        assertNull(queue.findByDedupeKey("evidence:req-1"))
    }

    @Test
    fun drainRetriesAfterUploadFailure() = runBlocking {
        val database = ContractProofTestDatabase.create()
        val queue = SqlDelightSyncQueueStore(database)
        val failing = FailingThenUploadingEvidence()
        val coordinator = SyncCoordinator(
            organizations = FixedOrg(),
            queue = queue,
            metadata = SqlDelightSyncMetadataStore(database),
            evidence = failing,
            exceptions = NoOpExceptions(),
            serviceJobs = NoOpJobs(),
            exceptionStore = SqlDelightExceptionStore(database),
            clock = { kotlinx.datetime.Instant.fromEpochMilliseconds(1_000_000) },
        )
        coordinator.enqueue(SyncItemKind.EvidenceUpload, "job-1", "req-1")
        coordinator.drain()
        val failed = queue.findByDedupeKey("evidence:req-1")
        assertNotNull(failed)
        assertEquals(EvidenceSyncStatus.Failed, failed.syncStatus)
        assertEquals(1, failed.attemptCount)
        queue.resetForImmediateRetry("evidence:req-1", "1970-01-01T00:16:40Z")
        coordinator.drain()
        assertNull(queue.findByDedupeKey("evidence:req-1"))
    }

    private class FailingThenUploadingEvidence : EvidenceRepository {
        private var attempts = 0

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
            attempts += 1
            if (attempts == 1) {
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
            return Membership(
                organizationId = "org-1",
                organizationName = "Northside",
                role = "cleaner",
                userId = "user-1",
            )
        }

        override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
            throw OrganizationFailure.Rejected
        }
    }

    private class UploadingEvidence : EvidenceRepository {
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
            onProgress(100)
            return EvidenceSubmitResult(uploaded = true, pending = false)
        }

        override suspend fun retryPhotoUpload(
            jobId: String,
            requirementId: String,
            onProgress: (Int) -> Unit,
        ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)

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

    private class NoOpExceptions : ExceptionGateway {
        override suspend fun submit(
            jobId: String,
            requirementId: String,
            reason: String,
            recordedAt: String,
        ): TaskExceptionResult = TaskExceptionResult(uploaded = true, pending = false)
    }

    private class NoOpJobs : ServiceJobRepository {
        override suspend fun generateForApprovedVersion(
            contractId: String,
            contractVersionId: String,
            horizonDays: Int,
        ) = Unit

        override suspend fun get(jobId: String) = null

        override suspend fun listAssignedOn(
            serviceDate: kotlinx.datetime.LocalDate,
            assigneeUserId: String,
        ) = emptyList<com.contractproof.domain.ServiceJob>()

        override suspend fun listForOrganizationOn(serviceDate: kotlinx.datetime.LocalDate) =
            emptyList<com.contractproof.domain.ServiceJob>()

        override suspend fun listForContract(
            contractId: String,
            fromDate: kotlinx.datetime.LocalDate,
            limit: Int,
        ) = emptyList<com.contractproof.domain.ServiceJob>()

        override suspend fun start(jobId: String, startedAt: String) =
            throw com.contractproof.data.ServiceJobFailure.Rejected

        override suspend fun complete(jobId: String, completedAt: String, completedBy: String) =
            throw com.contractproof.data.ServiceJobFailure.Rejected

        override suspend fun markIncomplete(jobId: String) =
            throw com.contractproof.data.ServiceJobFailure.Rejected

        override suspend fun markDisputed(jobId: String) =
            throw com.contractproof.data.ServiceJobFailure.Rejected
    }
}
