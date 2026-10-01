package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class TaskEvidenceRulesTest {
    @Test
    fun matchingRequirementPasses() {
        TaskEvidenceRules.requireMatchingRequirement("req-a", "req-a")
    }

    @Test
    fun mismatchRequirementFails() {
        assertFailsWith<TaskEvidenceRuleViolation> {
            TaskEvidenceRules.requireMatchingRequirement("req-a", "req-b")
        }
    }

    @Test
    fun blankRequirementFails() {
        assertFailsWith<TaskEvidenceRuleViolation> {
            TaskEvidenceRules.requireMatchingRequirement("", "req-a")
        }
    }

    @Test
    fun requirementNotOnJobFails() {
        val job = sampleJob()
        assertFailsWith<TaskEvidenceRuleViolation> {
            TaskEvidenceRules.requireRequirementOnJob("missing", job)
        }
    }

    @Test
    fun requirementOnJobPasses() {
        val job = sampleJob()
        TaskEvidenceRules.requireRequirementOnJob("req-1", job)
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("user-1", "Cleaner"),
            status = ServiceJobRules.Scheduled,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "req-1",
                    contractRequirementId = "cr-1",
                    requirementText = "Vacuum",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.NotStarted),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = "schedule-1",
        )
    }
}
