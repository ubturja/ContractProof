package com.contractproof.domain

object ReportRules {
    fun requireCanRequestReport(access: Access) {
        if (!access.canOpenDisputes) {
            throw ReportRuleViolation("You cannot request an evidence report.")
        }
    }

    fun requireRequest(request: ReportGenerationRequest) {
        if (request.disputeId.isBlank()) {
            throw ReportRuleViolation("Dispute id is required.")
        }
    }
}
