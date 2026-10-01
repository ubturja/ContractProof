package com.contractproof.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisputeAiSummary(
    val allegation: String,
    val requirements: List<DisputeAiRequirementSection> = emptyList(),
    @SerialName("recorded_evidence") val recordedEvidence: List<String> = emptyList(),
    @SerialName("missing_evidence") val missingEvidence: List<String> = emptyList(),
    val exceptions: List<String> = emptyList(),
    @SerialName("neutral_overview") val neutralOverview: String = "",
)

@Serializable
data class DisputeAiRequirementSection(
    val requirementText: String,
    val contractualContext: String = "",
)
