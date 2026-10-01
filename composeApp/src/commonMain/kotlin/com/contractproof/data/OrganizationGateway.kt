package com.contractproof.data

data class Membership(
    val organizationId: String,
    val organizationName: String,
    val role: String,
    val locationIds: List<String> = emptyList(),
    val clientId: String? = null,
    val userId: String = "",
)

sealed class OrganizationFailure : Exception() {
    data object Network : OrganizationFailure()

    data object Rejected : OrganizationFailure()
}

interface OrganizationGateway {
    suspend fun currentMembership(): Membership?

    suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership
}
