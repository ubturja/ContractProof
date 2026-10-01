package com.contractproof.feature.service

import com.contractproof.domain.EvidenceRequirementStatus
import com.contractproof.domain.JobCoverageSnapshot
import com.contractproof.domain.ServiceCompletionRules
object CoverageDisplayMapper {
    fun toUiState(
        snapshot: JobCoverageSnapshot,
        jobStatus: String,
    ): CoverageUiState {
        val missing = snapshot.requirements.filter {
            it.status == EvidenceRequirementStatus.EvidenceMissing
        }.map { formatLine(it.text, it.requiresPhoto) }
        val exceptions = snapshot.requirements.filter {
            it.status == EvidenceRequirementStatus.ExceptionReported
        }.map { it.text }
        val uploadPending = snapshot.requirements.filter {
            it.status == EvidenceRequirementStatus.UploadPending
        }.map { formatLine(it.text, it.requiresPhoto) }
        val uploadFailed = snapshot.requirements.filter {
            it.status == EvidenceRequirementStatus.UploadFailed
        }.map { formatLine(it.text, it.requiresPhoto) }
        val mandatoryTotal = snapshot.requirements.count { it.isMandatory }
        val summaryLine = if (mandatoryTotal == 0) {
            "No mandatory requirements"
        } else {
            "${snapshot.completedRequirements} of $mandatoryTotal required · ${snapshot.missingEvidenceCount} missing"
        }
        val canFinish = ServiceCompletionRules.canCompleteJob(snapshot, jobStatus)
        val primaryAction = resolvePrimaryAction(snapshot, canFinish)
        return CoverageUiState(
            coveragePercent = snapshot.coveragePercent,
            summaryLine = summaryLine,
            missingLines = missing,
            exceptionLines = exceptions,
            uploadPendingLines = uploadPending,
            uploadFailedLines = uploadFailed,
            primaryAction = primaryAction,
            canFinish = canFinish,
        )
    }

    private fun resolvePrimaryAction(
        snapshot: JobCoverageSnapshot,
        canFinish: Boolean,
    ): CoveragePrimaryAction {
        val blocking = snapshot.requirements.firstOrNull { row ->
            row.isMandatory &&
                (
                    row.status == EvidenceRequirementStatus.EvidenceMissing ||
                        row.status == EvidenceRequirementStatus.UploadFailed
                    )
        }
        when {
            blocking?.status == EvidenceRequirementStatus.UploadFailed ->
                return CoveragePrimaryAction.RetryUpload(
                    requirementId = blocking.requirementId,
                    label = "Retry upload for ${blocking.text}",
                )
            blocking != null ->
                return CoveragePrimaryAction.OpenTask(
                    requirementId = blocking.requirementId,
                    label = if (blocking.requiresPhoto) {
                        "Take photo: ${blocking.text}"
                    } else {
                        "Complete: ${blocking.text}"
                    },
                )
            canFinish && snapshot.evidenceState == com.contractproof.domain.ServiceJobEvidenceState.ReadyToComplete ->
                return CoveragePrimaryAction.FinishService
            else -> return CoveragePrimaryAction.BackToJob
        }
    }

    private fun formatLine(text: String, requiresPhoto: Boolean): String {
        return if (requiresPhoto) {
            "$text photo"
        } else {
            text
        }
    }
}
