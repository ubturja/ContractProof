package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class DisputeRulesTest {
    @Test
    fun ownerCanFile() {
        val access = Access.forMembership("owner")
        DisputeRules.requireCanFile(access)
    }

    @Test
    fun clientCanFile() {
        val access = Access.forMembership("client")
        DisputeRules.requireCanFile(access)
    }

    @Test
    fun managerCannotFile() {
        val access = Access.forMembership("manager")
        assertFailsWith<DisputeRuleViolation> {
            DisputeRules.requireCanFile(access)
        }
    }

    @Test
    fun draftRequiresComplaint() {
        val job = job()
        assertFailsWith<DisputeRuleViolation> {
            DisputeRules.requireDraft(
                job,
                DisputeDraft(
                    serviceJobId = job.id,
                    disputedServiceJobRequirementId = "r1",
                    complaint = "  ",
                    serviceDate = job.serviceDate,
                ),
            )
        }
    }

    @Test
    fun draftAcceptsValidInput() {
        val job = job()
        DisputeRules.requireDraft(
            job,
            DisputeDraft(
                serviceJobId = job.id,
                disputedServiceJobRequirementId = "r1",
                complaint = "Area not cleaned.",
                serviceDate = job.serviceDate,
            ),
        )
        assertTrue(true)
    }

    private fun job(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c", "Client"),
            location = ServiceJobLocationRef("l", "Loc", "UTC"),
            serviceDate = LocalDate(2026, 2, 1),
            scheduledStart = "2026-02-01T09:00:00Z",
            scheduledEnd = "2026-02-01T10:00:00Z",
            assignee = null,
            status = ServiceJobRules.Completed,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "r1",
                    contractRequirementId = "cr1",
                    requirementText = "Vacuum",
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
        )
    }
}
