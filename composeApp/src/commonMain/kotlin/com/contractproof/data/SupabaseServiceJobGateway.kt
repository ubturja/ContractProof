package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ClientRules
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules
import com.contractproof.domain.Role
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobDraft
import com.contractproof.domain.ServiceJobGenerationContext
import com.contractproof.domain.ServiceJobGenerationRules
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRequirement
import com.contractproof.domain.ServiceJobRuleViolation
import com.contractproof.domain.ServiceJobRules
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class SupabaseServiceJobGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
    private val schedules: ScheduleGateway,
    private val requirements: RequirementGateway,
    private val clients: ClientGateway,
    private val locations: LocationGateway,
) : ServiceJobRepository {
    override suspend fun generateForApprovedVersion(
        contractId: String,
        contractVersionId: String,
        horizonDays: Int,
    ) {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        try {
            val contract = client.from("contracts")
                .select {
                    filter {
                        eq("id", contractId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ContractJobContextRow>()
                .firstOrNull()
                ?: throw ServiceJobFailure.Rejected
            val requirementItems = requirements.listForVersion(contractVersionId)
            val scheduleItems = schedules.listForContract(contractId)
            val zone = TimeZone.of(
                scheduleItems.firstOrNull()?.visit?.timezone
                    ?: "UTC",
            )
            val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                .toLocalDateTime(zone)
                .date
            val context = ServiceJobGenerationContext(
                contractId = contractId,
                contractVersionId = contractVersionId,
                clientId = contract.clientId,
                locationId = contract.locationId,
                organizationId = membership.organizationId,
            )
            val drafts = ServiceJobGenerationRules.buildDrafts(
                context = context,
                schedules = scheduleItems,
                requirements = requirementItems,
                today = today,
                horizonDays = horizonDays,
            )
            drafts.forEach { draft ->
                insertJobIfNew(context, draft)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ServiceJobFailure) throw error
            throw if (error.isOfflineFailure()) ServiceJobFailure.Network else ServiceJobFailure.Rejected
        }
    }

    override suspend fun get(jobId: String): ServiceJob? {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        if (!canReadJobs(membership)) {
            throw ServiceJobFailure.Rejected
        }
        return try {
            val row = loadJobRow(membership.organizationId, jobId) ?: return null
            if (!canReadJob(membership, row)) {
                throw ServiceJobFailure.Rejected
            }
            val context = loadAssemblyContext(membership, listOf(row))
            assembleJob(row, loadRequirementsForJob(membership.organizationId, jobId), context)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ServiceJobFailure) throw error
            throw mapServiceJobFailure(error)
        }
    }

    override suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob> {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        if (!canReadJobs(membership)) {
            throw ServiceJobFailure.Rejected
        }
        return try {
            val rows = client.from("service_jobs")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ServiceJobRow>()
            val readable = rows.filter { canReadJob(membership, it) }
            val context = loadAssemblyContext(membership, readable)
            val matching = readable.filter { row ->
                val location = context.locations[row.locationId] ?: return@filter false
                serviceDateFor(row.scheduledStart, location.timezone) == serviceDate
            }
            val requirementsByJob = loadRequirementsForJobs(membership.organizationId, matching.map { it.id })
            matching.map { row ->
                assembleJob(row, requirementsByJob[row.id] ?: emptyList(), context)
            }.sortedBy { it.scheduledStart }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ServiceJobFailure) throw error
            throw mapServiceJobFailure(error)
        }
    }

    override suspend fun listForContract(
        contractId: String,
        fromDate: LocalDate,
        limit: Int,
    ): List<ServiceJob> {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        if (!canReadJobs(membership)) {
            throw ServiceJobFailure.Rejected
        }
        return try {
            val rows = client.from("service_jobs")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                        eq("contract_id", contractId)
                    }
                }
                .decodeList<ServiceJobRow>()
            val readable = rows.filter { canReadJob(membership, it) }
            val context = loadAssemblyContext(membership, readable)
            val matching = readable.filter { row ->
                val location = context.locations[row.locationId] ?: return@filter false
                serviceDateFor(row.scheduledStart, location.timezone) >= fromDate
            }
            val requirementsByJob = loadRequirementsForJobs(membership.organizationId, matching.map { it.id })
            matching.map { row ->
                assembleJob(row, requirementsByJob[row.id] ?: emptyList(), context)
            }.sortedBy { it.scheduledStart }
                .take(limit)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ServiceJobFailure) throw error
            throw mapServiceJobFailure(error)
        }
    }

    override suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob> {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        if (!canReadJobs(membership)) {
            throw ServiceJobFailure.Rejected
        }
        return try {
            val rows = client.from("service_jobs")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                        eq("assigned_user_id", assigneeUserId)
                    }
                }
                .decodeList<ServiceJobRow>()
            val context = loadAssemblyContext(membership, rows)
            val matching = rows.filter { row ->
                val location = context.locations[row.locationId] ?: return@filter false
                serviceDateFor(row.scheduledStart, location.timezone) == serviceDate
            }
            val requirementsByJob = loadRequirementsForJobs(membership.organizationId, matching.map { it.id })
            matching.map { row ->
                assembleJob(row, requirementsByJob[row.id] ?: emptyList(), context)
            }.sortedBy { it.scheduledStart }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ServiceJobFailure) throw error
            throw mapServiceJobFailure(error)
        }
    }

    override suspend fun start(jobId: String, startedAt: String): ServiceJob {
        return mutate(jobId) { job, membership ->
            ServiceJobRules.requireJobAction(
                Access.forMembership(membership.role),
                membership.userId,
                job,
            )
            ServiceJobRules.requireStart(job.status, startedAt)
            JobMutation(
                status = ServiceJobRules.InProgress,
                startedAt = startedAt,
                completedAt = null,
                completedBy = null,
            )
        }
    }

    override suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob {
        return mutate(jobId) { job, membership ->
            ServiceJobRules.requireJobAction(
                Access.forMembership(membership.role),
                membership.userId,
                job,
            )
            ServiceJobRules.requireComplete(job.status, job.requirements, completedAt, completedBy)
            JobMutation(
                status = ServiceJobRules.Completed,
                startedAt = job.startedAt ?: startedAtFromComplete(completedAt),
                completedAt = completedAt,
                completedBy = completedBy,
            )
        }
    }

    override suspend fun markIncomplete(jobId: String): ServiceJob {
        return mutate(jobId) { job, membership ->
            ServiceJobRules.requireJobAction(
                Access.forMembership(membership.role),
                membership.userId,
                job,
            )
            ServiceJobRules.requireMarkIncomplete(job.status)
            JobMutation(
                status = ServiceJobRules.Incomplete,
                startedAt = job.startedAt,
                completedAt = null,
                completedBy = null,
            )
        }
    }

    override suspend fun markDisputed(jobId: String): ServiceJob {
        return mutate(jobId) { job, membership ->
            val access = Access.forMembership(membership.role)
            if (!access.canWriteDisputes && !access.canWriteContracts) {
                throw ServiceJobRuleViolation("You cannot dispute this job.")
            }
            ServiceJobRules.requireMarkDisputed(
                job.status,
                job.startedAt,
                job.completedAt,
                job.completedBy,
            )
            JobMutation(
                status = ServiceJobRules.Disputed,
                startedAt = job.startedAt,
                completedAt = job.completedAt,
                completedBy = job.completedBy,
            )
        }
    }

    private suspend fun mutate(
        jobId: String,
        transform: suspend (ServiceJob, Membership) -> JobMutation,
    ): ServiceJob {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        try {
            val row = loadJobRow(membership.organizationId, jobId)
                ?: throw ServiceJobFailure.Rejected
            if (!canReadJob(membership, row)) {
                throw ServiceJobFailure.Rejected
            }
            val context = loadAssemblyContext(membership, listOf(row))
            val requirements = loadRequirementsForJob(membership.organizationId, jobId)
            val job = assembleJob(row, requirements, context)
            val mutation = transform(job, membership)
            client.from("service_jobs").update(
                {
                    set("status", mutation.status)
                    set("started_at", mutation.startedAt)
                    set("completed_at", mutation.completedAt)
                    set("completed_by", mutation.completedBy)
                },
            ) {
                filter {
                    eq("id", jobId)
                    eq("organization_id", membership.organizationId)
                }
            }
            val updatedRow = loadJobRow(membership.organizationId, jobId)
                ?: throw ServiceJobFailure.Rejected
            return assembleJob(updatedRow, requirements, context)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ServiceJobFailure) throw error
            if (error is ServiceJobRuleViolation) throw ServiceJobFailure.Rejected
            throw mapServiceJobFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun insertJobIfNew(context: ServiceJobGenerationContext, draft: ServiceJobDraft) {
        val jobId = Uuid.generateV4().toString().lowercase()
        val jobRow = ServiceJobInsert(
            id = jobId,
            organizationId = context.organizationId,
            locationId = context.locationId,
            clientId = context.clientId,
            contractId = context.contractId,
            contractVersionId = context.contractVersionId,
            scheduleId = draft.scheduleId,
            scheduledStart = draft.scheduledStart,
            scheduledEnd = draft.scheduledEnd,
        )
        try {
            client.from("service_jobs").insert(jobRow)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (isDuplicateOccurrence(error)) {
                return
            }
            throw error
        }
        draft.requirements.forEach { requirement ->
            val requirementId = Uuid.generateV4().toString().lowercase()
            client.from("service_job_requirements").insert(
                ServiceJobRequirementInsert(
                    id = requirementId,
                    organizationId = context.organizationId,
                    serviceJobId = jobId,
                    contractRequirementId = requirement.contractRequirementId,
                    requirementText = requirement.requirementText,
                    requiresPhoto = requirement.requiresPhoto,
                    isMandatory = requirement.isMandatory,
                    sortOrder = requirement.sortOrder,
                ),
            )
        }
    }

    private suspend fun loadJobRow(organizationId: String, jobId: String): ServiceJobRow? {
        return client.from("service_jobs")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    eq("id", jobId)
                }
            }
            .decodeList<ServiceJobRow>()
            .firstOrNull()
    }

    private suspend fun loadRequirementsForJob(organizationId: String, jobId: String): List<ServiceJobRequirementRow> {
        return loadRequirementsForJobs(organizationId, listOf(jobId))[jobId] ?: emptyList()
    }

    private suspend fun loadRequirementsForJobs(
        organizationId: String,
        jobIds: List<String>,
    ): Map<String, List<ServiceJobRequirementRow>> {
        if (jobIds.isEmpty()) {
            return emptyMap()
        }
        val rows = client.from("service_job_requirements")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    isIn("service_job_id", jobIds)
                }
            }
            .decodeList<ServiceJobRequirementRow>()
        return rows.groupBy { it.serviceJobId }
    }

    private suspend fun loadAssemblyContext(membership: Membership, rows: List<ServiceJobRow>): JobAssemblyContext {
        val access = Access.forMembership(membership.role)
        val clientMap = if (access.canOpenClients) {
            clients.list().associateBy { it.id }
        } else {
            rows.map { it.clientId }.distinct().associateWith { clientId ->
                ClientRecord(
                    id = clientId,
                    organizationId = membership.organizationId,
                    name = "Client",
                    status = ClientRules.Active,
                )
            }
        }
        val locationMap = if (access.canOpenLocations) {
            locations.list().associateBy { it.id }
        } else {
            rows.map { it.locationId }.distinct().associateWith { locationId ->
                LocationRecord(
                    id = locationId,
                    organizationId = membership.organizationId,
                    clientId = "",
                    name = "Location",
                    timezone = "UTC",
                    address = null,
                    zoneCode = null,
                    status = LocationRules.Active,
                )
            }
        }
        val userRows = client.from("users")
            .select()
            .decodeList<UserDisplayRow>()
        val users = userRows.associateBy { it.id }
        return JobAssemblyContext(
            organizationId = membership.organizationId,
            clients = clientMap,
            locations = locationMap,
            users = users,
        )
    }

    private fun assembleJob(
        row: ServiceJobRow,
        requirementRows: List<ServiceJobRequirementRow>,
        context: JobAssemblyContext,
    ): ServiceJob {
        val clientRecord = context.clients[row.clientId]
            ?: throw ServiceJobFailure.Rejected
        val locationRecord = context.locations[row.locationId]
            ?: throw ServiceJobFailure.Rejected
        val requirements = requirementRows
            .sortedBy { it.sortOrder }
            .map { it.toDomain() }
        val evidenceStatus = ServiceJobRules.computeEvidenceStatus(requirements)
        val assignee = row.assignedUserId?.let { userId ->
            val user = context.users[userId]
            ServiceJobAssignee(
                userId = userId,
                displayName = user?.displayName ?: userId,
            )
        }
        return ServiceJob(
            id = row.id,
            organizationId = row.organizationId,
            client = ServiceJobClientRef(clientRecord.id, clientRecord.name),
            location = ServiceJobLocationRef(
                locationRecord.id,
                locationRecord.name,
                locationRecord.timezone,
            ),
            serviceDate = serviceDateFor(row.scheduledStart, locationRecord.timezone),
            scheduledStart = row.scheduledStart,
            scheduledEnd = row.scheduledEnd,
            assignee = assignee,
            status = ServiceJobRules.requireStorageStatus(row.status),
            requirements = requirements,
            evidenceStatus = evidenceStatus,
            contractId = row.contractId,
            contractVersionId = row.contractVersionId,
            scheduleId = row.scheduleId,
            startedAt = row.startedAt,
            completedAt = row.completedAt,
            completedBy = row.completedBy,
        )
    }

    private fun serviceDateFor(scheduledStart: String, timezone: String): LocalDate {
        val zone = TimeZone.of(timezone)
        return Instant.parse(scheduledStart).toLocalDateTime(zone).date
    }

    private fun canReadJobs(membership: Membership): Boolean {
        val access = Access.forMembership(membership.role)
        return access.canOpenAssignedJobs ||
            access.canOpenContracts ||
            access.canOpenService ||
            access.canOpenClientServiceRecords
    }

    private fun canReadJob(membership: Membership, row: ServiceJobRow): Boolean {
        val access = Access.forMembership(membership.role)
        if (access.canOpenClientServiceRecords) {
            val clientId = membership.clientId
            return clientId != null &&
                row.clientId == clientId &&
                (row.status == ServiceJobRules.Completed || row.status == ServiceJobRules.Disputed)
        }
        if (access.canWriteContracts || access.canOpenService) {
            if (membership.role == "manager") {
                return Role.Manager.allowsLocation(row.locationId, membership.locationIds)
            }
            return true
        }
        if (access.canOpenAssignedJobs) {
            return row.assignedUserId == membership.userId
        }
        return false
    }

    private fun startedAtFromComplete(completedAt: String): String {
        return completedAt
    }

    private fun isDuplicateOccurrence(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: return false
        return message.contains("one_job_per_occurrence") ||
            message.contains("duplicate key") ||
            message.contains("unique constraint")
    }

    private fun mapServiceJobFailure(error: Throwable): ServiceJobFailure {
        return if (error.isOfflineFailure()) ServiceJobFailure.Network else ServiceJobFailure.Rejected
    }

    private data class JobMutation(
        val status: String,
        val startedAt: String?,
        val completedAt: String?,
        val completedBy: String?,
    )

    private data class JobAssemblyContext(
        val organizationId: String,
        val clients: Map<String, ClientRecord>,
        val locations: Map<String, LocationRecord>,
        val users: Map<String, UserDisplayRow>,
    )
}

@Serializable
private data class ContractJobContextRow(
    @SerialName("client_id") val clientId: String,
    @SerialName("location_id") val locationId: String,
)

@Serializable
private data class ServiceJobInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("location_id") val locationId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("contract_id") val contractId: String,
    @SerialName("contract_version_id") val contractVersionId: String,
    @SerialName("schedule_id") val scheduleId: String,
    @SerialName("scheduled_start") val scheduledStart: String,
    @SerialName("scheduled_end") val scheduledEnd: String,
)

@Serializable
private data class ServiceJobRequirementInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("contract_requirement_id") val contractRequirementId: String,
    @SerialName("requirement_text") val requirementText: String,
    @SerialName("requires_photo") val requiresPhoto: Boolean,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    val status: String = "missing",
)

@Serializable
private data class ServiceJobRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("location_id") val locationId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("contract_id") val contractId: String,
    @SerialName("contract_version_id") val contractVersionId: String,
    @SerialName("schedule_id") val scheduleId: String? = null,
    @SerialName("assigned_user_id") val assignedUserId: String? = null,
    @SerialName("scheduled_start") val scheduledStart: String,
    @SerialName("scheduled_end") val scheduledEnd: String,
    val status: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("completed_by") val completedBy: String? = null,
)

@Serializable
private data class ServiceJobRequirementRow(
    val id: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("contract_requirement_id") val contractRequirementId: String,
    @SerialName("requirement_text") val requirementText: String,
    @SerialName("requires_photo") val requiresPhoto: Boolean,
    @SerialName("is_mandatory") val isMandatory: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    val status: String,
)

@Serializable
private data class UserDisplayRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
)

private fun ServiceJobRequirementRow.toDomain(): ServiceJobRequirement {
    return ServiceJobRequirement(
        id = id,
        contractRequirementId = contractRequirementId,
        requirementText = requirementText,
        requiresPhoto = requiresPhoto,
        isMandatory = isMandatory,
        sortOrder = sortOrder,
        status = JobRequirementStatus.fromStorage(status),
    )
}
