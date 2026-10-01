package com.contractproof.domain

object EvidenceCompletenessMapper {
    fun toInput(
        requirement: ServiceJobRequirement,
        overlay: RequirementDraftOverlay?,
    ): RequirementEvidenceInput {
        val draft = overlay?.takeIf { it.requirementId == requirement.id }
        val hasLocalEvidence = draft?.evidencePending == true ||
            draft?.evidenceFailed == true ||
            draft?.evidenceSyncStatus != null
        val hasLocalException = draft?.exceptionPending == true || draft?.exceptionFailed == true
        return RequirementEvidenceInput(
            requirementId = requirement.id,
            text = requirement.requirementText,
            isMandatory = requirement.isMandatory,
            requiresPhoto = requirement.requiresPhoto,
            sortOrder = requirement.sortOrder,
            serverStatus = requirement.status,
            evidenceSyncStatus = draft?.evidenceSyncStatus,
            hasLocalEvidence = hasLocalEvidence,
            hasLocalException = hasLocalException,
            exceptionSyncPending = draft?.exceptionPending == true,
        )
    }

    fun evaluate(
        requirements: List<ServiceJobRequirement>,
        drafts: Map<String, RequirementDraftOverlay>,
    ): JobCoverageSnapshot {
        val inputs = requirements.map { requirement ->
            toInput(requirement, drafts[requirement.id])
        }
        return EvidenceCompletenessEngine.evaluate(inputs)
    }
}
