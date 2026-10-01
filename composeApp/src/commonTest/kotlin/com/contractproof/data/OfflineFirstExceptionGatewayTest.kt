package com.contractproof.data

import com.contractproof.data.local.ContractProofTestDatabase
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.local.SqlDelightSyncMetadataStore
import com.contractproof.data.local.SqlDelightSyncQueueStore
import com.contractproof.data.sync.SyncCoordinator
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.SyncItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class OfflineFirstExceptionGatewayTest {
    @Test
    fun submitQueuesLocallyWhenRemoteIsOffline() = runBlocking {
        val database = ContractProofTestDatabase.create()
        val local = SqlDelightExceptionStore(database)
        val queue = SqlDelightSyncQueueStore(database)
        val coordinator = SyncCoordinator(
            organizations = FixedOrg(),
            queue = queue,
            metadata = SqlDelightSyncMetadataStore(database),
            evidence = NoOpEvidence(),
            exceptions = NetworkRemote(),
            serviceJobs = NoOpJobs(),
            exceptionStore = local,
        )
        val gateway = OfflineFirstExceptionGateway(
            remote = NetworkRemote(),
            organizations = FixedOrg(),
            auth = FixedAuth(),
            local = local,
            syncCoordinator = coordinator,
        )
        val result = gateway.submit(
            jobId = "job-1",
            requirementId = "req-1",
            reason = "Area blocked",
            recordedAt = "2026-10-01T12:00:00Z",
        )
        assertFalse(result.uploaded)
        assertTrue(result.pending)
        val stored = local.getByRequirement("org-1", "req-1")
        assertEquals(EvidenceSyncStatus.Pending, stored?.syncStatus)
        assertTrue(queue.findByDedupeKey("exception:req-1") != null)
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

    private class FixedAuth : AuthGateway {
        override suspend fun hasStoredSession(): Boolean = true

        override suspend fun currentUser(): AuthUser {
            return AuthUser(id = "user-1", email = "cleaner@example.com")
        }

        override suspend fun signIn(email: String, password: String) = Unit

        override suspend fun signUp(email: String, password: String): Boolean = true

        override suspend fun signOut() = Unit

        override suspend fun requestPasswordReset(email: String) = Unit
    }

    private class NetworkRemote : ExceptionGateway {
        override suspend fun submit(
            jobId: String,
            requirementId: String,
            reason: String,
            recordedAt: String,
        ): TaskExceptionResult {
            throw ExceptionFailure.Network
        }
    }

    private class NoOpEvidence : com.contractproof.domain.EvidenceRepository {
        override suspend fun getByRequirement(organizationId: String, serviceJobRequirementId: String) = null
        override suspend fun listForJob(organizationId: String, serviceJobId: String) =
            emptyList<com.contractproof.domain.Evidence>()
        override suspend fun upsert(evidence: com.contractproof.domain.Evidence) = Unit
        override suspend fun preparePhotoEvidence(
            jobId: String,
            requirementId: String,
            photo: com.contractproof.core.platform.CapturedPhoto,
            capturedAt: String,
            location: com.contractproof.domain.EvidenceLocation?,
        ) = throw NotImplementedError()
        override suspend fun uploadPhotoEvidence(jobId: String, requirementId: String, onProgress: (Int) -> Unit) =
            com.contractproof.domain.EvidenceSubmitResult(uploaded = true, pending = false)
        override suspend fun retryPhotoUpload(jobId: String, requirementId: String, onProgress: (Int) -> Unit) =
            com.contractproof.domain.EvidenceSubmitResult(uploaded = true, pending = false)
        override suspend fun submitPhoto(
            jobId: String,
            requirementId: String,
            photo: com.contractproof.core.platform.CapturedPhoto,
            capturedAt: String,
            location: com.contractproof.domain.EvidenceLocation?,
            onProgress: (Int) -> Unit,
        ) = com.contractproof.domain.EvidenceSubmitResult(uploaded = true, pending = false)
        override suspend fun submitChecklistCompletion(
            jobId: String,
            requirementId: String,
            capturedAt: String,
            location: com.contractproof.domain.EvidenceLocation?,
        ) = com.contractproof.domain.EvidenceSubmitResult(uploaded = true, pending = false)
        override suspend fun submitTimestamp(
            jobId: String,
            requirementId: String,
            capturedAt: String,
            location: com.contractproof.domain.EvidenceLocation?,
        ) = com.contractproof.domain.EvidenceSubmitResult(uploaded = true, pending = false)
    }

    private class NoOpJobs : com.contractproof.domain.ServiceJobRepository {
        override suspend fun generateForApprovedVersion(contractId: String, contractVersionId: String, horizonDays: Int) = Unit
        override suspend fun get(jobId: String) = null
        override suspend fun listAssignedOn(serviceDate: kotlinx.datetime.LocalDate, assigneeUserId: String) =
            emptyList<com.contractproof.domain.ServiceJob>()
        override suspend fun listForOrganizationOn(serviceDate: kotlinx.datetime.LocalDate) =
            emptyList<com.contractproof.domain.ServiceJob>()
        override suspend fun listForContract(contractId: String, fromDate: kotlinx.datetime.LocalDate, limit: Int) =
            emptyList<com.contractproof.domain.ServiceJob>()
        override suspend fun start(jobId: String, startedAt: String) = throw ServiceJobFailure.Rejected
        override suspend fun complete(jobId: String, completedAt: String, completedBy: String) = throw ServiceJobFailure.Rejected
        override suspend fun markIncomplete(jobId: String) = throw ServiceJobFailure.Rejected
        override suspend fun markDisputed(jobId: String) = throw ServiceJobFailure.Rejected
    }
}
