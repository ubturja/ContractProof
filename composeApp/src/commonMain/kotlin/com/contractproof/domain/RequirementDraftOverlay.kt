package com.contractproof.domain

data class RequirementDraftOverlay(
    val requirementId: String,
    val evidencePending: Boolean = false,
    val evidenceFailed: Boolean = false,
    val evidenceSyncStatus: EvidenceSyncStatus? = null,
    val exceptionPending: Boolean = false,
    val exceptionFailed: Boolean = false,
)
