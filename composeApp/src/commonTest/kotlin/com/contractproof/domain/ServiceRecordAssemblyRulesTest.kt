package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class ServiceRecordAssemblyRulesTest {
    @Test
    fun assemblesTasksEvidenceExceptionsAndAcknowledgements() {
        val job = ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Client"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("u1", "Worker"),
            status = ServiceJobRules.Completed,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "req-1",
                    contractRequirementId = "cr-1",
                    requirementText = "Vacuum",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Satisfied,
                ),
                ServiceJobRequirement(
                    id = "req-2",
                    contractRequirementId = "cr-2",
                    requirementText = "Mop",
                    requiresPhoto = false,
                    isMandatory = true,
                    sortOrder = 1,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(2, 1, 50, ServiceJobEvidenceState.Partial),
            contractId = "contract-1",
            contractVersionId = "v1",
            scheduleId = "s1",
        )
        val evidence = listOf(
            Evidence(
                id = "ev-1",
                organizationId = "org-1",
                serviceJobId = "job-1",
                serviceJobRequirementId = "req-1",
                capturedByUserId = "u1",
                capturedAt = "2026-10-01T09:30:00Z",
                type = EvidenceType.ChecklistCompletion,
                location = null,
                file = null,
                syncStatus = EvidenceSyncStatus.Uploaded,
            ),
        )
        val exceptions = listOf(
            JobExceptionRecord(
                id = "ex-1",
                serviceJobRequirementId = "req-2",
                reason = "Area blocked",
                recordedAt = "2026-10-01T09:45:00Z",
                recordedBy = "u1",
                syncStatus = EvidenceSyncStatus.Uploaded,
            ),
        )
        val snapshot = ServiceRecordAssemblyRules.assemble(job, evidence, exceptions)
        assertEquals("Lobby", snapshot.locationName)
        assertEquals(1, snapshot.acknowledgements.size)
        assertEquals(1, snapshot.exceptionLines.size)
        assertTrue(snapshot.missingMandatoryLabels.contains("Mop"))
    }
}
