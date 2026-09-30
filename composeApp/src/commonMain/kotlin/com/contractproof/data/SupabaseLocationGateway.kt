package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRuleViolation
import com.contractproof.domain.LocationRules
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseLocationGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
) : LocationGateway {
    override suspend fun list(): List<LocationRecord> {
        val membership = organizations.currentMembership() ?: throw LocationFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenLocations) {
            throw LocationFailure.Rejected
        }
        return try {
            client.from("locations")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<LocationRow>()
                .map { it.toRecord() }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapLocationFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(
        clientId: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
    ): LocationRecord {
        val membership = organizations.currentMembership() ?: throw LocationFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            LocationRules.requireWrite(access)
            val clientOrgId = clientOrganizationId(clientId, membership.organizationId) ?: throw LocationFailure.Rejected
            LocationRules.requireClient(clientOrgId, membership.organizationId)
            val record = LocationRecord(
                id = Uuid.generateV4().toString(),
                organizationId = membership.organizationId,
                clientId = clientId,
                name = LocationRules.requireName(name),
                timezone = LocationRules.requireTimezone(timezone),
                address = LocationRules.optionalText(address),
                zoneCode = LocationRules.optionalText(zoneCode),
                status = LocationRules.Active,
            )
            LocationRules.requireOrganization(record.organizationId, membership.organizationId)
            LocationRules.requireStatus(record.status)
            client.from("locations").insert(record.toRow())
            return record
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is LocationRuleViolation) throw LocationFailure.Rejected
            throw mapLocationFailure(error)
        }
    }

    override suspend fun update(
        id: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
        status: String,
    ): LocationRecord {
        val membership = organizations.currentMembership() ?: throw LocationFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            LocationRules.requireWrite(access)
            LocationRules.requireStatus(status)
            val existing = list().find { it.id == id } ?: throw LocationFailure.Rejected
            LocationRules.requireOrganization(existing.organizationId, membership.organizationId)
            val record = existing.copy(
                name = LocationRules.requireName(name),
                timezone = LocationRules.requireTimezone(timezone),
                address = LocationRules.optionalText(address),
                zoneCode = LocationRules.optionalText(zoneCode),
                status = status,
            )
            client.from("locations").update(
                {
                    set("name", record.name)
                    set("timezone", record.timezone)
                    set("address", record.address)
                    set("zone_code", record.zoneCode)
                    set("status", record.status)
                },
            ) {
                filter {
                    eq("id", id)
                    eq("organization_id", membership.organizationId)
                }
            }
            return record
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is LocationRuleViolation) throw LocationFailure.Rejected
            throw mapLocationFailure(error)
        }
    }

    private suspend fun clientOrganizationId(clientId: String, organizationId: String): String? {
        return client.from("clients")
            .select {
                filter {
                    eq("id", clientId)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<LocationClientRow>()
            .firstOrNull()
            ?.organizationId
    }
}

private fun mapLocationFailure(error: Throwable): LocationFailure {
    if (error is LocationFailure) {
        return error
    }
    return if (error.isOfflineFailure()) LocationFailure.Network else LocationFailure.Rejected
}

@Serializable
private data class LocationClientRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
)

@Serializable
private data class LocationRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("client_id") val clientId: String,
    val name: String,
    val timezone: String,
    val address: String? = null,
    @SerialName("zone_code") val zoneCode: String? = null,
    val status: String,
) {
    fun toRecord(): LocationRecord {
        return LocationRecord(
            id = id,
            organizationId = organizationId,
            clientId = clientId,
            name = name,
            timezone = timezone,
            address = address,
            zoneCode = zoneCode,
            status = status,
        )
    }
}

private fun LocationRecord.toRow(): LocationRow {
    return LocationRow(
        id = id,
        organizationId = organizationId,
        clientId = clientId,
        name = name,
        timezone = timezone,
        address = address,
        zoneCode = zoneCode,
        status = status,
    )
}
