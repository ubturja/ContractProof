package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DashboardAssemblyRulesTest {
    @Test
    fun completionCountsNonCancelledJobs() {
        val jobs = listOf(
            job("j1", ServiceJobRules.Completed, evidencePercent = 100),
            job("j2", ServiceJobRules.InProgress, evidencePercent = 50),
            job("j3", ServiceJobRules.Cancelled, evidencePercent = 0),
        )
        val snapshot = DashboardAssemblyRules.assemble(jobs, emptyList())
        assertEquals("1 of 2 services complete today", snapshot.completion.label)
    }

    @Test
    fun openDisputesFilterAndSort() {
        val disputes = listOf(
            dispute("d1", DisputeStatus.Closed, createdAt = "2026-10-01T10:00:00Z"),
            dispute("d2", DisputeStatus.Open, createdAt = "2026-10-02T10:00:00Z"),
            dispute("d3", DisputeStatus.Open, createdAt = "2026-10-03T10:00:00Z"),
        )
        val snapshot = DashboardAssemblyRules.assemble(emptyList(), disputes)
        assertEquals(2, snapshot.openDisputes.size)
        assertEquals("d3", snapshot.openDisputes.first().disputeId)
    }

    @Test
    fun activityOrdersByTimestamp() {
        val jobs = listOf(
            job(
                "j1",
                ServiceJobRules.Completed,
                startedAt = "2026-10-01T08:00:00Z",
                completedAt = "2026-10-01T09:00:00Z",
            ),
        )
        val disputes = listOf(
            dispute("d1", DisputeStatus.Open, createdAt = "2026-10-01T12:00:00Z"),
        )
        val snapshot = DashboardAssemblyRules.assemble(jobs, disputes)
        assertEquals(DashboardActivityKind.DisputeOpened, snapshot.recentActivity.first().kind)
    }

    @Test
    fun needsAttentionWhenGapsOrDisputesOrIncompleteServices() {
        val gapJob = job("j1", ServiceJobRules.Scheduled, evidencePercent = 40, evidenceState = ServiceJobEvidenceState.Partial)
        val snapshotGaps = DashboardAssemblyRules.assemble(listOf(gapJob), emptyList())
        assertTrue(snapshotGaps.needsAttention)

        val openDispute = dispute("d1", DisputeStatus.Open, createdAt = "2026-10-01T12:00:00Z")
        val snapshotDispute = DashboardAssemblyRules.assemble(emptyList(), listOf(openDispute))
        assertTrue(snapshotDispute.needsAttention)

        val allDone = listOf(job("j1", ServiceJobRules.Completed, evidencePercent = 100))
        val snapshotOk = DashboardAssemblyRules.assemble(allDone, emptyList())
        assertFalse(snapshotOk.needsAttention)
    }

    private fun job(
        id: String,
        status: String,
        evidencePercent: Int = 100,
        evidenceState: ServiceJobEvidenceState = ServiceJobEvidenceState.ReadyToComplete,
        startedAt: String? = null,
        completedAt: String? = null,
    ): ServiceJob {
        val mandatoryTotal = 2
        val satisfied = (mandatoryTotal * evidencePercent) / 100
        return ServiceJob(
            id = id,
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("u1", "Cleaner"),
            status = status,
            requirements = emptyList(),
            evidenceStatus = ServiceJobEvidenceStatus(
                mandatoryTotal = mandatoryTotal,
                mandatorySatisfied = satisfied,
                coveragePercent = evidencePercent,
                state = evidenceState,
            ),
            contractId = "contract-1",
            contractVersionId = "v1",
            scheduleId = "s1",
            startedAt = startedAt,
            completedAt = completedAt,
        )
    }

    private fun dispute(id: String, status: DisputeStatus, createdAt: String): Dispute {
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
            createdAt = createdAt,
        )
    }
}
