package com.contractproof.feature.dashboard

import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeStatus
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class DashboardControllerTest {
    @Test
    fun refreshBuildsCountsFromJobsAndDisputes() = runBlocking {
        val organizations = OwnerOrganizationGateway()
        val jobs = MemoryOrgJobRepository(
            listOf(
                sampleJob("j1", ServiceJobRules.Scheduled, coverage = 50),
                sampleJob("j2", ServiceJobRules.Completed, coverage = 100),
            ),
        )
        val disputes = MemoryDisputeGateway(
            listOf(
                sampleDispute("d1", DisputeStatus.Open),
            ),
        )
        val controller = DashboardController(organizations, jobs, disputes)
        controller.refresh()
        assertEquals(2, controller.state.value.todayJobs.size)
        assertEquals(1, controller.state.value.openDisputes.size)
        assertTrue(controller.state.value.needsAttention)
    }

    @Test
    fun cleanerRoleCannotOpenDashboard() = runBlocking {
        val organizations = object : OrganizationGateway {
            override suspend fun currentMembership(): Membership {
                return Membership(
                    organizationId = "org-1",
                    organizationName = "Northside",
                    role = "cleaner",
                    userId = "u1",
                )
            }

            override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
                throw OrganizationFailure.Rejected
            }
        }
        val controller = DashboardController(organizations, MemoryOrgJobRepository(), MemoryDisputeGateway())
        controller.refresh()
        assertEquals("Dashboard is not available for your role.", controller.state.value.banner)
    }

    private fun sampleJob(id: String, status: String, coverage: Int): ServiceJob {
        return ServiceJob(
            id = id,
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("u1", "Owner"),
            status = status,
            requirements = emptyList(),
            evidenceStatus = ServiceJobEvidenceStatus(2, coverage / 50, coverage, ServiceJobEvidenceState.Partial),
            contractId = "c-1",
            contractVersionId = "v-1",
            scheduleId = "s-1",
        )
    }

    private fun sampleDispute(id: String, status: DisputeStatus): Dispute {
        return Dispute(
            id = id,
            organizationId = "org-1",
            clientId = "c1",
            clientName = "Acme",
            locationId = "l1",
            locationName = "Lobby",
            serviceDate = LocalDate(2026, 10, 1),
            complaint = "Issue",
            serviceJobId = "j1",
            disputedServiceJobRequirementId = null,
            complaintAttachmentObjectPath = null,
            complaintAttachmentMimeType = null,
            recordedBy = "u1",
            status = status,
            syncStatus = EvidenceSyncStatus.Uploaded,
            createdAt = "2026-10-01T12:00:00Z",
        )
    }
}

private class OwnerOrganizationGateway : OrganizationGateway {
    override suspend fun currentMembership(): Membership {
        return Membership(
            organizationId = "org-1",
            organizationName = "Northside",
            role = "owner",
            userId = "owner-1",
        )
    }

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class MemoryOrgJobRepository(
    private val items: List<ServiceJob> = emptyList(),
    private val failWith: ServiceJobFailure? = null,
) : ServiceJobRepository {
    override suspend fun generateForApprovedVersion(contractId: String, contractVersionId: String, horizonDays: Int) = Unit

    override suspend fun get(jobId: String): ServiceJob? = items.find { it.id == jobId }

    override suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob> = emptyList()

    override suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob> {
        failWith?.let { throw it }
        return items
    }

    override suspend fun listForContract(contractId: String, fromDate: LocalDate, limit: Int): List<ServiceJob> = emptyList()

    override suspend fun start(jobId: String, startedAt: String): ServiceJob = throw ServiceJobFailure.Rejected

    override suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob =
        throw ServiceJobFailure.Rejected

    override suspend fun markIncomplete(jobId: String): ServiceJob = throw ServiceJobFailure.Rejected

    override suspend fun markDisputed(jobId: String): ServiceJob = throw ServiceJobFailure.Rejected
}

private class MemoryDisputeGateway(
    private val items: List<Dispute> = emptyList(),
) : DisputeGateway {
    override suspend fun list(): List<Dispute> = items

    override suspend fun get(id: String): Dispute? = items.find { it.id == id }

    override suspend fun listDisputableJobs(): List<ServiceJob> = emptyList()

    override suspend fun create(draft: com.contractproof.domain.DisputeDraft): Dispute {
        throw DisputeFailure.Rejected("Rejected")
    }

    override suspend fun getReconstruction(disputeId: String): com.contractproof.domain.DisputeReconstructionBundle? = null

    override suspend fun requestSummary(disputeId: String): com.contractproof.domain.DisputeAiSummary {
        throw DisputeFailure.Rejected("Rejected")
    }
}
