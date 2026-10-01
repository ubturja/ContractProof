package com.contractproof.data

data class RequirementDraft(
    val task: String,
    val requiresPhoto: Boolean,
    val isMandatory: Boolean,
    val extractionKey: String?,
)
