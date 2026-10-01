package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.RequirementRecord
import com.contractproof.domain.RequirementRuleViolation
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.RequirementVisit
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseRequirementGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
) : RequirementGateway {
    override suspend fun headerForVersion(contractVersionId: String): RequirementVersionHeader {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw RequirementFailure.Rejected
        }
        return try {
            val context = loadContext(membership.organizationId, contractVersionId)
            RequirementVersionHeader(
                contractId = context.contractId,
                locationId = context.locationId,
                locationName = context.locationName,
                zoneCode = context.zoneCode,
                contractStartsOn = context.contractStartsOn,
                versionStatus = context.versionStatus,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapRequirementFailure(error)
        }
    }

    override suspend fun listForVersion(contractVersionId: String): List<RequirementRecord> {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw RequirementFailure.Rejected
        }
        return try {
            val context = loadContext(membership.organizationId, contractVersionId)
            loadRequirements(membership.organizationId, contractVersionId).map { row ->
                row.toRecord(context)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapRequirementFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadContext(membership.organizationId, contractVersionId)
            RequirementRules.requireOrganization(context.organizationId, membership.organizationId)
            RequirementRules.requireEditable(context.versionStatus)
            val recordTask = RequirementRules.requireTask(task)
            val order = RequirementRules.requireSortOrder(sortOrder)
            val row = RequirementInsert(
                id = Uuid.generateV4().toString().lowercase(),
                organizationId = membership.organizationId,
                contractVersionId = contractVersionId,
                sortOrder = order,
                requirementText = recordTask,
                requiresPhoto = requiresPhoto,
                isMandatory = isMandatory,
                source = RequirementRules.SourceManual,
            )
            client.from("contract_requirements").insert(row)
            return row.toRecord(context)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation) throw RequirementFailure.Rejected
            throw mapRequirementFailure(error)
        }
    }

    override suspend fun update(
        id: String,
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadContext(membership.organizationId, contractVersionId)
            RequirementRules.requireOrganization(context.organizationId, membership.organizationId)
            RequirementRules.requireEditable(context.versionStatus)
            val existing = loadRequirements(membership.organizationId, contractVersionId)
                .firstOrNull { it.id == id }
                ?: throw RequirementFailure.Rejected
            RequirementRules.requireManualSource(existing.source)
            val recordTask = RequirementRules.requireTask(task)
            val order = RequirementRules.requireSortOrder(sortOrder)
            client.from("contract_requirements").update(
                {
                    set("requirement_text", recordTask)
                    set("requires_photo", requiresPhoto)
                    set("is_mandatory", isMandatory)
                    set("sort_order", order)
                },
            ) {
                filter {
                    eq("id", id)
                    eq("contract_version_id", contractVersionId)
                    eq("organization_id", membership.organizationId)
                }
            }
            return RequirementRecord(
                id = id,
                organizationId = context.organizationId,
                contractId = context.contractId,
                contractVersionId = contractVersionId,
                locationId = context.locationId,
                locationName = context.locationName,
                zoneCode = context.zoneCode,
                task = recordTask,
                sortOrder = order,
                requiresPhoto = requiresPhoto,
                isMandatory = isMandatory,
                source = RequirementRules.SourceManual,
                extractionKey = null,
                isActive = RequirementRules.isActive(context.versionStatus),
                visits = context.visits,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation) throw RequirementFailure.Rejected
            throw mapRequirementFailure(error)
        }
    }

    override suspend fun delete(id: String, contractVersionId: String) {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadContext(membership.organizationId, contractVersionId)
            RequirementRules.requireOrganization(context.organizationId, membership.organizationId)
            RequirementRules.requireEditable(context.versionStatus)
            val existing = loadRequirements(membership.organizationId, contractVersionId)
                .firstOrNull { it.id == id }
                ?: throw RequirementFailure.Rejected
            RequirementRules.requireManualSource(existing.source)
            client.from("contract_requirements").delete {
                filter {
                    eq("id", id)
                    eq("contract_version_id", contractVersionId)
                    eq("organization_id", membership.organizationId)
                }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation) throw RequirementFailure.Rejected
            throw mapRequirementFailure(error)
        }
    }

    override suspend fun reorder(contractVersionId: String, orderedIds: List<String>) {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadContext(membership.organizationId, contractVersionId)
            RequirementRules.requireOrganization(context.organizationId, membership.organizationId)
            RequirementRules.requireEditable(context.versionStatus)
            val existing = loadRequirements(membership.organizationId, contractVersionId)
            val existingIds = existing.map { it.id }.toSet()
            if (orderedIds.toSet() != existingIds || orderedIds.size != existing.size) {
                throw RequirementRuleViolation("Requirement order does not match the saved list.")
            }
            val pairs = RequirementRules.reorderSortOrders(orderedIds)
            val offset = existing.maxOfOrNull { it.sortOrder }?.plus(1_000) ?: 1_000
            pairs.forEachIndexed { index, (requirementId, _) ->
                client.from("contract_requirements").update(
                    {
                        set("sort_order", offset + index)
                    },
                ) {
                    filter {
                        eq("id", requirementId)
                        eq("contract_version_id", contractVersionId)
                        eq("organization_id", membership.organizationId)
                    }
                }
            }
            pairs.forEach { (requirementId, order) ->
                client.from("contract_requirements").update(
                    {
                        set("sort_order", order)
                    },
                ) {
                    filter {
                        eq("id", requirementId)
                        eq("contract_version_id", contractVersionId)
                        eq("organization_id", membership.organizationId)
                    }
                }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation) throw RequirementFailure.Rejected
            throw mapRequirementFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun replaceAllForVersion(
        contractVersionId: String,
        items: List<RequirementDraft>,
    ) {
        val membership = organizations.currentMembership() ?: throw RequirementFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadContext(membership.organizationId, contractVersionId)
            RequirementRules.requireOrganization(context.organizationId, membership.organizationId)
            RequirementRules.requireEditable(context.versionStatus)
            if (items.isEmpty()) {
                throw RequirementRuleViolation("At least one requirement is required.")
            }
            client.from("contract_requirements").delete {
                filter {
                    eq("contract_version_id", contractVersionId)
                    eq("organization_id", membership.organizationId)
                }
            }
            items.forEachIndexed { index, draft ->
                val task = RequirementRules.requireTask(draft.task)
                val source = if (draft.extractionKey != null) {
                    RequirementRules.SourceExtraction
                } else {
                    RequirementRules.SourceManual
                }
                val row = RequirementInsert(
                    id = Uuid.generateV4().toString().lowercase(),
                    organizationId = membership.organizationId,
                    contractVersionId = contractVersionId,
                    sortOrder = index,
                    requirementText = task,
                    requiresPhoto = draft.requiresPhoto,
                    isMandatory = draft.isMandatory,
                    source = source,
                    extractionKey = draft.extractionKey,
                )
                client.from("contract_requirements").insert(row)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation) throw RequirementFailure.Rejected
            throw mapRequirementFailure(error)
        }
    }

    private suspend fun loadContext(
        organizationId: String,
        contractVersionId: String,
    ): RequirementContext {
        val version = client.from("contract_versions")
            .select {
                filter {
                    eq("id", contractVersionId)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<VersionLookupRow>()
            .firstOrNull()
            ?: throw RequirementFailure.Rejected
        val contract = client.from("contracts")
            .select {
                filter {
                    eq("id", version.contractId)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<ContractLookupRow>()
            .firstOrNull()
            ?: throw RequirementFailure.Rejected
        val contractStartsOn = contract.startsOn
        val location = client.from("locations")
            .select {
                filter {
                    eq("id", contract.locationId)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<LocationLookupRow>()
            .firstOrNull()
            ?: throw RequirementFailure.Rejected
        val visits = client.from("service_schedules")
            .select {
                filter {
                    eq("contract_id", contract.id)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<ScheduleRow>()
            .map { it.toVisit() }
            .sortedBy { it.weekday }
        RequirementRules.requireVisits(visits)
        return RequirementContext(
            organizationId = organizationId,
            contractId = contract.id,
            contractStartsOn = contractStartsOn,
            locationId = location.id,
            locationName = location.name,
            zoneCode = location.zoneCode,
            versionStatus = version.status,
            visits = visits,
        )
    }

    private suspend fun loadRequirements(
        organizationId: String,
        contractVersionId: String,
    ): List<RequirementRow> {
        return client.from("contract_requirements")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    eq("contract_version_id", contractVersionId)
                }
            }
            .decodeList<RequirementRow>()
            .sortedBy { it.sortOrder }
    }
}

private fun mapRequirementFailure(error: Throwable): RequirementFailure {
    if (error is RequirementFailure) {
        return error
    }
    return if (error.isOfflineFailure()) RequirementFailure.Network else RequirementFailure.Rejected
}

private data class RequirementContext(
    val organizationId: String,
    val contractId: String,
    val contractStartsOn: String,
    val locationId: String,
    val locationName: String,
    val zoneCode: String?,
    val versionStatus: String,
    val visits: List<RequirementVisit>,
)

@Serializable
private data class VersionLookupRow(
    val id: String,
    val status: String,
    @SerialName("contract_id") val contractId: String,
)

@Serializable
private data class ContractLookupRow(
    val id: String,
    @SerialName("location_id") val locationId: String,
    @SerialName("starts_on") val startsOn: String,
)

@Serializable
private data class LocationLookupRow(
    val id: String,
    val name: String,
    @SerialName("zone_code") val zoneCode: String? = null,
)

@Serializable
private data class ScheduleRow(
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val timezone: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String? = null,
    val status: String,
) {
    fun toVisit(): RequirementVisit {
        return RequirementVisit(
            weekday = weekday,
            startTime = startTime,
            endTime = endTime,
            timezone = timezone,
            startsOn = startsOn,
            endsOn = endsOn,
            status = status,
        )
    }
}

@Serializable
private data class RequirementRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("contract_version_id") val contractVersionId: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("requirement_text") val requirementText: String,
    @SerialName("requires_photo") val requiresPhoto: Boolean,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    val source: String,
    @SerialName("extraction_key") val extractionKey: String? = null,
) {
    fun toRecord(context: RequirementContext): RequirementRecord {
        return RequirementRecord(
            id = id,
            organizationId = organizationId,
            contractId = context.contractId,
            contractVersionId = contractVersionId,
            locationId = context.locationId,
            locationName = context.locationName,
            zoneCode = context.zoneCode,
            task = requirementText,
            sortOrder = sortOrder,
            requiresPhoto = requiresPhoto,
            isMandatory = isMandatory,
            source = source,
            extractionKey = extractionKey,
            isActive = RequirementRules.isActive(context.versionStatus),
            visits = context.visits,
        )
    }
}

@Serializable
private data class RequirementInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("contract_version_id") val contractVersionId: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("requirement_text") val requirementText: String,
    @SerialName("requires_photo") val requiresPhoto: Boolean,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    val source: String,
    @SerialName("extraction_key") val extractionKey: String? = null,
) {
    fun toRecord(context: RequirementContext): RequirementRecord {
        return RequirementRecord(
            id = id,
            organizationId = organizationId,
            contractId = context.contractId,
            contractVersionId = contractVersionId,
            locationId = context.locationId,
            locationName = context.locationName,
            zoneCode = context.zoneCode,
            task = requirementText,
            sortOrder = sortOrder,
            requiresPhoto = requiresPhoto,
            isMandatory = isMandatory,
            source = source,
            extractionKey = extractionKey,
            isActive = RequirementRules.isActive(context.versionStatus),
            visits = context.visits,
        )
    }
}
