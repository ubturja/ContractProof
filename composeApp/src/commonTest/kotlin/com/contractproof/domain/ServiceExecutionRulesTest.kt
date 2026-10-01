package com.contractproof.domain

import com.contractproof.domain.EvidenceSyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceExecutionRulesTest {
    @Test
    fun pendingEvidenceCountsTowardCoverage() {
        val requirement = mandatoryRequirement(JobRequirementStatus.Missing)
        val overlay = RequirementDraftOverlay(requirementId = requirement.id, evidencePending = true)
        val coverage = ServiceExecutionRules.countsForCoverage(
            listOf(requirement),
            mapOf(requirement.id to overlay),
        )
        assertEquals(100, coverage.coveragePercent)
        assertTrue(ServiceExecutionRules.canFinish(listOf(requirement), mapOf(requirement.id to overlay)))
    }

    @Test
    fun missingMandatoryBlocksFinish() {
        val requirement = mandatoryRequirement(JobRequirementStatus.Missing)
        assertFalse(ServiceExecutionRules.canFinish(listOf(requirement), emptyMap()))
    }

    @Test
    fun serverSatisfiedCountsWithoutDraft() {
        val requirement = mandatoryRequirement(JobRequirementStatus.Satisfied)
        assertTrue(ServiceExecutionRules.canFinish(listOf(requirement), emptyMap()))
    }

    @Test
    fun uploadFailedBlocksFinish() {
        val requirement = mandatoryRequirement(JobRequirementStatus.Missing)
        val overlay = RequirementDraftOverlay(
            requirementId = requirement.id,
            evidencePending = true,
            evidenceFailed = true,
            evidenceSyncStatus = EvidenceSyncStatus.Failed,
        )
        assertFalse(ServiceExecutionRules.canFinish(listOf(requirement), mapOf(requirement.id to overlay)))
    }

    private fun mandatoryRequirement(status: JobRequirementStatus): ServiceJobRequirement {
        return ServiceJobRequirement(
            id = "req-1",
            contractRequirementId = "cr-1",
            requirementText = "Vacuum lobby",
            requiresPhoto = true,
            isMandatory = true,
            sortOrder = 0,
            status = status,
        )
    }
}
