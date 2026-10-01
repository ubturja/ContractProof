package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceCompletionRulesTest {
    @Test
    fun completeJobWhenMandatoryMet() {
        val snapshot = snapshotOf(
            row("req-1", EvidenceRequirementStatus.Complete, mandatory = true),
            row("req-2", EvidenceRequirementStatus.UploadPending, mandatory = true, sortOrder = 1),
        )
        assertTrue(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress))
    }

    @Test
    fun missingEvidenceBlocksCompletion() {
        val snapshot = snapshotOf(
            row("req-1", EvidenceRequirementStatus.EvidenceMissing, mandatory = true),
        )
        assertFalse(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress))
        assertTrue(ServiceCompletionRules.completionBlockers(snapshot).isNotEmpty())
    }

    @Test
    fun exceptionDocumentedAllowsCompletion() {
        val snapshot = snapshotOf(
            row("req-1", EvidenceRequirementStatus.ExceptionReported, mandatory = true),
        )
        assertTrue(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress))
        assertTrue(ServiceCompletionRules.isDocumentedException(snapshot.requirements.first()))
    }

    @Test
    fun uploadPendingAllowsCompletion() {
        val snapshot = snapshotOf(
            row("req-1", EvidenceRequirementStatus.UploadPending, mandatory = true),
        )
        assertTrue(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress))
    }

    @Test
    fun uploadFailedBlocksCompletion() {
        val snapshot = snapshotOf(
            row("req-1", EvidenceRequirementStatus.UploadFailed, mandatory = true),
        )
        assertFalse(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress))
        assertTrue(ServiceCompletionRules.isIncompleteWork(snapshot.requirements.first()))
    }

    @Test
    fun notInProgressBlocksCompletion() {
        val snapshot = snapshotOf(
            row("req-1", EvidenceRequirementStatus.Complete, mandatory = true),
        )
        assertFalse(ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.Scheduled))
    }

    private fun row(
        id: String,
        status: EvidenceRequirementStatus,
        mandatory: Boolean,
        sortOrder: Int = 0,
        requiresPhoto: Boolean = true,
    ): RequirementCoverageRow {
        return RequirementCoverageRow(
            requirementId = id,
            text = "Task $id",
            isMandatory = mandatory,
            requiresPhoto = requiresPhoto,
            status = status,
            nextActionHint = "",
        )
    }

    private fun snapshotOf(vararg rows: RequirementCoverageRow): JobCoverageSnapshot {
        val mandatory = rows.filter { it.isMandatory }
        val completed = mandatory.count {
            it.status == EvidenceRequirementStatus.Complete ||
                it.status == EvidenceRequirementStatus.ExceptionReported ||
                it.status == EvidenceRequirementStatus.UploadPending
        }
        val total = mandatory.size
        val percent = if (total == 0) 100 else (completed * 100) / total
        return JobCoverageSnapshot(
            totalRequirements = rows.size,
            completedRequirements = completed,
            requiredEvidenceCount = rows.count { it.isMandatory && it.requiresPhoto },
            missingEvidenceCount = mandatory.count {
                it.status == EvidenceRequirementStatus.EvidenceMissing ||
                    it.status == EvidenceRequirementStatus.UploadFailed
            },
            coveragePercent = percent,
            evidenceState = if (completed == total) {
                ServiceJobEvidenceState.ReadyToComplete
            } else {
                ServiceJobEvidenceState.Partial
            },
            requirements = rows.toList(),
        )
    }
}
