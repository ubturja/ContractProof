package com.contractproof.domain

data class DisputeContractRef(
    val contractId: String,
    val contractTitle: String,
    val versionLabel: String,
)

data class DisputeReconstructionBundle(
    val dispute: Dispute,
    val items: List<DisputeItem>,
    val job: ServiceJob,
    val disputedRequirementText: String,
    val contract: DisputeContractRef,
    val evidence: List<Evidence>,
    val exceptions: List<JobExceptionRecord>,
    val timeline: List<DisputeTimelineEvent>,
    val missingRequirementLabels: List<String>,
)

enum class DisputeTimelineCategory {
    Job,
    Evidence,
    Exception,
    Acknowledgement,
    MissingEvidence,
    DisputeFiled,
}

data class DisputeTimelineEvent(
    val occurredAt: String,
    val category: DisputeTimelineCategory,
    val title: String,
    val body: String,
    val sortKey: Int = category.ordinal,
)
