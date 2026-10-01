package com.contractproof.domain

class TaskEvidenceRuleViolation(message: String) : IllegalArgumentException(message)

object TaskEvidenceRules {
    fun requireMatchingRequirement(expectedId: String, activeId: String) {
        if (expectedId.isBlank() || activeId.isBlank()) {
            throw TaskEvidenceRuleViolation("Requirement context is missing.")
        }
        if (expectedId != activeId) {
            throw TaskEvidenceRuleViolation("Evidence does not match the open task requirement.")
        }
    }

    fun requireRequirementOnJob(requirementId: String, job: ServiceJob) {
        if (job.requirements.none { it.id == requirementId }) {
            throw TaskEvidenceRuleViolation("Requirement is not on this service job.")
        }
    }
}
