package com.contractproof.domain

import com.contractproof.domain.fixtures.ExtractionFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExtractionRulesTest {
    @Test
    fun normalContract_normalizesAndAccepts() {
        val normalized = ExtractionRules.requireCompleted(ExtractionFixtures.normalContract())
        assertEquals("08:00:00", normalized.visits.single().startTime)
        assertEquals(2, normalized.requirements.size)
        assertEquals("req_1", normalized.requirements[0].key)
        assertEquals("Empty trash receptacles", normalized.requirements[1].task)
    }

    @Test
    fun validExtractionNormalizesTimesAndKeys() {
        val extraction = sampleExtraction()
        val normalized = ExtractionRules.requireCompleted(extraction)
        assertEquals("08:00:00", normalized.visits.single().startTime)
        assertEquals("req_1", normalized.requirements.single().key)
        assertEquals("Sweep lobby", normalized.requirements.single().task)
    }

    @Test
    fun duplicateKeysAreRejected() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.duplicateRequirements())
        }
    }

    @Test
    fun emptyRequirementsAreRejected() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.emptyRequirements())
        }
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireRequirements(emptyList())
        }
    }

    @Test
    fun confidenceMustBeInRange() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.badConfidence())
        }
    }

    @Test
    fun invalidWeekdayIsRejected() {
        assertFailsWith<RequirementRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.invalidWeekday())
        }
    }

    @Test
    fun endBeforeStartIsRejected() {
        assertFailsWith<RequirementRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.endBeforeStart())
        }
    }

    @Test
    fun missingKeyIsRejected() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.missingKey())
        }
    }

    @Test
    fun badSchemaVersionIsRejected() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.badSchemaVersion())
        }
    }

    @Test
    fun reviewableFixturesPassValidation() {
        ExtractionRules.requireCompleted(ExtractionFixtures.missingEvidenceRequirement())
        ExtractionRules.requireCompleted(ExtractionFixtures.ambiguousFrequency())
        ExtractionRules.requireCompleted(ExtractionFixtures.unsupportedWording())
    }

    private fun sampleExtraction(): ContractExtractionV1 {
        return ContractExtractionV1(
            schemaVersion = ExtractionRules.SchemaVersion,
            status = ExtractionRules.StatusCompleted,
            extractedAt = "2026-10-01T12:00:00Z",
            pipeline = ExtractionPipelineInfo(
                textEngine = "unpdf",
                llmProvider = "gemini",
                llmModel = "gemini-2.0-flash",
                usedFallback = false,
            ),
            document = ExtractionDocumentInfo(
                pageCount = 2,
                textCharCount = 100,
                ocrPageIndexes = emptyList(),
                warnings = emptyList(),
            ),
            visits = listOf(sampleVisit()),
            requirements = listOf(requirement(" req_1 ", " Sweep lobby ")),
        )
    }

    private fun sampleVisit(): ExtractionVisitCandidate {
        return ExtractionVisitCandidate(
            weekday = 2,
            startTime = "08:00",
            endTime = "10:00",
            timezone = "America/New_York",
            startsOn = "2026-10-01",
            endsOn = null,
            confidence = 0.9,
        )
    }

    private fun requirement(key: String, task: String): ExtractionRequirementCandidate {
        return ExtractionRequirementCandidate(
            key = key,
            task = task,
            requiresPhoto = true,
            isMandatory = true,
        )
    }
}
