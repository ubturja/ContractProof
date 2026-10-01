package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvidenceCompletenessEngineTest {
    @Test
    fun allMandatoryComplete() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(
                    id = "req-1",
                    serverStatus = JobRequirementStatus.Satisfied,
                ),
            ),
        )
        assertEquals(100, snapshot.coveragePercent)
        assertEquals(1, snapshot.completedRequirements)
        assertEquals(EvidenceRequirementStatus.Complete, snapshot.requirements.first().status)
    }

    @Test
    fun evidenceMissingBlocksCoverage() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(id = "req-1", serverStatus = JobRequirementStatus.Missing),
            ),
        )
        assertEquals(0, snapshot.coveragePercent)
        assertEquals(1, snapshot.missingEvidenceCount)
        assertEquals(EvidenceRequirementStatus.EvidenceMissing, snapshot.requirements.first().status)
    }

    @Test
    fun exceptionReportedCountsAsComplete() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(
                    id = "req-1",
                    serverStatus = JobRequirementStatus.Exception,
                ),
            ),
        )
        assertEquals(100, snapshot.coveragePercent)
        assertEquals(EvidenceRequirementStatus.ExceptionReported, snapshot.requirements.first().status)
    }

    @Test
    fun uploadPendingCountsTowardCoverage() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(
                    id = "req-1",
                    serverStatus = JobRequirementStatus.Missing,
                    hasLocalEvidence = true,
                    evidenceSyncStatus = EvidenceSyncStatus.Pending,
                ),
            ),
        )
        assertEquals(100, snapshot.coveragePercent)
        assertEquals(EvidenceRequirementStatus.UploadPending, snapshot.requirements.first().status)
    }

    @Test
    fun uploadFailedDoesNotCount() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(
                    id = "req-1",
                    serverStatus = JobRequirementStatus.Missing,
                    hasLocalEvidence = true,
                    evidenceSyncStatus = EvidenceSyncStatus.Failed,
                ),
            ),
        )
        assertEquals(0, snapshot.coveragePercent)
        assertEquals(1, snapshot.missingEvidenceCount)
        assertEquals(EvidenceRequirementStatus.UploadFailed, snapshot.requirements.first().status)
    }

    @Test
    fun optionalIncompleteDoesNotAffectMandatoryPercent() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(id = "req-1", serverStatus = JobRequirementStatus.Satisfied),
                input(
                    id = "req-2",
                    isMandatory = false,
                    serverStatus = JobRequirementStatus.Missing,
                ),
            ),
        )
        assertEquals(100, snapshot.coveragePercent)
        assertEquals(2, snapshot.totalRequirements)
        assertEquals(EvidenceRequirementStatus.Incomplete, snapshot.requirements.last().status)
    }

    @Test
    fun zeroMandatoryIsFullCoverage() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(
                    id = "req-1",
                    isMandatory = false,
                    serverStatus = JobRequirementStatus.Missing,
                ),
            ),
        )
        assertEquals(100, snapshot.coveragePercent)
        assertEquals(ServiceJobEvidenceState.ReadyToComplete, snapshot.evidenceState)
    }

    @Test
    fun requiredEvidenceCountIncludesPhotoMandatory() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(id = "req-1", requiresPhoto = true),
                input(id = "req-2", requiresPhoto = false, sortOrder = 1),
            ),
        )
        assertEquals(1, snapshot.requiredEvidenceCount)
    }

    @Test
    fun localExceptionPendingCountsAsExceptionReported() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(
                    id = "req-1",
                    serverStatus = JobRequirementStatus.Missing,
                    hasLocalException = true,
                    exceptionSyncPending = true,
                ),
            ),
        )
        assertEquals(100, snapshot.coveragePercent)
        assertEquals(EvidenceRequirementStatus.ExceptionReported, snapshot.requirements.first().status)
    }

    @Test
    fun partialMandatoryCoverage() {
        val snapshot = EvidenceCompletenessEngine.evaluate(
            listOf(
                input(id = "req-1", serverStatus = JobRequirementStatus.Satisfied),
                input(id = "req-2", sortOrder = 1, serverStatus = JobRequirementStatus.Missing),
            ),
        )
        assertEquals(50, snapshot.coveragePercent)
        assertEquals(ServiceJobEvidenceState.Partial, snapshot.evidenceState)
    }

    private fun input(
        id: String,
        isMandatory: Boolean = true,
        requiresPhoto: Boolean = true,
        sortOrder: Int = 0,
        serverStatus: JobRequirementStatus = JobRequirementStatus.Missing,
        evidenceSyncStatus: EvidenceSyncStatus? = null,
        hasLocalEvidence: Boolean = false,
        hasLocalException: Boolean = false,
        exceptionSyncPending: Boolean = false,
    ): RequirementEvidenceInput {
        return RequirementEvidenceInput(
            requirementId = id,
            text = "Task $id",
            isMandatory = isMandatory,
            requiresPhoto = requiresPhoto,
            sortOrder = sortOrder,
            serverStatus = serverStatus,
            evidenceSyncStatus = evidenceSyncStatus,
            hasLocalEvidence = hasLocalEvidence,
            hasLocalException = hasLocalException,
            exceptionSyncPending = exceptionSyncPending,
        )
    }
}
