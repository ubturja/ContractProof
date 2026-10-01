package com.contractproof.data

import com.contractproof.domain.RequirementRecord

data class RequirementVersionHeader(
    val contractId: String,
    val locationId: String,
    val locationName: String,
    val zoneCode: String?,
    val contractStartsOn: String,
    val versionStatus: String,
)

sealed class RequirementFailure : Exception() {
    data object Network : RequirementFailure()

    data object Rejected : RequirementFailure()
}

interface RequirementGateway {
    suspend fun headerForVersion(contractVersionId: String): RequirementVersionHeader

    suspend fun listForVersion(contractVersionId: String): List<RequirementRecord>

    suspend fun create(
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord

    suspend fun update(
        id: String,
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord

    suspend fun delete(id: String, contractVersionId: String)

    suspend fun reorder(contractVersionId: String, orderedIds: List<String>)

    suspend fun replaceAllForVersion(contractVersionId: String, items: List<RequirementDraft>)
}
