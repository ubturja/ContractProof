package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DisputeReconstructionRulesTest {
    @Test
    fun timelineIncludesFilingAndMissing() {
        val job = ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c", "Client"),
            location = ServiceJobLocationRef("l", "Loc", "UTC"),
            serviceDate = LocalDate(2026, 3, 1),
            scheduledStart = "2026-03-01T08:00:00Z",
            scheduledEnd = "2026-03-01T09:00:00Z",
            assignee = ServiceJobAssignee("u", "Alex"),
            status = ServiceJobRules.Disputed,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "r1",
                    contractRequirementId = "cr",
                    requirementText = "Mop floors",
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
            startedAt = "2026-03-01T08:10:00Z",
            completedAt = "2026-03-01T08:55:00Z",
        )
        val dispute = Dispute(
            id = "d1",
            organizationId = "org-1",
            clientId = "c",
            clientName = "Client",
            locationId = "l",
            locationName = "Loc",
            serviceDate = job.serviceDate,
            complaint = "Floors still dirty.",
            serviceJobId = job.id,
            disputedServiceJobRequirementId = "r1",
            complaintAttachmentObjectPath = null,
            complaintAttachmentMimeType = null,
            recordedBy = "owner",
            status = DisputeStatus.Open,
            syncStatus = EvidenceSyncStatus.Uploaded,
            createdAt = "2026-03-02T10:00:00Z",
        )
        val bundle = DisputeReconstructionRules.build(
            dispute = dispute,
            items = listOf(
                DisputeItem(
                    id = "i1",
                    disputeId = "d1",
                    serviceJobId = job.id,
                    serviceJobRequirementId = "r1",
                    evidenceRecordId = null,
                    exceptionId = null,
                    outcome = DisputeOutcome.Missing,
                    createdAt = dispute.createdAt,
                ),
            ),
            job = job,
            disputedRequirementText = "Mop floors",
            contract = DisputeContractRef("ct", "Cleaning", "v1"),
            evidence = emptyList(),
            exceptions = emptyList(),
        )
        assertTrue(bundle.timeline.any { it.category == DisputeTimelineCategory.DisputeFiled })
        assertTrue(bundle.missingRequirementLabels.contains("Mop floors"))
    }
}
