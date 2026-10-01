package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.RequirementRuleViolation
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.RequirementVisit
import com.contractproof.domain.ScheduleFrequency
import com.contractproof.domain.ScheduleRecord
import com.contractproof.domain.ScheduleRuleViolation
import com.contractproof.domain.ScheduleRules
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseScheduleGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
) : ScheduleGateway {
    override suspend fun listForContract(contractId: String): List<ScheduleRecord> {
        val membership = organizations.currentMembership() ?: throw ScheduleFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw ScheduleFailure.Rejected
        }
        return try {
            client.from("service_schedules")
                .select {
                    filter {
                        eq("contract_id", contractId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ServiceScheduleRow>()
                .map { it.toRecord(contractId) }
                .sortedBy { it.visit.weekday }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapScheduleFailure(error)
        }
    }

    override suspend fun upsertWeeklyVisit(
        contractVersionId: String,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord {
        return upsertSchedule(contractVersionId, ScheduleFrequency.Weekly, scheduleId, visit)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun upsertSchedule(
        contractVersionId: String,
        frequency: ScheduleFrequency,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord {
        val membership = organizations.currentMembership() ?: throw ScheduleFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadVersionContext(membership.organizationId, contractVersionId)
            RequirementRules.requireEditable(context.versionStatus)
            val weekday = ScheduleRules.requireWeekdayForFrequency(frequency, visit.weekday)
            val normalized = RequirementRules.requireVisit(visit.copy(weekday = weekday))
            val frequencyValue = frequency.toStorageValue()
            if (scheduleId == null) {
                val id = Uuid.generateV4().toString().lowercase()
                val row = ScheduleInsert(
                    id = id,
                    organizationId = membership.organizationId,
                    contractId = context.contractId,
                    locationId = context.locationId,
                    frequency = frequencyValue,
                    weekday = normalized.weekday,
                    startTime = normalized.startTime,
                    endTime = normalized.endTime,
                    timezone = normalized.timezone,
                    startsOn = normalized.startsOn,
                    endsOn = normalized.endsOn,
                    status = normalized.status,
                )
                client.from("service_schedules").insert(row)
                return row.toRecord(context.contractId)
            }
            client.from("service_schedules").update(
                {
                    set("frequency", frequencyValue)
                    set("weekday", normalized.weekday)
                    set("start_time", normalized.startTime)
                    set("end_time", normalized.endTime)
                    set("timezone", normalized.timezone)
                    set("starts_on", normalized.startsOn)
                    set("ends_on", normalized.endsOn)
                    set("status", normalized.status)
                },
            ) {
                filter {
                    eq("id", scheduleId)
                    eq("contract_id", context.contractId)
                    eq("organization_id", membership.organizationId)
                }
            }
            return ScheduleRecord(
                id = scheduleId,
                contractId = context.contractId,
                locationId = context.locationId,
                frequency = frequencyValue,
                visit = normalized,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation || error is ScheduleRuleViolation) {
                throw ScheduleFailure.Rejected
            }
            throw mapScheduleFailure(error)
        }
    }

    override suspend fun setStatus(
        contractVersionId: String,
        scheduleId: String,
        status: String,
    ): ScheduleRecord {
        val membership = organizations.currentMembership() ?: throw ScheduleFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            RequirementRules.requireWrite(access)
            val context = loadVersionContext(membership.organizationId, contractVersionId)
            RequirementRules.requireEditable(context.versionStatus)
            val normalizedStatus = status.trim()
            if (normalizedStatus != RequirementRules.ScheduleActive &&
                normalizedStatus != RequirementRules.SchedulePaused &&
                normalizedStatus != RequirementRules.ScheduleEnded
            ) {
                throw RequirementRuleViolation("Visit status is invalid.")
            }
            val existing = client.from("service_schedules")
                .select {
                    filter {
                        eq("id", scheduleId)
                        eq("contract_id", context.contractId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ServiceScheduleRow>()
                .firstOrNull()
                ?: throw ScheduleFailure.Rejected
            client.from("service_schedules").update(
                {
                    set("status", normalizedStatus)
                },
            ) {
                filter {
                    eq("id", scheduleId)
                    eq("contract_id", context.contractId)
                    eq("organization_id", membership.organizationId)
                }
            }
            val visit = existing.toVisit().copy(status = normalizedStatus)
            return existing.toRecord(context.contractId).copy(visit = visit)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is RequirementRuleViolation) throw ScheduleFailure.Rejected
            throw mapScheduleFailure(error)
        }
    }

    private suspend fun loadVersionContext(
        organizationId: String,
        contractVersionId: String,
    ): VersionScheduleContext {
        val version = client.from("contract_versions")
            .select {
                filter {
                    eq("id", contractVersionId)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<ScheduleVersionLookupRow>()
            .firstOrNull()
            ?: throw ScheduleFailure.Rejected
        val contract = client.from("contracts")
            .select {
                filter {
                    eq("id", version.contractId)
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<ScheduleContractLookupRow>()
            .firstOrNull()
            ?: throw ScheduleFailure.Rejected
        return VersionScheduleContext(
            contractId = contract.id,
            locationId = contract.locationId,
            versionStatus = version.status,
        )
    }
}

private fun mapScheduleFailure(error: Throwable): ScheduleFailure {
    if (error is ScheduleFailure) {
        return error
    }
    return if (error.isOfflineFailure()) ScheduleFailure.Network else ScheduleFailure.Rejected
}

private data class VersionScheduleContext(
    val contractId: String,
    val locationId: String,
    val versionStatus: String,
)

@Serializable
private data class ScheduleVersionLookupRow(
    val id: String,
    val status: String,
    @SerialName("contract_id") val contractId: String,
)

@Serializable
private data class ScheduleContractLookupRow(
    val id: String,
    @SerialName("location_id") val locationId: String,
)

@Serializable
private data class ServiceScheduleRow(
    val id: String,
    @SerialName("contract_id") val contractId: String,
    @SerialName("location_id") val locationId: String,
    val frequency: String = ScheduleFrequency.STORAGE_WEEKLY,
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

    fun toRecord(fallbackContractId: String): ScheduleRecord {
        return ScheduleRecord(
            id = id,
            contractId = contractId.ifEmpty { fallbackContractId },
            locationId = locationId,
            frequency = frequency,
            visit = toVisit(),
        )
    }
}

@Serializable
private data class ScheduleInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("contract_id") val contractId: String,
    @SerialName("location_id") val locationId: String,
    val frequency: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val timezone: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String? = null,
    val status: String,
) {
    fun toRecord(contractId: String): ScheduleRecord {
        return ScheduleRecord(
            id = id,
            contractId = contractId,
            locationId = locationId,
            frequency = frequency,
            visit = RequirementVisit(
                weekday = weekday,
                startTime = startTime,
                endTime = endTime,
                timezone = timezone,
                startsOn = startsOn,
                endsOn = endsOn,
                status = status,
            ),
        )
    }
}
