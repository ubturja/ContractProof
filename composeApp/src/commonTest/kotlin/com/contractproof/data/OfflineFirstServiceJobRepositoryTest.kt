package com.contractproof.data

import com.contractproof.data.local.ContractProofTestDatabase
import com.contractproof.data.local.SqlDelightServiceJobStore
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
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class OfflineFirstServiceJobRepositoryTest {
    @Test
    fun returnsCachedJobsWhenRemoteIsOffline() = runBlocking {
        val local = SqlDelightServiceJobStore(ContractProofTestDatabase.create())
        val cached = sampleJob()
        local.upsert(cached)
        val repository = OfflineFirstServiceJobRepository(
            remote = FailingRemote(),
            local = local,
            organizations = FixedOrg(),
        )
        val jobs = repository.listAssignedOn(LocalDate(2026, 10, 1), "user-1")
        assertEquals(1, jobs.size)
        assertEquals("job-1", jobs.first().id)
    }

    private class FailingRemote : com.contractproof.domain.ServiceJobRepository {
        override suspend fun generateForApprovedVersion(
            contractId: String,
            contractVersionId: String,
            horizonDays: Int,
        ) = Unit

        override suspend fun get(jobId: String): ServiceJob? {
            throw ServiceJobFailure.Network
        }

        override suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob> {
            throw ServiceJobFailure.Network
        }

        override suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob> = emptyList()

        override suspend fun listForContract(contractId: String, fromDate: LocalDate, limit: Int): List<ServiceJob> =
            emptyList()

        override suspend fun start(jobId: String, startedAt: String): ServiceJob {
            throw ServiceJobFailure.Network
        }

        override suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob {
            throw ServiceJobFailure.Network
        }

        override suspend fun markIncomplete(jobId: String): ServiceJob {
            throw ServiceJobFailure.Rejected
        }

        override suspend fun markDisputed(jobId: String): ServiceJob {
            throw ServiceJobFailure.Rejected
        }
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
            status = ServiceJobRules.Scheduled,
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
