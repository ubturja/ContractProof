package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ClientRuleViolation
import com.contractproof.domain.ClientRules
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseClientGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
) : ClientGateway {
    override suspend fun list(): List<ClientRecord> {
        val membership = organizations.currentMembership() ?: throw ClientFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenClients) {
            throw ClientFailure.Rejected
        }
        return try {
            client.from("clients")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ClientRow>()
                .map { it.toRecord() }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapClientFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(name: String): ClientRecord {
        val membership = organizations.currentMembership() ?: throw ClientFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            ClientRules.requireWrite(access)
            val trimmed = ClientRules.requireName(name)
            ClientRules.requireOrganization(membership.organizationId, membership.organizationId)
            val record = ClientRecord(
                id = Uuid.generateV4().toString(),
                organizationId = membership.organizationId,
                name = trimmed,
                status = ClientRules.Active,
            )
            ClientRules.requireStatus(record.status)
            client.from("clients").insert(record.toRow())
            return record
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ClientRuleViolation) throw ClientFailure.Rejected
            throw mapClientFailure(error)
        }
    }

    override suspend fun update(id: String, name: String, status: String): ClientRecord {
        val membership = organizations.currentMembership() ?: throw ClientFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            ClientRules.requireWrite(access)
            val trimmed = ClientRules.requireName(name)
            ClientRules.requireStatus(status)
            val record = ClientRecord(
                id = id,
                organizationId = membership.organizationId,
                name = trimmed,
                status = status,
            )
            ClientRules.requireOrganization(record.organizationId, membership.organizationId)
            client.from("clients").update(
                {
                    set("name", record.name)
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
            if (error is ClientRuleViolation) throw ClientFailure.Rejected
            throw mapClientFailure(error)
        }
    }
}

private fun mapClientFailure(error: Throwable): ClientFailure {
    if (error is ClientFailure) {
        return error
    }
    return if (error.isOfflineFailure()) ClientFailure.Network else ClientFailure.Rejected
}

@Serializable
private data class ClientRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val name: String,
    val status: String,
) {
    fun toRecord(): ClientRecord {
        return ClientRecord(
            id = id,
            organizationId = organizationId,
            name = name,
            status = status,
        )
    }
}

private fun ClientRecord.toRow(): ClientRow {
    return ClientRow(
        id = id,
        organizationId = organizationId,
        name = name,
        status = status,
    )
}
