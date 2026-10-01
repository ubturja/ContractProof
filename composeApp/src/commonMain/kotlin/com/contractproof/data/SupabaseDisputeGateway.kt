package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeAiSummary
import com.contractproof.domain.DisputeAssemblyInput
import com.contractproof.domain.DisputeAssemblyRules
import com.contractproof.domain.DisputeContractRef
import com.contractproof.domain.DisputeDraft
import com.contractproof.domain.DisputeReconstructionBundle
import com.contractproof.domain.DisputeReconstructionRules
import com.contractproof.domain.DisputeRuleViolation
import com.contractproof.domain.DisputeRules
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.uploadAsFlow
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabaseDisputeGateway(
    private val client: SupabaseClient,
    private val config: SupabaseConfig,
    private val organizations: OrganizationGateway,
    private val auth: AuthGateway,
    private val clients: ClientGateway,
    private val locations: LocationGateway,
    private val serviceJobs: ServiceJobRepository,
) : DisputeGateway {
    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun list(): List<Dispute> {
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        if (!Access.forMembership(membership.role).canOpenDisputes) {
            throw DisputeFailure.Rejected("Disputes are not available.")
        }
        return try {
            val rows = client.from("disputes")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<DisputeRow>()
            val clientNames = clients.list().associate { it.id to it.name }
            val locationNames = locations.list().associate { it.id to it.name }
            rows.map { row ->
                DisputeMapper.dispute(
                    row = row,
                    clientName = clientNames[row.clientId] ?: row.clientId,
                    locationName = locationNames[row.locationId] ?: row.locationId,
                )
            }.sortedByDescending { it.createdAt }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapFailure(error)
        }
    }

    override suspend fun get(id: String): Dispute? {
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        if (!Access.forMembership(membership.role).canOpenDisputes) {
            throw DisputeFailure.Rejected("Disputes are not available.")
        }
        return try {
            val row = client.from("disputes")
                .select {
                    filter {
                        eq("id", id)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<DisputeRow>()
                .firstOrNull()
                ?: return null
            val clientName = clients.list().firstOrNull { it.id == row.clientId }?.name ?: row.clientId
            val locationName = locations.list().firstOrNull { it.id == row.locationId }?.name ?: row.locationId
            DisputeMapper.dispute(row, clientName, locationName)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapFailure(error)
        }
    }

    override suspend fun listDisputableJobs(): List<ServiceJob> {
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        DisputeRules.requireCanFile(Access.forMembership(membership.role))
        return try {
            val rows = client.from("service_jobs")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                        isIn("status", listOf(ServiceJobRules.Completed, ServiceJobRules.Disputed))
                    }
                }
                .decodeList<ServiceJobListRow>()
            rows.mapNotNull { serviceJobs.get(it.id) }
                .sortedByDescending { it.serviceDate.toString() }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(draft: DisputeDraft): Dispute {
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        val access = Access.forMembership(membership.role)
        DisputeRules.requireCanFile(access)
        val user = auth.currentUser() ?: throw DisputeFailure.Rejected("Sign in required.")
        val job = serviceJobs.get(draft.serviceJobId)
            ?: throw DisputeFailure.Rejected("Service job not found.")
        try {
            DisputeRules.requireDraft(job, draft)
            val evidenceMap = loadUploadedEvidenceIds(membership.organizationId, job.id)
            val exceptionMap = loadUploadedExceptionIds(membership.organizationId, job.id)
            val items = DisputeAssemblyRules.buildItems(
                DisputeAssemblyInput(
                    job = job,
                    uploadedEvidenceByRequirement = evidenceMap,
                    uploadedExceptionByRequirement = exceptionMap,
                ),
            )
            val disputeId = Uuid.generateV4().toString().lowercase()
            var attachmentPath: String? = null
            val attachmentMime = draft.attachmentMimeType
            val bytes = draft.attachmentBytes
            if (bytes != null && !attachmentMime.isNullOrBlank()) {
                attachmentPath = "${membership.organizationId}/$disputeId/complaint"
                var uploaded = false
                client.storage.from(DISPUTE_ATTACHMENTS_BUCKET).uploadAsFlow(attachmentPath, bytes) {
                    contentType = ContentType.parse(attachmentMime)
                    upsert = true
                }.collect { status ->
                    if (status is UploadStatus.Success) {
                        uploaded = true
                    }
                }
                if (!uploaded) {
                    throw DisputeFailure.Rejected("Attachment upload failed.")
                }
            }
            client.from("disputes").insert(
                DisputeInsertMapper.dispute(
                    id = disputeId,
                    organizationId = membership.organizationId,
                    job = job,
                    draft = draft,
                    recordedBy = user.id,
                    attachmentPath = attachmentPath,
                    attachmentMime = attachmentMime,
                ),
            )
            items.forEach { itemDraft ->
                val itemId = Uuid.generateV4().toString().lowercase()
                client.from("dispute_items").insert(
                    DisputeInsertMapper.item(
                        id = itemId,
                        organizationId = membership.organizationId,
                        disputeId = disputeId,
                        jobId = job.id,
                        draft = itemDraft,
                    ),
                )
            }
            if (access.canWriteDisputes && job.status == ServiceJobRules.Completed) {
                serviceJobs.markDisputed(job.id)
            }
            notifyDisputeReceived(disputeId, membership.organizationId)
            return get(disputeId) ?: throw DisputeFailure.Rejected("Dispute was not saved.")
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is DisputeRuleViolation) throw DisputeFailure.Rejected(error.message ?: "Invalid dispute.")
            throw mapFailure(error)
        }
    }

    override suspend fun getReconstruction(disputeId: String): DisputeReconstructionBundle? {
        val dispute = get(disputeId) ?: return null
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        val jobId = dispute.serviceJobId ?: return null
        val job = serviceJobs.get(jobId) ?: return null
        return try {
            val items = client.from("dispute_items")
                .select {
                    filter {
                        eq("dispute_id", disputeId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<DisputeItemRow>()
                .map { DisputeMapper.item(it) }
            val disputedText = job.requirements
                .firstOrNull { it.id == dispute.disputedServiceJobRequirementId }
                ?.requirementText
                ?: dispute.disputedServiceJobRequirementId
                ?: "Requirement not recorded."
            val contractRef = loadContractRef(job.contractId, job.contractVersionId)
            val evidence = loadEvidenceRecords(membership.organizationId, jobId)
            val exceptions = loadExceptionRecords(membership.organizationId, jobId)
            DisputeReconstructionRules.build(
                dispute = dispute,
                items = items,
                job = job,
                disputedRequirementText = disputedText,
                contract = contractRef,
                evidence = evidence,
                exceptions = exceptions,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapFailure(error)
        }
    }

    override suspend fun requestSummary(disputeId: String): DisputeAiSummary {
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        if (!Access.forMembership(membership.role).canOpenDisputes) {
            throw DisputeFailure.Rejected("Disputes are not available.")
        }
        try {
            client.auth.awaitInitialization()
            val token = client.auth.currentSessionOrNull()?.accessToken
                ?: throw DisputeFailure.Rejected("Sign in required.")
            val response = http.post("${config.url.trimEnd('/')}/functions/v1/summarize-dispute") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(SummarizeDisputeRequest(disputeId = disputeId))
            }
            val body = response.body<SummarizeDisputeResponse>()
            if (!body.ok || body.summary == null) {
                throw DisputeFailure.Rejected(body.error?.message ?: "Summary failed.")
            }
            return body.summary
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is DisputeFailure) throw error
            throw mapFailure(error)
        }
    }

    private suspend fun loadContractRef(contractId: String, versionId: String): DisputeContractRef {
        val membership = organizations.currentMembership() ?: throw DisputeFailure.Rejected("Sign in required.")
        val title = client.from("contracts")
            .select {
                filter {
                    eq("id", contractId)
                    eq("organization_id", membership.organizationId)
                }
            }
            .decodeList<ContractTitleRow>()
            .firstOrNull()
            ?.title
            ?: contractId
        val label = client.from("contract_versions")
            .select {
                filter {
                    eq("id", versionId)
                    eq("organization_id", membership.organizationId)
                }
            }
            .decodeList<ContractVersionLabelRow>()
            .firstOrNull()
            ?.let { "v${it.versionNumber}" }
            ?: versionId
        return DisputeContractRef(
            contractId = contractId,
            contractTitle = title,
            versionLabel = label,
        )
    }

    private suspend fun loadUploadedEvidenceIds(organizationId: String, jobId: String): Map<String, String> {
        return loadEvidenceRecords(organizationId, jobId)
            .filter { it.syncStatus == EvidenceSyncStatus.Uploaded }
            .associate { it.serviceJobRequirementId to it.id }
    }

    private suspend fun loadUploadedExceptionIds(organizationId: String, jobId: String): Map<String, String> {
        return loadExceptionRecords(organizationId, jobId)
            .filter { it.syncStatus == EvidenceSyncStatus.Uploaded }
            .associate { it.serviceJobRequirementId to it.id }
    }

    private suspend fun loadEvidenceRecords(organizationId: String, jobId: String): List<com.contractproof.domain.Evidence> {
        val records = client.from("evidence_records")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    eq("service_job_id", jobId)
                }
            }
            .decodeList<EvidenceRecordRow>()
        if (records.isEmpty()) return emptyList()
        val recordIds = records.map { it.id }
        val files = client.from("evidence_files")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    isIn("evidence_record_id", recordIds)
                }
            }
            .decodeList<EvidenceFileRow>()
        val filesByRecord = files.associateBy { it.evidenceRecordId }
        return records.map { DisputeMapper.evidence(it, filesByRecord[it.id]) }
    }

    private suspend fun loadExceptionRecords(organizationId: String, jobId: String): List<com.contractproof.domain.JobExceptionRecord> {
        return client.from("exceptions")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    eq("service_job_id", jobId)
                }
            }
            .decodeList<ExceptionRecordRow>()
            .map { DisputeMapper.exception(it) }
    }

    private suspend fun notifyDisputeReceived(disputeId: String, organizationId: String) {
        try {
            client.auth.awaitInitialization()
            val token = client.auth.currentSessionOrNull()?.accessToken ?: return
            http.post("${config.url.trimEnd('/')}/functions/v1/notify-dispute-received") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(NotifyDisputeReceivedRequest(disputeId = disputeId, organizationId = organizationId))
            }
        } catch (_: Throwable) {
            // Notification is best-effort; dispute save already succeeded.
        }
    }

    private fun mapFailure(error: Throwable): DisputeFailure {
        return if (error.isOfflineFailure()) {
            DisputeFailure.Network
        } else if (error is DisputeFailure) {
            error
        } else {
            DisputeFailure.Rejected(error.message ?: "Dispute request failed.")
        }
    }

    private companion object {
        const val DISPUTE_ATTACHMENTS_BUCKET = "dispute-attachments"
    }
}

@Serializable
private data class ServiceJobListRow(val id: String)

@Serializable
private data class NotifyDisputeReceivedRequest(
    @SerialName("dispute_id") val disputeId: String,
    @SerialName("organization_id") val organizationId: String,
)

@Serializable
private data class SummarizeDisputeRequest(
    @SerialName("dispute_id") val disputeId: String,
)

@Serializable
private data class SummarizeDisputeResponse(
    val ok: Boolean,
    val summary: DisputeAiSummary? = null,
    val error: SummarizeDisputeError? = null,
)

@Serializable
private data class SummarizeDisputeError(
    val message: String,
)
