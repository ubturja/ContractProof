package com.contractproof.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseOrganizationGateway(
    private val client: SupabaseClient,
    private val users: AuthGateway,
) : OrganizationGateway {
    override suspend fun currentMembership(): Membership? {
        val user = users.currentUser() ?: return null
        return try {
            readMembership(user.id)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapOrganizationFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createOwnerOrganization(
        companyName: String,
        displayName: String,
    ): Membership {
        val user = users.currentUser() ?: throw OrganizationFailure.Rejected
        val name = companyName.trim()
        val ownerName = displayName.trim()
        if (name.isEmpty() || ownerName.isEmpty()) {
            throw OrganizationFailure.Rejected
        }
        try {
            val existing = readMembership(user.id)
            if (existing != null) {
                return existing
            }
            ensureProfile(user, ownerName)
            val organizationId = existingCreatedOrganization(user.id) ?: insertOrganization(user.id, name)
            insertOwnerMembership(organizationId, user.id)
            return readMembership(user.id)
                ?: Membership(
                    organizationId = organizationId,
                    organizationName = name,
                    role = "owner",
                    locationIds = emptyList(),
                    clientId = null,
                )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is OrganizationFailure) throw error
            throw mapOrganizationFailure(error)
        }
    }

    private suspend fun ensureProfile(user: AuthUser, displayName: String) {
        val existing = client.from("users")
            .select(Columns.list("id")) {
                filter {
                    eq("id", user.id)
                }
            }
            .decodeList<UserIdRow>()
        if (existing.isNotEmpty()) {
            return
        }
        client.from("users").insert(
            UserInsert(
                id = user.id,
                email = user.email,
                displayName = displayName,
            ),
        )
    }

    private suspend fun existingCreatedOrganization(userId: String): String? {
        return client.from("organizations")
            .select(Columns.list("id")) {
                filter {
                    eq("created_by", userId)
                }
                limit(1)
            }
            .decodeList<OrganizationIdRow>()
            .firstOrNull()
            ?.id
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun insertOrganization(userId: String, name: String): String {
        val id = Uuid.generateV4().toString()
        client.from("organizations").insert(
            OrganizationInsert(
                id = id,
                name = name,
                createdBy = userId,
            ),
        )
        return id
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun insertOwnerMembership(organizationId: String, userId: String) {
        client.from("organization_members").insert(
            MembershipInsert(
                id = Uuid.generateV4().toString(),
                organizationId = organizationId,
                userId = userId,
                role = "owner",
                clientId = null,
                locationIds = emptyList(),
            ),
        )
    }

    private suspend fun readMembership(userId: String): Membership? {
        val member = client.from("organization_members")
            .select(Columns.list("organization_id", "role", "location_ids", "client_id")) {
                filter {
                    eq("user_id", userId)
                }
                limit(1)
            }
            .decodeList<MembershipRow>()
            .firstOrNull()
            ?: return null
        val organization = client.from("organizations")
            .select(Columns.list("id", "name")) {
                filter {
                    eq("id", member.organizationId)
                }
                limit(1)
            }
            .decodeList<OrganizationRow>()
            .firstOrNull()
            ?: return null
        return Membership(
            organizationId = organization.id,
            organizationName = organization.name,
            role = member.role,
            locationIds = member.locationIds,
            clientId = member.clientId,
        )
    }
}

private fun mapOrganizationFailure(error: Throwable): OrganizationFailure {
    return if (error.isOfflineFailure()) OrganizationFailure.Network else OrganizationFailure.Rejected
}

@Serializable
private data class UserIdRow(
    val id: String,
)

@Serializable
private data class UserInsert(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
private data class OrganizationIdRow(
    val id: String,
)

@Serializable
private data class OrganizationRow(
    val id: String,
    val name: String,
)

@Serializable
private data class OrganizationInsert(
    val id: String,
    val name: String,
    @SerialName("created_by") val createdBy: String,
)

@Serializable
private data class MembershipRow(
    @SerialName("organization_id") val organizationId: String,
    val role: String,
    @SerialName("location_ids") val locationIds: List<String> = emptyList(),
    @SerialName("client_id") val clientId: String? = null,
)

@Serializable
private data class MembershipInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("client_id") val clientId: String?,
    @SerialName("location_ids") val locationIds: List<String>,
)
