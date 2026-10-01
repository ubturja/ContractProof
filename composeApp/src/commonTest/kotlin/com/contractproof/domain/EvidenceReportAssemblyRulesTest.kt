package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class EvidenceReportAssemblyRulesTest {
    @Test
    fun refsCaptureAllEntityIds() {
        val bundle = sampleBundle()
        val report = EvidenceReportAssemblyRules.build(bundle)
        assertEquals("d1", report.refs.disputeId)
        assertEquals("job-1", report.refs.serviceJobId)
        assertEquals("ct", report.refs.contractId)
        assertTrue(report.refs.disputeItemIds.contains("i1"))
        assertTrue(report.refs.evidenceRecordIds.contains("ev1"))
        assertTrue(report.refs.exceptionIds.contains("ex1"))
    }

    @Test
    fun coverageListsMissingOutcome() {
        val report = EvidenceReportAssemblyRules.build(sampleBundle())
        val missing = report.coverage.filter { it.outcome == DisputeOutcome.Missing }
        assertEquals(1, missing.size)
        assertEquals("Mop floors", missing.first().requirementText)
    }

    @Test
    fun sectionsPopulated() {
        val report = EvidenceReportAssemblyRules.build(sampleBundle())
        assertTrue(report.contractRequirements.isNotEmpty())
        assertTrue(report.timeline.isNotEmpty())
        assertEquals(1, report.evidence.size)
        assertEquals(1, report.exceptions.size)
        assertTrue(report.acknowledgements.any { it.label == "Dispute filed" })
    }

    private fun sampleBundle(): DisputeReconstructionBundle {
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
                ServiceJobRequirement(
                    id = "r2",
                    contractRequirementId = "cr2",
                    requirementText = "Vacuum",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 1,
                    status = JobRequirementStatus.Satisfied,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(2, 1, 50, ServiceJobEvidenceState.Partial),
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
        val evidence = Evidence(
            id = "ev1",
            organizationId = "org-1",
            serviceJobId = job.id,
            serviceJobRequirementId = "r2",
            capturedByUserId = "u",
            capturedAt = "2026-03-01T08:30:00Z",
            type = EvidenceType.Photo,
            location = null,
            file = EvidenceFile(
                id = "f1",
                evidenceRecordId = "ev1",
                bucket = "evidence",
                objectPath = "org-1/job-1/r2/ev1",
                mimeType = "image/jpeg",
                byteSize = 100,
                sha256 = "abc",
                syncStatus = EvidenceSyncStatus.Uploaded,
                uploadedAt = "2026-03-01T08:31:00Z",
            ),
            syncStatus = EvidenceSyncStatus.Uploaded,
        )
        val exception = JobExceptionRecord(
            id = "ex1",
            serviceJobRequirementId = "r2",
            reason = "Blocked area",
            recordedAt = "2026-03-01T08:20:00Z",
            recordedBy = "u",
            syncStatus = EvidenceSyncStatus.Uploaded,
        )
        return DisputeReconstructionRules.build(
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
                DisputeItem(
                    id = "i2",
                    disputeId = "d1",
                    serviceJobId = job.id,
                    serviceJobRequirementId = "r2",
                    evidenceRecordId = "ev1",
                    exceptionId = null,
                    outcome = DisputeOutcome.Satisfied,
                    createdAt = dispute.createdAt,
                ),
            ),
            job = job,
            disputedRequirementText = "Mop floors",
            contract = DisputeContractRef("ct", "Cleaning", "v1"),
            evidence = listOf(evidence),
            exceptions = listOf(exception),
        )
    }
}
