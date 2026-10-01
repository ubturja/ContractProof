package com.contractproof.integration

import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeAssemblyInput
import com.contractproof.domain.DisputeAssemblyRules
import com.contractproof.domain.DisputeContractRef
import com.contractproof.domain.DisputeItem
import com.contractproof.domain.DisputeOutcome
import com.contractproof.domain.DisputeReconstructionRules
import com.contractproof.domain.DisputeStatus
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFile
import com.contractproof.domain.EvidenceReportAssemblyRules
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRequirement
import com.contractproof.domain.ServiceJobRules
import com.contractproof.domain.JobExceptionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DisputeReportWorkflowTest {
    @Test
    fun disputeAssemblyReconstructionAndReportPreviewChain() {
        val job = sampleJob()
        val items = DisputeAssemblyRules.buildItems(
            DisputeAssemblyInput(
                job = job,
                uploadedEvidenceByRequirement = mapOf("r-satisfied" to "ev-1"),
                uploadedExceptionByRequirement = mapOf("r-exception" to "ex-1"),
            ),
        )
        assertEquals(3, items.size)
        assertEquals(DisputeOutcome.Missing, items.first { it.serviceJobRequirementId == "r-missing" }.outcome)

        val dispute = Dispute(
            id = "d1",
            organizationId = "org-1",
            clientId = "c",
            clientName = "Northstar",
            locationId = "l",
            locationName = "Dock",
            serviceDate = job.serviceDate,
            complaint = "Break room not sanitized.",
            serviceJobId = job.id,
            disputedServiceJobRequirementId = "r-exception",
            complaintAttachmentObjectPath = null,
            complaintAttachmentMimeType = null,
            recordedBy = "client",
            status = DisputeStatus.Open,
            syncStatus = EvidenceSyncStatus.Uploaded,
            createdAt = "2026-03-02T10:00:00Z",
        )
        val disputeItems = items.mapIndexed { index, draft ->
            DisputeItem(
                id = "item-$index",
                disputeId = dispute.id,
                serviceJobId = job.id,
                serviceJobRequirementId = draft.serviceJobRequirementId,
                evidenceRecordId = draft.evidenceRecordId,
                exceptionId = draft.exceptionId,
                outcome = draft.outcome,
                createdAt = dispute.createdAt,
            )
        }
        val evidence = listOf(
            Evidence(
                id = "ev-1",
                organizationId = "org-1",
                serviceJobId = job.id,
                serviceJobRequirementId = "r-satisfied",
                capturedByUserId = "u",
                capturedAt = "2026-03-01T09:00:00Z",
                type = EvidenceType.Photo,
                location = null,
                file = EvidenceFile(
                    id = "file-1",
                    evidenceRecordId = "ev-1",
                    bucket = "evidence",
                    objectPath = "org/job/r/ev-1",
                    mimeType = "image/jpeg",
                    byteSize = 100,
                    sha256 = "a".repeat(64),
                    syncStatus = EvidenceSyncStatus.Uploaded,
                    uploadedAt = "2026-03-01T09:01:00Z",
                ),
                syncStatus = EvidenceSyncStatus.Uploaded,
            ),
        )
        val exceptions = listOf(
            JobExceptionRecord(
                id = "ex-1",
                serviceJobRequirementId = "r-exception",
                reason = "Area blocked",
                recordedAt = "2026-03-01T09:30:00Z",
                recordedBy = "u",
                syncStatus = EvidenceSyncStatus.Uploaded,
            ),
        )
        val bundle = DisputeReconstructionRules.build(
            dispute = dispute,
            items = disputeItems,
            job = job,
            disputedRequirementText = "Sanitize break room",
            contract = DisputeContractRef("ct", "Weekly service", "v1"),
            evidence = evidence,
            exceptions = exceptions,
        )
        assertTrue(bundle.timeline.isNotEmpty())

        val report = EvidenceReportAssemblyRules.build(bundle)
        assertEquals("Northstar", report.header.clientName)
        assertEquals("Sanitize break room", report.header.disputedRequirementText)
        assertEquals(1, report.evidence.size)
        assertEquals(1, report.exceptions.size)
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c", "Northstar"),
            location = ServiceJobLocationRef("l", "Dock", "UTC"),
            serviceDate = LocalDate(2026, 3, 1),
            scheduledStart = "2026-03-01T08:00:00Z",
            scheduledEnd = "2026-03-01T10:00:00Z",
            assignee = ServiceJobAssignee("u", "Cleaner"),
            status = ServiceJobRules.Disputed,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "r-satisfied",
                    contractRequirementId = "cr1",
                    requirementText = "Sweep dock",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Satisfied,
                ),
                ServiceJobRequirement(
                    id = "r-exception",
                    contractRequirementId = "cr2",
                    requirementText = "Sanitize break room",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 1,
                    status = JobRequirementStatus.Exception,
                ),
                ServiceJobRequirement(
                    id = "r-missing",
                    contractRequirementId = "cr3",
                    requirementText = "Empty bins",
                    requiresPhoto = false,
                    isMandatory = false,
                    sortOrder = 2,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(2, 1, 50, ServiceJobEvidenceState.ReadyToComplete),
            contractId = "ct",
            contractVersionId = "cv",
            scheduleId = null,
            startedAt = "2026-03-01T08:05:00Z",
            completedAt = "2026-03-01T09:45:00Z",
        )
    }
}
