package com.contractproof.domain

data class RequirementEvidenceInput(
    val requirementId: String,
    val text: String,
    val isMandatory: Boolean,
    val requiresPhoto: Boolean,
    val sortOrder: Int,
    val serverStatus: JobRequirementStatus,
    val evidenceSyncStatus: EvidenceSyncStatus? = null,
    val hasLocalEvidence: Boolean = false,
    val hasLocalException: Boolean = false,
    val exceptionSyncPending: Boolean = false,
)

data class RequirementCoverageRow(
    val requirementId: String,
    val text: String,
    val isMandatory: Boolean,
    val requiresPhoto: Boolean,
    val status: EvidenceRequirementStatus,
    val nextActionHint: String,
)

data class JobCoverageSnapshot(
    val totalRequirements: Int,
    val completedRequirements: Int,
    val requiredEvidenceCount: Int,
    val missingEvidenceCount: Int,
    val coveragePercent: Int,
    val evidenceState: ServiceJobEvidenceState,
    val requirements: List<RequirementCoverageRow>,
)

object EvidenceCompletenessEngine {
    fun evaluate(inputs: List<RequirementEvidenceInput>): JobCoverageSnapshot {
        val ordered = inputs.sortedBy { it.sortOrder }
        val rows = ordered.map { classify(it) }
        val mandatory = rows.filter { it.isMandatory }
        val mandatoryTotal = mandatory.size
        val completedMandatory = mandatory.count { it.status.countsTowardMandatoryCoverage() }
        val coveragePercent = if (mandatoryTotal == 0) {
            100
        } else {
            (completedMandatory * 100) / mandatoryTotal
        }
        val requiredEvidenceCount = ordered.count { it.isMandatory && it.requiresPhoto }
        val missingEvidenceCount = mandatory.count {
            it.status == EvidenceRequirementStatus.EvidenceMissing ||
                it.status == EvidenceRequirementStatus.UploadFailed
        }
        val evidenceState = when {
            mandatoryTotal == 0 -> ServiceJobEvidenceState.ReadyToComplete
            completedMandatory == 0 -> ServiceJobEvidenceState.NotStarted
            completedMandatory < mandatoryTotal -> ServiceJobEvidenceState.Partial
            else -> ServiceJobEvidenceState.ReadyToComplete
        }
        return JobCoverageSnapshot(
            totalRequirements = ordered.size,
            completedRequirements = completedMandatory,
            requiredEvidenceCount = requiredEvidenceCount,
            missingEvidenceCount = missingEvidenceCount,
            coveragePercent = coveragePercent,
            evidenceState = evidenceState,
            requirements = rows,
        )
    }

    private fun classify(input: RequirementEvidenceInput): RequirementCoverageRow {
        val status = resolveStatus(input)
        return RequirementCoverageRow(
            requirementId = input.requirementId,
            text = input.text,
            isMandatory = input.isMandatory,
            requiresPhoto = input.requiresPhoto,
            status = status,
            nextActionHint = nextActionHint(input, status),
        )
    }

    private fun resolveStatus(input: RequirementEvidenceInput): EvidenceRequirementStatus {
        if (input.hasLocalException || input.exceptionSyncPending) {
            return EvidenceRequirementStatus.ExceptionReported
        }
        when (input.serverStatus) {
            JobRequirementStatus.Exception -> return EvidenceRequirementStatus.ExceptionReported
            JobRequirementStatus.Satisfied -> return EvidenceRequirementStatus.Complete
            JobRequirementStatus.Missing -> Unit
        }
        val sync = input.evidenceSyncStatus
        if (input.hasLocalEvidence || sync != null) {
            return when (sync) {
                EvidenceSyncStatus.Uploaded -> EvidenceRequirementStatus.Complete
                EvidenceSyncStatus.Failed -> EvidenceRequirementStatus.UploadFailed
                EvidenceSyncStatus.Pending,
                EvidenceSyncStatus.Uploading,
                EvidenceSyncStatus.Retrying,
                -> EvidenceRequirementStatus.UploadPending
                null -> EvidenceRequirementStatus.UploadPending
            }
        }
        if (!input.isMandatory) {
            return EvidenceRequirementStatus.Incomplete
        }
        return EvidenceRequirementStatus.EvidenceMissing
    }

    private fun nextActionHint(input: RequirementEvidenceInput, status: EvidenceRequirementStatus): String {
        return when (status) {
            EvidenceRequirementStatus.EvidenceMissing ->
                if (input.requiresPhoto) "Take photo" else "Mark done"
            EvidenceRequirementStatus.UploadFailed -> "Retry upload"
            EvidenceRequirementStatus.UploadPending -> "Wait for upload"
            EvidenceRequirementStatus.ExceptionReported -> "Exception documented"
            EvidenceRequirementStatus.Complete -> "Complete"
            EvidenceRequirementStatus.Incomplete ->
                if (input.requiresPhoto) "Take photo" else "Mark done"
        }
    }

    private fun EvidenceRequirementStatus.countsTowardMandatoryCoverage(): Boolean {
        return when (this) {
            EvidenceRequirementStatus.Complete,
            EvidenceRequirementStatus.ExceptionReported,
            EvidenceRequirementStatus.UploadPending,
            -> true
            EvidenceRequirementStatus.Incomplete,
            EvidenceRequirementStatus.EvidenceMissing,
            EvidenceRequirementStatus.UploadFailed,
            -> false
        }
    }
}
