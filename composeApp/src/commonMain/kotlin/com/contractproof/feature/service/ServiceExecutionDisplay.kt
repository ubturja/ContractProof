package com.contractproof.feature.service

import com.contractproof.core.design.CpWorkStatus
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.RequirementDraftOverlay
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.ServiceJobRequirement
import com.contractproof.domain.ServiceExecutionRules
import com.contractproof.domain.TaskExecutionState
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.TaskNextAction

object ServiceExecutionDisplay {
    fun workStatusForEvidenceDraft(draft: EvidenceDraft): CpWorkStatus {
        return when (draft.syncStatus) {
            EvidenceSyncStatus.Pending -> CpWorkStatus.Pending
            EvidenceSyncStatus.Uploading -> CpWorkStatus.Uploading
            EvidenceSyncStatus.Uploaded -> CpWorkStatus.Uploaded
            EvidenceSyncStatus.Failed -> CpWorkStatus.Failed
            EvidenceSyncStatus.Retrying -> CpWorkStatus.Retrying
        }
    }

    fun taskWorkStatus(
        state: TaskExecutionState,
        evidenceDraft: EvidenceDraft?,
    ): CpWorkStatus {
        if (evidenceDraft != null) {
            return workStatusForEvidenceDraft(evidenceDraft)
        }
        return toWorkStatus(state)
    }
    fun toWorkStatus(state: TaskExecutionState): CpWorkStatus {
        return when (state) {
            TaskExecutionState.Missing -> CpWorkStatus.Missing
            TaskExecutionState.PendingEvidence -> CpWorkStatus.Pending
            TaskExecutionState.Satisfied -> CpWorkStatus.Uploaded
            TaskExecutionState.PendingException -> CpWorkStatus.Exception
            TaskExecutionState.Exception -> CpWorkStatus.Exception
        }
    }

    fun overlayFromDrafts(
        requirementId: String,
        evidence: EvidenceDraft?,
        exception: ExceptionDraft?,
    ): RequirementDraftOverlay? {
        if (evidence == null && exception == null) {
            return null
        }
        return RequirementDraftOverlay(
            requirementId = requirementId,
            evidencePending = evidence != null && evidence.syncStatus != EvidenceSyncStatus.Uploaded,
            evidenceFailed = evidence?.syncStatus == EvidenceSyncStatus.Failed,
            evidenceSyncStatus = evidence?.syncStatus,
            exceptionPending = exception?.pending == true,
            exceptionFailed = exception?.failed == true,
        )
    }

    fun taskLabel(requirement: ServiceJobRequirement, action: TaskNextAction): String {
        return when (action) {
            TaskNextAction.CapturePhoto -> "Take photo"
            TaskNextAction.MarkDone -> "Mark done"
            TaskNextAction.ReportException -> "Report exception"
            TaskNextAction.ViewTask -> if (requirement.status == JobRequirementStatus.Exception) {
                "View exception"
            } else {
                "View task"
            }
        }
    }

    fun evidenceHint(requirement: ServiceJobRequirement): String {
        return RequirementRules.evidenceLabel(requirement.requiresPhoto)
    }

    fun stateFor(
        requirement: ServiceJobRequirement,
        drafts: ServiceExecutionDraftStore,
    ): TaskExecutionState {
        val overlay = overlayFromDrafts(
            requirement.id,
            drafts.evidenceDraft(requirement.id),
            drafts.exceptionDraft(requirement.id),
        )
        return ServiceExecutionRules.effectiveState(requirement, overlay)
    }
}
