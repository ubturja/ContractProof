package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExceptionRulesTest {
    @Test
    fun otherRequiresNote() {
        assertFailsWith<ExceptionRuleViolation> {
            ExceptionRules.formatReason(ExceptionRules.Other, "")
        }
    }

    @Test
    fun formatsReasonWithNote() {
        val formatted = ExceptionRules.formatReason(ExceptionRules.AreaBlocked, "Gate locked")
        assertTrue(formatted.contains("Area blocked"))
        assertTrue(formatted.contains("Gate locked"))
    }

    @Test
    fun requireReasonRejectsUnknown() {
        assertFailsWith<ExceptionRuleViolation> {
            ExceptionRules.requireReason("Not a real reason")
        }
    }

    @Test
    fun otherWithoutNoteRejectedInFormatReason() {
        assertFailsWith<ExceptionRuleViolation> {
            ExceptionRules.formatReason(ExceptionRules.Other, "")
        }
    }
}
