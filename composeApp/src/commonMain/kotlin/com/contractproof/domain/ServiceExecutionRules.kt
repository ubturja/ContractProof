package com.contractproof.domain

class ServiceExecutionRuleViolation(message: String) : IllegalArgumentException(message)

object ServiceExecutionRules {
    fun coverageSnapshot(
        requirements: List<ServiceJobRequirement>,
        drafts: Map<String, RequirementDraftOverlay>,
    ): JobCoverageSnapshot {
        return EvidenceCompletenessMapper.evaluate(requirements, drafts)
    }

    fun effectiveState(
        requirement: ServiceJobRequirement,
        draft: RequirementDraftOverlay?,
    ): TaskExecutionState {
        val row = EvidenceCompletenessEngine.evaluate(
            listOf(EvidenceCompletenessMapper.toInput(requirement, draft)),
        ).requirements.first()
        val overlay = draft?.takeIf { it.requirementId == requirement.id }
        return when (row.status) {
            EvidenceRequirementStatus.Complete -> TaskExecutionState.Satisfied
            EvidenceRequirementStatus.UploadPending -> TaskExecutionState.PendingEvidence
            EvidenceRequirementStatus.ExceptionReported ->
                if (overlay?.exceptionPending == true) {
                    TaskExecutionState.PendingException
                } else {
                    TaskExecutionState.Exception
                }
            EvidenceRequirementStatus.UploadFailed,
            EvidenceRequirementStatus.EvidenceMissing,
            EvidenceRequirementStatus.Incomplete,
            -> TaskExecutionState.Missing
        }
    }

    fun countsForCoverage(
        requirements: List<ServiceJobRequirement>,
        drafts: Map<String, RequirementDraftOverlay>,
    ): ServiceJobEvidenceStatus {
        val snapshot = coverageSnapshot(requirements, drafts)
        val mandatoryTotal = snapshot.requirements.count { it.isMandatory }
        return ServiceJobEvidenceStatus(
            mandatoryTotal = mandatoryTotal,
            mandatorySatisfied = snapshot.completedRequirements,
            coveragePercent = snapshot.coveragePercent,
            state = snapshot.evidenceState,
        )
    }

    fun canFinish(
        requirements: List<ServiceJobRequirement>,
        drafts: Map<String, RequirementDraftOverlay>,
    ): Boolean {
        val snapshot = coverageSnapshot(requirements, drafts)
        return ServiceCompletionRules.canCompleteJob(snapshot, ServiceJobRules.InProgress)
    }

    fun nextAction(
        requirement: ServiceJobRequirement,
        draft: RequirementDraftOverlay?,
    ): TaskNextAction {
        return when (effectiveState(requirement, draft)) {
            TaskExecutionState.Satisfied,
            TaskExecutionState.PendingEvidence,
            TaskExecutionState.Exception,
            TaskExecutionState.PendingException,
            -> TaskNextAction.ViewTask
            TaskExecutionState.Missing ->
                if (requirement.requiresPhoto) {
                    TaskNextAction.CapturePhoto
                } else {
                    TaskNextAction.MarkDone
                }
        }
    }

    fun requireCanOpenTask(job: ServiceJob, userId: String, access: Access) {
        ServiceJobRules.requireJobAction(access, userId, job)
        if (job.status == ServiceJobRules.Scheduled) {
            throw ServiceExecutionRuleViolation("Start the service before working on tasks.")
        }
        if (job.status == ServiceJobRules.Completed || job.status == ServiceJobRules.Disputed) {
            throw ServiceExecutionRuleViolation("This service is already finished.")
        }
    }
}
