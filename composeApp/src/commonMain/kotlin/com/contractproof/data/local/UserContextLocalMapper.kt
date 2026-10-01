package com.contractproof.data.local

import com.contractproof.data.Membership
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object UserContextLocalMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toMembership(row: User_context, userId: String): Membership {
        val locationIds = json.decodeFromString(
            ListSerializer(String.serializer()),
            row.location_ids_json,
        )
        return Membership(
            organizationId = row.organization_id,
            organizationName = row.organization_name,
            role = row.role,
            locationIds = locationIds,
            clientId = row.client_id,
            userId = userId,
        )
    }

    fun fromMembership(email: String, membership: Membership, cachedAt: String): UserContextValues {
        return UserContextValues(
            userId = membership.userId,
            email = email,
            organizationId = membership.organizationId,
            organizationName = membership.organizationName,
            role = membership.role,
            locationIdsJson = json.encodeToString(
                ListSerializer(String.serializer()),
                membership.locationIds,
            ),
            clientId = membership.clientId,
            cachedAt = cachedAt,
        )
    }
}

data class UserContextValues(
    val userId: String,
    val email: String,
    val organizationId: String,
    val organizationName: String,
    val role: String,
    val locationIdsJson: String,
    val clientId: String?,
    val cachedAt: String,
)
