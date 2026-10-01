package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class ContractHealthRulesTest {
    @Test
    fun countsUpcomingOpenDisputesAndExceptions() {
        val contract = contractRecord(ContractRules.Active)
        val today = LocalDate(2026, 10, 1)
        val jobs = listOf(
            job("j1", contract.id, today, ServiceJobRules.Scheduled, coverage = 60),
            job("j2", contract.id, LocalDate(2026, 10, 5), ServiceJobRules.InProgress, coverage = 100, exceptions = 1),
            job("j3", contract.id, today, ServiceJobRules.Completed, coverage = 100),
        )
        val disputes = listOf(
            dispute("d1", "j1", DisputeStatus.Open),
            dispute("d2", "j3", DisputeStatus.Closed),
        )
        val jobMap = jobs.associate { it.id to it.contractId }
        val snapshot = ContractHealthRules.assemble(contract, jobs, disputes, jobMap, today)
        assertEquals(2, snapshot.upcomingCount)
        assertEquals(1, snapshot.openDisputeCount)
        assertEquals(1, snapshot.exceptionCount)
        assertEquals("2 upcoming · 1 open dispute · 1 exception", snapshot.listSubtitle)
    }

    @Test
    fun inactiveContractHasEmptyListSubtitle() {
        val contract = contractRecord(ContractRules.Draft)
        val snapshot = ContractHealthRules.assemble(contract, emptyList(), emptyList(), emptyMap(), LocalDate(2026, 10, 1))
        assertEquals("", snapshot.listSubtitle)
        assertEquals(false, snapshot.isActive)
    }

    private fun contractRecord(status: String): ContractRecord {
        return ContractRecord(
            id = "c-1",
            organizationId = "org-1",
            clientId = "client-1",
            locationId = "loc-1",
            title = "Cleaning",
            status = status,
            startsOn = "2026-01-01",
            endsOn = null,
            currentVersionId = "v1",
            documentPath = null,
        )
    }

    private fun job(
        id: String,
        contractId: String,
        date: LocalDate,
        status: String,
        coverage: Int,
        exceptions: Int = 0,
    ): ServiceJob {
        val requirements = (0 until exceptions).map { index ->
            ServiceJobRequirement(
                id = "req-$id-$index",
                contractRequirementId = "cr-$index",
                requirementText = "Task",
                requiresPhoto = true,
                isMandatory = true,
                sortOrder = index,
                status = JobRequirementStatus.Exception,
            )
        }
        return ServiceJob(
            id = id,
            organizationId = "org-1",
            client = ServiceJobClientRef("cl", "Client"),
            location = ServiceJobLocationRef("loc", "Lobby", "UTC"),
            serviceDate = date,
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("u1", "User"),
            status = status,
            requirements = requirements,
            evidenceStatus = ServiceJobEvidenceStatus(1, 1, coverage, ServiceJobEvidenceState.Partial),
            contractId = contractId,
            contractVersionId = "v1",
            scheduleId = "s1",
        )
    }

    private fun dispute(id: String, jobId: String, status: DisputeStatus): Dispute {
        return Dispute(
            id = id,
            organizationId = "org-1",
            clientId = "cl",
            clientName = "Client",
            locationId = "loc",
            locationName = "Lobby",
            serviceDate = LocalDate(2026, 10, 1),
            complaint = "Issue",
            serviceJobId = jobId,
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
