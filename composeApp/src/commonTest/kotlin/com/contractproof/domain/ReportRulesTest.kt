package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReportRulesTest {
    @Test
    fun managerCanRequestReport() {
        ReportRules.requireCanRequestReport(Access.forMembership("manager"))
    }

    @Test
    fun cleanerCannotRequestReport() {
        assertFailsWith<ReportRuleViolation> {
            ReportRules.requireCanRequestReport(Access.forMembership("cleaner"))
        }
    }

    @Test
    fun blankDisputeIdRejected() {
        assertFailsWith<ReportRuleViolation> {
            ReportRules.requireRequest(ReportGenerationRequest(disputeId = "  "))
        }
    }
}
