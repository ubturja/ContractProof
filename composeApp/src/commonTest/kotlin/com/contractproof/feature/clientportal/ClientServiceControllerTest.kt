package com.contractproof.feature.clientportal

import com.contractproof.data.ClientServiceGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRules
import com.contractproof.domain.ServiceRecordSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class ClientServiceControllerTest {
    @Test
    fun refreshListMapsJobsForClient() = runBlocking {
        val job = sampleJob()
        val controller = ClientServiceController(
            ClientOrganizationGateway(),
            FakeClientServiceGateway(jobs = listOf(job)),
        )
        controller.refreshList()
        assertEquals(1, controller.listState.value.items.size)
        assertEquals("Lobby", controller.listState.value.items.single().locationName)
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Client"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("u1", "Worker"),
            status = ServiceJobRules.Completed,
            requirements = emptyList(),
            evidenceStatus = ServiceJobEvidenceStatus(0, 0, 100, ServiceJobEvidenceState.ReadyToComplete),
            contractId = "c-1",
            contractVersionId = "v-1",
            scheduleId = "s-1",
        )
    }
}

private class ClientOrganizationGateway : OrganizationGateway {
    override suspend fun currentMembership(): Membership {
        return Membership(
            organizationId = "org-1",
            organizationName = "Acme",
            role = "client",
            clientId = "c1",
            userId = "client-user",
        )
    }

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class FakeClientServiceGateway(
    private val jobs: List<ServiceJob> = emptyList(),
) : ClientServiceGateway {
    override suspend fun listCompletedForClient(): List<ServiceJob> = jobs

    override suspend fun loadRecord(jobId: String): ServiceRecordSnapshot? = null
}
