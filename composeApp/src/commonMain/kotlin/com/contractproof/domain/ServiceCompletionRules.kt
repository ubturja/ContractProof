package com.contractproof.domain

data class CompletionBlocker(
    val requirementId: String,
    val label: String,
    val status: EvidenceRequirementStatus,
)

object ServiceCompletionRules {
    fun canCompleteJob(snapshot: JobCoverageSnapshot, jobStatus: String): Boolean {
        if (jobStatus != ServiceJobRules.InProgress) {
            return false
        }
        return snapshot.requirements
            .filter { it.isMandatory }
            .all { it.status.allowsJobCompletion() }
    }

    fun completionBlockers(snapshot: JobCoverageSnapshot): List<CompletionBlocker> {
        return snapshot.requirements
            .filter { it.isMandatory && !it.status.allowsJobCompletion() }
            .map { row ->
                CompletionBlocker(
                    requirementId = row.requirementId,
                    label = blockerLabel(row),
                    status = row.status,
                )
            }
    }

    fun isDocumentedException(row: RequirementCoverageRow): Boolean {
        return row.status == EvidenceRequirementStatus.ExceptionReported
    }

    fun isIncompleteWork(row: RequirementCoverageRow): Boolean {
        return row.status == EvidenceRequirementStatus.EvidenceMissing ||
            row.status == EvidenceRequirementStatus.UploadFailed
    }

    private fun blockerLabel(row: RequirementCoverageRow): String {
        val suffix = when {
            row.status == EvidenceRequirementStatus.UploadFailed -> " (upload failed)"
            row.requiresPhoto -> " photo"
            else -> ""
        }
        return row.text + suffix
    }

    private fun EvidenceRequirementStatus.allowsJobCompletion(): Boolean {
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
