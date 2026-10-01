package com.contractproof.domain

import kotlin.random.Random

data class ReviewRequirementDraft(
    val localId: String,
    val extractionKey: String?,
    val task: String,
    val requiresPhoto: Boolean,
    val isMandatory: Boolean,
    val confidence: Double?,
    val evidenceQuote: String?,
    val fromExtraction: Boolean,
)

class ExtractionReviewRuleViolation(message: String) : IllegalArgumentException(message)

object ExtractionReviewRules {
    const val UnapprovedBanner = "AI-generated — not approved until you confirm."

    fun draftsFromExtraction(extraction: ContractExtractionV1): List<ReviewRequirementDraft> {
        val normalized = ExtractionRules.requireCompleted(extraction)
        return normalized.requirements.map { requirement ->
            ReviewRequirementDraft(
                localId = newLocalId(),
                extractionKey = requirement.key,
                task = requirement.task,
                requiresPhoto = requirement.requiresPhoto,
                isMandatory = requirement.isMandatory,
                confidence = requirement.confidence,
                evidenceQuote = requirement.evidenceQuote,
                fromExtraction = true,
            )
        }
    }

    fun newManualDraft(): ReviewRequirementDraft {
        return ReviewRequirementDraft(
            localId = newLocalId(),
            extractionKey = null,
            task = "",
            requiresPhoto = true,
            isMandatory = true,
            confidence = null,
            evidenceQuote = null,
            fromExtraction = false,
        )
    }

    fun requireCanApprove(drafts: List<ReviewRequirementDraft>) {
        val valid = drafts.count { it.task.trim().isNotEmpty() }
        if (valid < 1) {
            throw ExtractionReviewRuleViolation("Add at least one requirement before approving.")
        }
    }

    fun confidenceLabel(confidence: Double?): String? {
        if (confidence == null) {
            return null
        }
        val percent = (confidence * 100).toInt()
        return if (confidence < 0.6) {
            "Low confidence ($percent%)"
        } else {
            "Confidence $percent%"
        }
    }

    fun moveInOrder(drafts: List<ReviewRequirementDraft>, localId: String, delta: Int): List<ReviewRequirementDraft> {
        val ids = drafts.map { it.localId }
        val movedIds = RequirementRules.moveInOrder(ids, localId, delta)
        return movedIds.map { id -> drafts.first { it.localId == id } }
    }

    fun newLocalId(): String {
        return "draft_${Random.nextInt(0, Int.MAX_VALUE)}"
    }
}
