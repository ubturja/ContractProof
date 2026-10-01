package com.contractproof.domain

import com.contractproof.domain.fixtures.ExtractionFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExtractionReviewRulesTest {
    @Test
    fun draftsFromExtraction_mapsNormalFixture() {
        val drafts = ExtractionReviewRules.draftsFromExtraction(ExtractionFixtures.normalContract())
        assertEquals(2, drafts.size)
        assertEquals("req_1", drafts[0].extractionKey)
        assertTrue(drafts[0].fromExtraction)
        assertEquals(0.9, drafts[0].confidence)
        assertEquals("req_2", drafts[1].extractionKey)
    }

    @Test
    fun draftsFromExtractionPreserveKeys() {
        val extraction = ContractExtractionV1(
            schemaVersion = 1,
            status = "completed",
            extractedAt = "2026-10-01T12:00:00Z",
            pipeline = ExtractionPipelineInfo("unpdf", "gemini", "gemini-2.0-flash", false),
            document = ExtractionDocumentInfo(1, 10, emptyList(), emptyList()),
            visits = emptyList(),
            requirements = listOf(
                ExtractionRequirementCandidate(
                    key = "req_1",
                    task = "Sweep",
                    requiresPhoto = true,
                    isMandatory = true,
                ),
            ),
        )
        val drafts = ExtractionReviewRules.draftsFromExtraction(extraction)
        assertEquals(1, drafts.size)
        assertEquals("req_1", drafts.single().extractionKey)
        assertTrue(drafts.single().fromExtraction)
    }

    @Test
    fun draftsFromExtraction_rejectsDuplicateKeys() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionReviewRules.draftsFromExtraction(ExtractionFixtures.duplicateRequirements())
        }
    }

    @Test
    fun draftsFromExtraction_rejectsEmptyRequirements() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionReviewRules.draftsFromExtraction(ExtractionFixtures.emptyRequirements())
        }
    }

    @Test
    fun approveRequiresAtLeastOneTask() {
        assertFailsWith<ExtractionReviewRuleViolation> {
            ExtractionReviewRules.requireCanApprove(
                listOf(ExtractionReviewRules.newManualDraft()),
            )
        }
    }

    @Test
    fun lowConfidenceLabel() {
        assertEquals("Low confidence (50%)", ExtractionReviewRules.confidenceLabel(0.5))
    }
}
