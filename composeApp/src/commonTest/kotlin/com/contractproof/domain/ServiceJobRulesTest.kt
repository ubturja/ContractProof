package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceJobRulesTest {
    @Test
    fun invalidStorageStatusIsRejected() {
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireStorageStatus("bogus")
        }
    }

    @Test
    fun evidenceCoverageCountsMandatoryOnly() {
        val requirements = listOf(
            requirement(isMandatory = true, status = JobRequirementStatus.Satisfied),
            requirement(isMandatory = true, status = JobRequirementStatus.Missing),
            requirement(isMandatory = false, status = JobRequirementStatus.Missing),
        )
        val evidence = ServiceJobRules.computeEvidenceStatus(requirements)
        assertEquals(2, evidence.mandatoryTotal)
        assertEquals(1, evidence.mandatorySatisfied)
        assertEquals(50, evidence.coveragePercent)
        assertEquals(ServiceJobEvidenceState.Partial, evidence.state)
        assertFalse(ServiceJobRules.canComplete(requirements))
    }

    @Test
    fun exceptionCountsAsSatisfiedForMandatory() {
        val requirements = listOf(
            requirement(isMandatory = true, status = JobRequirementStatus.Exception),
        )
        val evidence = ServiceJobRules.computeEvidenceStatus(requirements)
        assertEquals(100, evidence.coveragePercent)
        assertEquals(ServiceJobEvidenceState.ReadyToComplete, evidence.state)
        assertTrue(ServiceJobRules.canComplete(requirements))
    }

    @Test
    fun noMandatoryRequirementsAreReadyToComplete() {
        val requirements = listOf(
            requirement(isMandatory = false, status = JobRequirementStatus.Missing),
        )
        assertTrue(ServiceJobRules.canComplete(requirements))
    }

    @Test
    fun allowedStatusTransitions() {
        ServiceJobRules.requireStatusTransition(ServiceJobRules.Scheduled, ServiceJobRules.InProgress)
        ServiceJobRules.requireStatusTransition(ServiceJobRules.InProgress, ServiceJobRules.Completed)
        ServiceJobRules.requireStatusTransition(ServiceJobRules.InProgress, ServiceJobRules.Incomplete)
        ServiceJobRules.requireStatusTransition(ServiceJobRules.Completed, ServiceJobRules.Disputed)
    }

    @Test
    fun disallowedStatusTransitions() {
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireStatusTransition(ServiceJobRules.Scheduled, ServiceJobRules.Completed)
        }
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireStatusTransition(ServiceJobRules.Incomplete, ServiceJobRules.InProgress)
        }
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireStatusTransition(ServiceJobRules.Disputed, ServiceJobRules.Completed)
        }
    }

    @Test
    fun completeRequiresMandatoryCoverage() {
        val requirements = listOf(
            requirement(isMandatory = true, status = JobRequirementStatus.Missing),
        )
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireComplete(
                ServiceJobRules.InProgress,
                requirements,
                "2026-10-01T12:00:00Z",
                "user-1",
            )
        }
    }

    @Test
    fun disputedRequiresCompletionFields() {
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireMarkDisputed(
                ServiceJobRules.Completed,
                startedAt = null,
                completedAt = "2026-10-01T12:00:00Z",
                completedBy = "user-1",
            )
        }
        ServiceJobRules.requireMarkDisputed(
            ServiceJobRules.Completed,
            startedAt = "2026-10-01T10:00:00Z",
            completedAt = "2026-10-01T12:00:00Z",
            completedBy = "user-1",
        )
    }

    @Test
    fun cleanerMustBeAssignee() {
        val job = sampleJob(assigneeId = "other-user")
        assertFailsWith<ServiceJobRuleViolation> {
            ServiceJobRules.requireAssigneeForCleanerAction("cleaner-1", job)
        }
        ServiceJobRules.requireAssigneeForCleanerAction("other-user", job)
    }

    private fun requirement(
        isMandatory: Boolean,
        status: JobRequirementStatus,
    ): ServiceJobRequirement {
        return ServiceJobRequirement(
            id = "req-1",
            contractRequirementId = "cr-1",
            requirementText = "Task",
            requiresPhoto = true,
            isMandatory = isMandatory,
            sortOrder = 0,
            status = status,
        )
    }

    private fun sampleJob(assigneeId: String): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Client"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = kotlinx.datetime.LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee(assigneeId, "Worker"),
            status = ServiceJobRules.Scheduled,
            requirements = emptyList(),
            evidenceStatus = ServiceJobRules.computeEvidenceStatus(emptyList()),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = "schedule-1",
        )
    }
}
