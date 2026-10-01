package com.contractproof.feature.dispute

import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeAiSummary
import com.contractproof.domain.DisputeDraft
import com.contractproof.domain.DisputeReconstructionBundle
import com.contractproof.domain.DisputeStatus
import com.contractproof.domain.EvidenceSyncStatus
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
import com.contractproof.core.analytics.NoOpProductAnalytics
import com.contractproof.core.observability.NoOpErrorReporter
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class DisputeCreateControllerTest {
    @Test
    fun submitCallsGateway() = runBlocking {
        val gateway = FakeDisputeGateway()
        val controller = DisputeCreateController(
            FakeOrganizations(),
            gateway,
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        gateway.jobs = listOf(sampleJob())
        controller.load()
        controller.selectJob("job-1")
        controller.selectRequirement("r1")
        controller.updateComplaint("Issue noted.")
        controller.updateServiceDate("2026-01-15")
        var createdId: String? = null
        controller.submit { createdId = it }
        assertEquals("dispute-1", createdId)
        assertEquals(1, gateway.createCount)
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c", "Client"),
            location = ServiceJobLocationRef("l", "Loc", "UTC"),
            serviceDate = LocalDate(2026, 1, 15),
            scheduledStart = "2026-01-15T09:00:00Z",
            scheduledEnd = "2026-01-15T10:00:00Z",
            assignee = ServiceJobAssignee("u", "Worker"),
            status = ServiceJobRules.Completed,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "r1",
                    contractRequirementId = "cr",
                    requirementText = "Clean",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.NotStarted),
            contractId = "ct",
            contractVersionId = "cv",
            scheduleId = null,
        )
    }

    private class FakeOrganizations : OrganizationGateway {
        override suspend fun currentMembership(): Membership? {
            return Membership(
                organizationId = "org-1",
                organizationName = "Org",
                userId = "u",
                role = "owner",
                locationIds = emptyList(),
            )
        }

        override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
            error("not used")
        }
    }

    private class FakeDisputeGateway : DisputeGateway {
        var jobs: List<ServiceJob> = emptyList()
        var createCount = 0

        override suspend fun list(): List<Dispute> = emptyList()

        override suspend fun get(id: String): Dispute? = null

        override suspend fun listDisputableJobs(): List<ServiceJob> = jobs

        override suspend fun create(draft: DisputeDraft): Dispute {
            createCount++
            return Dispute(
                id = "dispute-1",
                organizationId = "org-1",
                clientId = "c",
                clientName = "Client",
                locationId = "l",
                locationName = "Loc",
                serviceDate = draft.serviceDate,
                complaint = draft.complaint,
                serviceJobId = draft.serviceJobId,
                disputedServiceJobRequirementId = draft.disputedServiceJobRequirementId,
                complaintAttachmentObjectPath = null,
                complaintAttachmentMimeType = null,
                recordedBy = "u",
                status = DisputeStatus.Open,
                syncStatus = EvidenceSyncStatus.Uploaded,
                createdAt = "2026-01-16T00:00:00Z",
            )
        }

        override suspend fun getReconstruction(disputeId: String): DisputeReconstructionBundle? = null

        override suspend fun requestSummary(disputeId: String): DisputeAiSummary {
            throw DisputeFailure.Rejected("not implemented")
        }
    }
}
