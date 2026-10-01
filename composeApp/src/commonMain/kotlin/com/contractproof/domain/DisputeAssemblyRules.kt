package com.contractproof.domain

data class DisputeAssemblyInput(
    val job: ServiceJob,
    val uploadedEvidenceByRequirement: Map<String, String>,
    val uploadedExceptionByRequirement: Map<String, String>,
)

object DisputeAssemblyRules {
    fun buildItems(input: DisputeAssemblyInput): List<DisputeItemDraft> {
        return input.job.requirements.sortedBy { it.sortOrder }.map { requirement ->
            itemForRequirement(
                requirement = requirement,
                evidenceId = input.uploadedEvidenceByRequirement[requirement.id],
                exceptionId = input.uploadedExceptionByRequirement[requirement.id],
            )
        }
    }

    private fun itemForRequirement(
        requirement: ServiceJobRequirement,
        evidenceId: String?,
        exceptionId: String?,
    ): DisputeItemDraft {
        return when (requirement.status) {
            JobRequirementStatus.Satisfied -> {
                val linked = evidenceId
                    ?: throw DisputeRuleViolation(
                        "Requirement \"${requirement.requirementText}\" is satisfied but evidence is not uploaded.",
                    )
                DisputeItemDraft(
                    serviceJobRequirementId = requirement.id,
                    evidenceRecordId = linked,
                    exceptionId = null,
                    outcome = DisputeOutcome.Satisfied,
                )
            }
            JobRequirementStatus.Exception -> {
                val linked = exceptionId
                    ?: throw DisputeRuleViolation(
                        "Requirement \"${requirement.requirementText}\" has an exception that is not uploaded.",
                    )
                DisputeItemDraft(
                    serviceJobRequirementId = requirement.id,
                    evidenceRecordId = null,
                    exceptionId = linked,
                    outcome = DisputeOutcome.Exception,
                )
            }
            JobRequirementStatus.Missing -> {
                DisputeItemDraft(
                    serviceJobRequirementId = requirement.id,
                    evidenceRecordId = null,
                    exceptionId = null,
                    outcome = DisputeOutcome.Missing,
                )
            }
        }
    }
}
