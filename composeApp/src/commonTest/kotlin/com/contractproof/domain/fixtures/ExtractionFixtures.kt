package com.contractproof.domain.fixtures

import com.contractproof.domain.ContractExtractionV1
import com.contractproof.domain.ExtractionDocumentInfo
import com.contractproof.domain.ExtractionPipelineInfo
import com.contractproof.domain.ExtractionRequirementCandidate
import com.contractproof.domain.ExtractionRules
import com.contractproof.domain.ExtractionVisitCandidate

/** Deterministic extraction payloads mirroring shared extraction-fixtures JSON files. */
object ExtractionFixtures {
    private fun basePipeline(usedFallback: Boolean = false): ExtractionPipelineInfo {
        return ExtractionPipelineInfo(
            textEngine = "unpdf",
            llmProvider = if (usedFallback) "groq" else "gemini",
            llmModel = if (usedFallback) "llama-3.3-70b-versatile" else "gemini-2.0-flash",
            usedFallback = usedFallback,
        )
    }

    private fun envelope(
        document: ExtractionDocumentInfo,
        visits: List<ExtractionVisitCandidate>,
        requirements: List<ExtractionRequirementCandidate>,
        pipeline: ExtractionPipelineInfo = basePipeline(),
    ): ContractExtractionV1 {
        return ContractExtractionV1(
            schemaVersion = ExtractionRules.SchemaVersion,
            status = ExtractionRules.StatusCompleted,
            extractedAt = "2026-10-01T12:00:00Z",
            pipeline = pipeline,
            document = document,
            visits = visits,
            requirements = requirements,
        )
    }

    fun normalContract(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(3, 4200, emptyList(), emptyList()),
            visits = listOf(
                visit(weekday = 2, start = "08:00", end = "10:00", confidence = 0.92),
            ),
            requirements = listOf(
                requirement(
                    key = "req_1",
                    task = "Sweep lobby",
                    requiresPhoto = true,
                    confidence = 0.9,
                    quote = "Lobby shall be swept weekly",
                ),
                requirement(
                    key = "req_2",
                    task = "Empty trash receptacles",
                    requiresPhoto = true,
                    confidence = 0.88,
                    quote = "All trash removed each service",
                ),
            ),
        )
    }

    fun missingEvidenceRequirement(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(
                pageCount = 2,
                textCharCount = 1800,
                ocrPageIndexes = emptyList(),
                warnings = listOf(
                    "Contract requires photo evidence for trash removal; model did not flag requires_photo.",
                ),
            ),
            visits = listOf(
                visit(weekday = 3, start = "18:00", end = "20:00", confidence = 0.75),
            ),
            requirements = listOf(
                requirement(
                    key = "req_trash",
                    task = "Remove trash from kitchen",
                    requiresPhoto = false,
                    confidence = 0.7,
                    quote = "Trash shall be removed nightly",
                ),
            ),
        )
    }

    fun ambiguousFrequency(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(
                pageCount = 4,
                textCharCount = 3100,
                ocrPageIndexes = listOf(2),
                warnings = listOf(
                    "Visit frequency ambiguous: contract mentions both weekly and biweekly service.",
                ),
            ),
            visits = emptyList(),
            requirements = listOf(
                requirement(
                    key = "req_floor",
                    task = "Mop main hallway",
                    requiresPhoto = true,
                    confidence = 0.82,
                    quote = "Hallways maintained",
                ),
            ),
            pipeline = basePipeline(usedFallback = true),
        )
    }

    fun unsupportedWording(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(
                pageCount = 1,
                textCharCount = 900,
                ocrPageIndexes = emptyList(),
                warnings = listOf(
                    "Unsupported phrasing normalized for req_misc: vendor shall keep premises tidy.",
                ),
            ),
            visits = listOf(
                visit(
                    weekday = 5,
                    start = "06:00",
                    end = "08:00",
                    timezone = "America/Chicago",
                    confidence = 0.55,
                ),
            ),
            requirements = listOf(
                requirement(
                    key = "req_misc",
                    task = "Keep premises tidy",
                    requiresPhoto = false,
                    isMandatory = false,
                    confidence = 0.45,
                    quote = "Vendor shall keep premises tidy",
                ),
            ),
        )
    }

    fun duplicateRequirements(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(1, 200, emptyList(), emptyList()),
            visits = listOf(visit(confidence = 0.8)),
            requirements = listOf(
                requirement(key = "req_dup", task = "First task"),
                requirement(key = "req_dup", task = "Second task with same key", confidence = 0.85),
            ),
        )
    }

    fun emptyRequirements(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(1, 100, emptyList(), emptyList()),
            visits = listOf(visit()),
            requirements = emptyList(),
        )
    }

    fun humanCorrectionsBaseline(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(2, 1500, emptyList(), emptyList()),
            visits = listOf(visit(confidence = 0.9)),
            requirements = listOf(
                requirement(key = "req_1", task = "Sweep lobby", quote = "Lobby shall be swept"),
                requirement(
                    key = "req_2",
                    task = "Dust surfaces",
                    requiresPhoto = false,
                    confidence = 0.8,
                ),
            ),
        )
    }

    fun badConfidence(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(1, 50, emptyList(), emptyList()),
            visits = listOf(visit(confidence = 1.5)),
            requirements = listOf(requirement(key = "req_1", task = "Sweep")),
        )
    }

    fun invalidWeekday(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(1, 50, emptyList(), emptyList()),
            visits = listOf(visit(weekday = 0)),
            requirements = listOf(requirement(key = "req_1", task = "Sweep")),
        )
    }

    fun endBeforeStart(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(1, 50, emptyList(), emptyList()),
            visits = listOf(
                visit(start = "14:00", end = "10:00"),
            ),
            requirements = listOf(requirement(key = "req_1", task = "Sweep")),
        )
    }

    fun missingKey(): ContractExtractionV1 {
        return envelope(
            document = ExtractionDocumentInfo(1, 50, emptyList(), emptyList()),
            visits = listOf(visit()),
            requirements = listOf(
                requirement(key = "   ", task = "Sweep"),
            ),
        )
    }

    fun badSchemaVersion(): ContractExtractionV1 {
        return normalContract().copy(schemaVersion = 99)
    }

    private fun visit(
        weekday: Int = 2,
        start: String = "08:00",
        end: String = "10:00",
        timezone: String = "America/New_York",
        confidence: Double = 0.8,
    ): ExtractionVisitCandidate {
        return ExtractionVisitCandidate(
            weekday = weekday,
            startTime = start,
            endTime = end,
            timezone = timezone,
            startsOn = "2026-10-01",
            endsOn = null,
            confidence = confidence,
        )
    }

    private fun requirement(
        key: String,
        task: String,
        requiresPhoto: Boolean = true,
        isMandatory: Boolean = true,
        confidence: Double = 0.9,
        quote: String? = null,
    ): ExtractionRequirementCandidate {
        return ExtractionRequirementCandidate(
            key = key,
            task = task,
            requiresPhoto = requiresPhoto,
            isMandatory = isMandatory,
            zoneCode = null,
            confidence = confidence,
            evidenceQuote = quote,
        )
    }
}
