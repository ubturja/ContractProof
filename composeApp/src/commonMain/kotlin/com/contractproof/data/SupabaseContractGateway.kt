package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.ContractExtractionV1
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRuleViolation
import com.contractproof.domain.ContractRules
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.ServiceJobRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import io.ktor.http.ContentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseContractGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
    private val users: AuthGateway,
    private val serviceJobs: ServiceJobRepository,
) : ContractGateway {
    override suspend fun list(): List<ContractRecord> {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw ContractFailure.Rejected
        }
        return try {
            loadContracts(membership.organizationId)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapContractFailure(error)
        }
    }

    override suspend fun listVersions(contractId: String): List<ContractVersionRecord> {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw ContractFailure.Rejected
        }
        return try {
            loadVersions(membership.organizationId, contractId)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapContractFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(
        clientId: String,
        location: LocationRecord,
        title: String,
        startsOn: String,
        endsOn: String,
    ): ContractRecord {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            ContractRules.requireWrite(access)
            ContractRules.requireOrganization(membership.organizationId, membership.organizationId)
            ContractRules.requireLocationForClient(location, clientId, membership.organizationId)
            val start = ContractRules.requireIsoDate(startsOn, "Start date")
            val end = ContractRules.optionalEndDate(endsOn, start)
            val record = ContractRecord(
                id = Uuid.generateV4().toString().lowercase(),
                organizationId = membership.organizationId.lowercase(),
                clientId = clientId,
                locationId = location.id,
                title = ContractRules.requireTitle(title),
                status = ContractRules.Draft,
                startsOn = start,
                endsOn = end,
                currentVersionId = null,
                documentPath = null,
            )
            ContractRules.requireWritableStatus(record.status)
            client.from("contracts").insert(record.toInsert())
            return record
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ContractRuleViolation) throw ContractFailure.Rejected
            throw mapContractFailure(error)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun attachDocument(
        contractId: String,
        document: NewContractDocument,
        effectiveOn: String,
        onProgress: (Int) -> Unit,
    ): ContractRecord {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        val user = users.currentUser() ?: throw ContractFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            ContractRules.requireWrite(access)
            val fileName = ContractRules.requirePdfFileName(document.fileName)
            val bytes = ContractRules.requirePdfBytes(document.bytes)
            val existing = loadContracts(membership.organizationId).find { it.id == contractId }
                ?: throw ContractFailure.Rejected
            ContractRules.requireOrganization(existing.organizationId, membership.organizationId)
            val versions = loadVersions(membership.organizationId, existing.id)
            ContractVersionRules.requireCanStartVersion(versions)
            val start = ContractVersionRules.requireEffectiveOn(
                effectiveOn,
                ContractVersionRules.previousEffectiveOn(versions),
            )
            val versionId = Uuid.generateV4().toString().lowercase()
            val path = ContractRules.objectPath(
                organizationId = membership.organizationId,
                contractId = existing.id,
                versionId = versionId,
            )
            onProgress(0)
            var uploaded = false
            client.storage.from("contracts").uploadAsFlow(path, bytes) {
                contentType = ContentType.Application.Pdf
                upsert = true
                userMetadata = buildJsonObject {
                    put("original_file_name", fileName)
                    put("byte_size", bytes.size)
                    put("contract_id", existing.id)
                }
            }.collect { status ->
                when (status) {
                    is UploadStatus.Progress -> {
                        val total = status.contentLength
                        val percent = if (total <= 0L) {
                            0
                        } else {
                            ((status.totalBytesSend * 100) / total).toInt().coerceIn(0, 99)
                        }
                        onProgress(percent)
                    }
                    is UploadStatus.Success -> {
                        uploaded = true
                        onProgress(100)
                    }
                }
            }
            if (!uploaded) {
                throw ContractFailure.Upload
            }
            client.from("contract_versions").insert(
                VersionInsert(
                    id = versionId,
                    organizationId = membership.organizationId,
                    contractId = existing.id,
                    versionNumber = ContractVersionRules.nextNumber(versions),
                    status = ContractVersionRules.Uploaded,
                    bucket = "contracts",
                    objectPath = path,
                    originalFileName = fileName,
                    mimeType = ContractRules.PdfMimeType,
                    byteSize = bytes.size.toLong(),
                    createdBy = user.id,
                    effectiveOn = start,
                ),
            )
            return existing.copy(
                documentPath = path,
                documentFileName = fileName,
                documentByteSize = bytes.size.toLong(),
                documentMimeType = ContractRules.PdfMimeType,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ContractRuleViolation) throw ContractFailure.Rejected
            if (error is ContractFailure) throw error
            throw ContractFailure.Upload
        }
    }

    override suspend fun getVersion(contractId: String, versionId: String): ContractVersionDetail {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw ContractFailure.Rejected
        }
        return try {
            val row = client.from("contract_versions")
                .select {
                    filter {
                        eq("id", versionId)
                        eq("contract_id", contractId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<VersionDetailRow>()
                .firstOrNull()
                ?: throw ContractFailure.Rejected
            ContractVersionDetail(
                id = row.id,
                contractId = row.contractId,
                organizationId = membership.organizationId,
                status = row.status,
                documentFileName = row.originalFileName,
                documentPath = row.objectPath,
                extraction = row.extraction,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapContractFailure(error)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun approveVersion(contractId: String, versionId: String): ContractRecord {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        val user = users.currentUser() ?: throw ContractFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            val version = loadVersions(membership.organizationId, contractId).find { it.id == versionId }
                ?: throw ContractFailure.Rejected
            val requirementCount = client.from("contract_requirements")
                .select {
                    filter {
                        eq("contract_version_id", versionId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<RequirementCountRow>()
                .size
            ContractVersionRules.requireApprove(access, version, membership.organizationId, requirementCount)
            client.from("contract_versions").update(
                {
                    set("status", ContractVersionRules.Approved)
                    set("approved_at", Clock.System.now().toString())
                    set("approved_by", user.id)
                },
            ) {
                filter {
                    eq("id", versionId)
                    eq("contract_id", contractId)
                    eq("organization_id", membership.organizationId)
                }
            }
            val record = loadContracts(membership.organizationId).find { it.id == contractId }
                ?: throw ContractFailure.Rejected
            generateJobsForVersion(contractId, versionId)
            return record
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ContractRuleViolation) throw ContractFailure.Rejected
            throw mapContractFailure(error)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun activateVersion(contractId: String, versionId: String): ContractRecord {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        val user = users.currentUser() ?: throw ContractFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            val version = loadVersions(membership.organizationId, contractId).find { it.id == versionId }
                ?: throw ContractFailure.Rejected
            ContractVersionRules.requireActivate(access, version, membership.organizationId)
            client.from("contract_versions").update(
                {
                    set("status", ContractVersionRules.Approved)
                    set("approved_at", Clock.System.now().toString())
                    set("approved_by", user.id)
                },
            ) {
                filter {
                    eq("id", versionId)
                    eq("contract_id", contractId)
                    eq("organization_id", membership.organizationId)
                }
            }
            val record = loadContracts(membership.organizationId).find { it.id == contractId }
                ?: throw ContractFailure.Rejected
            generateJobsForVersion(contractId, versionId)
            return record
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ContractRuleViolation) throw ContractFailure.Rejected
            throw mapContractFailure(error)
        }
    }

    private suspend fun generateJobsForVersion(contractId: String, versionId: String) {
        try {
            serviceJobs.generateForApprovedVersion(contractId, versionId)
        } catch (_: ServiceJobFailure) {
            // Job generation is best-effort after approval; schedules may be added later.
        }
    }

    override suspend fun update(
        id: String,
        title: String,
        startsOn: String,
        endsOn: String,
        status: String,
    ): ContractRecord {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        val access = Access.forMembership(membership.role)
        try {
            ContractRules.requireWrite(access)
            ContractRules.requireWritableStatus(status)
            val start = ContractRules.requireIsoDate(startsOn, "Start date")
            val end = ContractRules.optionalEndDate(endsOn, start)
            val trimmed = ContractRules.requireTitle(title)
            val existing = loadContracts(membership.organizationId).find { it.id == id }
                ?: throw ContractFailure.Rejected
            ContractRules.requireOrganization(existing.organizationId, membership.organizationId)
            val record = existing.copy(
                title = trimmed,
                startsOn = start,
                endsOn = end,
                status = status,
            )
            client.from("contracts").update(
                {
                    set("title", record.title)
                    set("starts_on", record.startsOn)
                    set("ends_on", record.endsOn)
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
            if (error is ContractRuleViolation) throw ContractFailure.Rejected
            throw mapContractFailure(error)
        }
    }

    override suspend fun documentUrl(path: String): String {
        val membership = organizations.currentMembership() ?: throw ContractFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw ContractFailure.Rejected
        }
        return try {
            ContractRules.requirePathInOrganization(path, membership.organizationId)
            client.storage.from("contracts").createSignedUrl(path = path, expiresIn = 15.minutes)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ContractRuleViolation) throw ContractFailure.Rejected
            throw mapContractFailure(error)
        }
    }

    private suspend fun loadContracts(organizationId: String): List<ContractRecord> {
        val rows = client.from("contracts")
            .select {
                filter {
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<ContractRow>()
        val versions = client.from("contract_versions")
            .select {
                filter {
                    eq("organization_id", organizationId)
                }
            }
            .decodeList<VersionRow>()
        val latest = versions
            .filter { !it.objectPath.isNullOrEmpty() }
            .groupBy { it.contractId }
            .mapValues { entry ->
                entry.value.maxBy { it.versionNumber }
            }
        return rows.map { row ->
            val version = latest[row.id]
            row.toRecord(
                documentPath = version?.objectPath,
                documentFileName = version?.originalFileName,
                documentByteSize = version?.byteSize,
                documentMimeType = version?.mimeType,
            )
        }
    }

    private suspend fun loadVersions(
        organizationId: String,
        contractId: String,
    ): List<ContractVersionRecord> {
        return client.from("contract_versions")
            .select {
                filter {
                    eq("organization_id", organizationId)
                    eq("contract_id", contractId)
                }
            }
            .decodeList<VersionRow>()
            .map { it.toRecord(organizationId) }
            .sortedBy { it.versionNumber }
    }
}

private fun mapContractFailure(error: Throwable): ContractFailure {
    if (error is ContractFailure) {
        return error
    }
    return if (error.isOfflineFailure()) ContractFailure.Network else ContractFailure.Rejected
}

@Serializable
private data class ContractRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("location_id") val locationId: String,
    val title: String,
    val status: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String? = null,
    @SerialName("current_version_id") val currentVersionId: String? = null,
) {
    fun toRecord(
        documentPath: String?,
        documentFileName: String?,
        documentByteSize: Long?,
        documentMimeType: String?,
    ): ContractRecord {
        return ContractRecord(
            id = id,
            organizationId = organizationId,
            clientId = clientId,
            locationId = locationId,
            title = title,
            status = status,
            startsOn = startsOn,
            endsOn = endsOn,
            currentVersionId = currentVersionId,
            documentPath = documentPath,
            documentFileName = documentFileName,
            documentByteSize = documentByteSize,
            documentMimeType = documentMimeType,
        )
    }
}

private fun ContractRecord.toInsert(): ContractInsert {
    return ContractInsert(
        id = id,
        organizationId = organizationId,
        clientId = clientId,
        locationId = locationId,
        title = title,
        status = status,
        startsOn = startsOn,
        endsOn = endsOn,
    )
}

@Serializable
private data class ContractInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("location_id") val locationId: String,
    val title: String,
    val status: String,
    @SerialName("starts_on") val startsOn: String,
    @SerialName("ends_on") val endsOn: String?,
)

@Serializable
private data class VersionRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String? = null,
    @SerialName("contract_id") val contractId: String,
    @SerialName("version_number") val versionNumber: Int,
    val status: String = ContractVersionRules.Uploaded,
    @SerialName("effective_on") val effectiveOn: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("object_path") val objectPath: String? = null,
    @SerialName("original_file_name") val originalFileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("byte_size") val byteSize: Long? = null,
) {
    fun toRecord(organizationId: String): ContractVersionRecord {
        return ContractVersionRecord(
            id = id,
            organizationId = this.organizationId ?: organizationId,
            contractId = contractId,
            versionNumber = versionNumber,
            status = status,
            effectiveOn = effectiveOn.orEmpty(),
            createdBy = createdBy.orEmpty(),
            documentPath = objectPath,
            documentFileName = originalFileName,
            documentByteSize = byteSize,
            documentMimeType = mimeType,
        )
    }
}

@Serializable
private data class VersionInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("contract_id") val contractId: String,
    @SerialName("version_number") val versionNumber: Int,
    val status: String,
    val bucket: String,
    @SerialName("object_path") val objectPath: String,
    @SerialName("original_file_name") val originalFileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("byte_size") val byteSize: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("effective_on") val effectiveOn: String,
)

@Serializable
private data class VersionDetailRow(
    val id: String,
    @SerialName("contract_id") val contractId: String,
    val status: String,
    @SerialName("object_path") val objectPath: String? = null,
    @SerialName("original_file_name") val originalFileName: String? = null,
    val extraction: ContractExtractionV1? = null,
)

@Serializable
private data class RequirementCountRow(
    val id: String,
)
