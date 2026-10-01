package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DisputeAssemblyRulesTest {
    @Test
    fun buildsItemPerRequirement() {
        val job = sampleJob(
            requirements = listOf(
                req("r1", JobRequirementStatus.Satisfied, 0),
                req("r2", JobRequirementStatus.Missing, 1),
                req("r3", JobRequirementStatus.Exception, 2),
            ),
        )
        val items = DisputeAssemblyRules.buildItems(
            DisputeAssemblyInput(
                job = job,
                uploadedEvidenceByRequirement = mapOf("r1" to "ev-1"),
                uploadedExceptionByRequirement = mapOf("r3" to "ex-1"),
            ),
        )
        assertEquals(3, items.size)
        assertEquals(DisputeOutcome.Satisfied, items[0].outcome)
        assertEquals("ev-1", items[0].evidenceRecordId)
        assertEquals(DisputeOutcome.Missing, items[1].outcome)
        assertEquals(DisputeOutcome.Exception, items[2].outcome)
        assertEquals("ex-1", items[2].exceptionId)
    }

    @Test
    fun satisfiedWithoutUploadedEvidenceFails() {
        val job = sampleJob(listOf(req("r1", JobRequirementStatus.Satisfied, 0)))
        val error = runCatching {
            DisputeAssemblyRules.buildItems(
                DisputeAssemblyInput(job, emptyMap(), emptyMap()),
            )
        }.exceptionOrNull()
        assertTrue(error is DisputeRuleViolation)
    }

    private fun req(id: String, status: JobRequirementStatus, order: Int): ServiceJobRequirement {
        return ServiceJobRequirement(
            id = id,
            contractRequirementId = "c-$id",
            requirementText = "Task $id",
            requiresPhoto = true,
            isMandatory = true,
            sortOrder = order,
            status = status,
        )
    }

    private fun sampleJob(requirements: List<ServiceJobRequirement>): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("client-1", "Client"),
            location = ServiceJobLocationRef("loc-1", "Site", "UTC"),
            serviceDate = LocalDate(2026, 1, 15),
            scheduledStart = "2026-01-15T09:00:00Z",
            scheduledEnd = "2026-01-15T10:00:00Z",
            assignee = ServiceJobAssignee("u-1", "Worker"),
            status = ServiceJobRules.Completed,
            requirements = requirements,
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.Partial),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = null,
            startedAt = "2026-01-15T09:05:00Z",
            completedAt = "2026-01-15T09:50:00Z",
        )
    }
}
