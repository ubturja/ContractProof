package com.contractproof.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractionRequest(
    @SerialName("contract_version_id") val contractVersionId: String,
    val force: Boolean = false,
)

@Serializable
data class ExtractionPipelineInfo(
    @SerialName("text_engine") val textEngine: String,
    @SerialName("llm_provider") val llmProvider: String,
    @SerialName("llm_model") val llmModel: String,
    @SerialName("used_fallback") val usedFallback: Boolean,
)

@Serializable
data class ExtractionDocumentInfo(
    @SerialName("page_count") val pageCount: Int,
    @SerialName("text_char_count") val textCharCount: Int,
    @SerialName("ocr_page_indexes") val ocrPageIndexes: List<Int>,
    val warnings: List<String>,
)

@Serializable
data class ExtractionVisitCandidate(
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val timezone: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String? = null,
    val confidence: Double? = null,
)

@Serializable
data class ExtractionRequirementCandidate(
    val key: String,
    val task: String,
    @SerialName("requires_photo") val requiresPhoto: Boolean,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    @SerialName("zone_code") val zoneCode: String? = null,
    val confidence: Double? = null,
    @SerialName("evidence_quote") val evidenceQuote: String? = null,
)

@Serializable
data class ContractExtractionV1(
    @SerialName("schema_version") val schemaVersion: Int,
    val status: String,
    @SerialName("extracted_at") val extractedAt: String,
    val pipeline: ExtractionPipelineInfo,
    val document: ExtractionDocumentInfo,
    val visits: List<ExtractionVisitCandidate>,
    val requirements: List<ExtractionRequirementCandidate>,
)

class ExtractionRuleViolation(message: String) : IllegalArgumentException(message)

object ExtractionRules {
    const val SchemaVersion = 1
    const val StatusCompleted = "completed"

    fun requireCompleted(extraction: ContractExtractionV1): ContractExtractionV1 {
        if (extraction.schemaVersion != SchemaVersion) {
            throw ExtractionRuleViolation("Extraction schema version is not supported.")
        }
        if (extraction.status != StatusCompleted) {
            throw ExtractionRuleViolation("Extraction status must be completed.")
        }
        if (extraction.extractedAt.trim().isEmpty()) {
            throw ExtractionRuleViolation("Extraction timestamp is required.")
        }
        requireDocument(extraction.document)
        val visits = extraction.visits.map { requireVisitCandidate(it) }
        val requirements = requireRequirements(extraction.requirements)
        return extraction.copy(visits = visits, requirements = requirements)
    }

    fun requireDocument(document: ExtractionDocumentInfo): ExtractionDocumentInfo {
        if (document.pageCount < 0 || document.textCharCount < 0) {
            throw ExtractionRuleViolation("Document counts cannot be negative.")
        }
        document.ocrPageIndexes.forEach { page ->
            if (page < 1) {
                throw ExtractionRuleViolation("OCR page indexes must be 1-based.")
            }
        }
        return document
    }

    fun requireVisitCandidate(visit: ExtractionVisitCandidate): ExtractionVisitCandidate {
        visit.confidence?.let { requireConfidence(it) }
        val normalized = RequirementRules.requireVisit(
            RequirementVisit(
                weekday = visit.weekday,
                startTime = visit.startTime,
                endTime = visit.endTime,
                timezone = visit.timezone,
                startsOn = visit.startsOn,
                endsOn = visit.endsOn,
                status = RequirementRules.ScheduleActive,
            ),
        )
        return visit.copy(
            startTime = normalized.startTime,
            endTime = normalized.endTime,
            timezone = normalized.timezone,
            startsOn = normalized.startsOn,
            endsOn = normalized.endsOn,
        )
    }

    fun requireRequirements(requirements: List<ExtractionRequirementCandidate>): List<ExtractionRequirementCandidate> {
        if (requirements.isEmpty()) {
            throw ExtractionRuleViolation("At least one requirement candidate is required.")
        }
        val keys = mutableSetOf<String>()
        return requirements.map { candidate ->
            candidate.confidence?.let { requireConfidence(it) }
            val task = RequirementRules.requireTask(candidate.task)
            val key = candidate.key.trim()
            if (key.isEmpty()) {
                throw ExtractionRuleViolation("Requirement key is required.")
            }
            if (!keys.add(key)) {
                throw ExtractionRuleViolation("Requirement keys must be unique.")
            }
            candidate.copy(key = key, task = task)
        }
    }

    private fun requireConfidence(value: Double) {
        if (value < 0.0 || value > 1.0) {
            throw ExtractionRuleViolation("Confidence must be between 0 and 1.")
        }
    }
}
