package com.contractproof.data

import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.core.platform.compressEvidencePhoto
import com.contractproof.core.platform.readLocalFileBytes
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFailure
import com.contractproof.domain.EvidenceLocation
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.EvidenceRules
import com.contractproof.domain.EvidenceSubmitResult
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType
import com.contractproof.domain.EvidenceUploadRules
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.data.local.SqlDelightEvidenceStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import io.ktor.http.ContentType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SupabaseEvidenceRepository(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
    private val auth: AuthGateway,
    private val serviceJobs: ServiceJobRepository,
    private val local: SqlDelightEvidenceStore,
) : EvidenceRepository {
    private val photoStagingBytes = mutableMapOf<String, ByteArray>()

    private fun stagingBytesKey(organizationId: String, requirementId: String): String {
        return "$organizationId:$requirementId"
    }
    override suspend fun getByRequirement(organizationId: String, serviceJobRequirementId: String): Evidence? {
        return local.getByRequirement(organizationId, serviceJobRequirementId)
    }

    override suspend fun listForJob(organizationId: String, serviceJobId: String): List<Evidence> {
        return local.listForJob(organizationId, serviceJobId)
    }

    override suspend fun upsert(evidence: Evidence) {
        local.upsert(evidence)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun preparePhotoEvidence(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation?,
    ): Evidence {
        val membership = organizations.currentMembership() ?: throw EvidenceFailure.Rejected
        val user = auth.currentUser() ?: throw EvidenceFailure.Rejected
        val job = serviceJobs.get(jobId) ?: throw EvidenceFailure.Rejected
        val existing = local.getByRequirement(membership.organizationId, requirementId)
        EvidenceUploadRules.requireCanCapturePhoto(existing)
        val compressed = compressEvidencePhoto(photo.localPath)
        val recordId = Uuid.generateV4().toString().lowercase()
        val fileId = Uuid.generateV4().toString().lowercase()
        val objectPath = "${membership.organizationId}/$jobId/$requirementId/$recordId"
        val identity = EvidenceUploadRules.identityForUpload(existing, recordId, fileId, objectPath)
        val draft = EvidenceMapper.draftPhoto(
            recordId = identity.recordId,
            fileId = identity.fileId,
            job = job,
            requirementId = requirementId,
            userId = user.id,
            capturedAt = capturedAt,
            objectPath = identity.objectPath,
            mimeType = compressed.mimeType,
            bytes = compressed.bytes,
            location = location,
            localStagingPath = compressed.localPath,
        )
        EvidenceRules.requireReadyToPersist(draft, job)
        photoStagingBytes[stagingBytesKey(membership.organizationId, requirementId)] = compressed.bytes
        local.upsert(draft)
        val isNewRemote = existing == null || !EvidenceUploadRules.canReuseExisting(existing)
        try {
            ensureRemoteRows(draft, membership.organizationId, isNewRemote)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw if (error.isOfflineFailure()) EvidenceFailure.Network else EvidenceFailure.Rejected
        }
        return draft
    }

    override suspend fun uploadPhotoEvidence(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        val membership = organizations.currentMembership() ?: throw EvidenceFailure.Rejected
        val evidence = local.getByRequirement(membership.organizationId, requirementId)
            ?: throw EvidenceFailure.Rejected
        val file = evidence.file ?: throw EvidenceFailure.Rejected
        val stagingKey = stagingBytesKey(membership.organizationId, requirementId)
        val bytes = photoStagingBytes.remove(stagingKey)
            ?: readLocalFileBytes(file.localStagingPath ?: throw EvidenceFailure.Rejected)
        return uploadPreparedEvidence(
            evidence = evidence,
            bytes = bytes,
            mimeType = file.mimeType,
            requirementId = requirementId,
            organizationId = membership.organizationId,
            onProgress = onProgress,
        )
    }

    override suspend fun retryPhotoUpload(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        val membership = organizations.currentMembership() ?: throw EvidenceFailure.Rejected
        val evidence = local.getByRequirement(membership.organizationId, requirementId)
            ?: throw EvidenceFailure.Rejected
        val retrying = evidence.copy(syncStatus = EvidenceSyncStatus.Retrying)
        persistSyncStatus(retrying, EvidenceSyncStatus.Retrying)
        return uploadPhotoEvidence(jobId, requirementId, onProgress)
    }

    override suspend fun submitPhoto(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation?,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        preparePhotoEvidence(jobId, requirementId, photo, capturedAt, location)
        return uploadPhotoEvidence(jobId, requirementId, onProgress)
    }

    override suspend fun submitChecklistCompletion(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult {
        return submitBytes(
            jobId = jobId,
            requirementId = requirementId,
            bytes = EvidenceAssets.acknowledgmentPng,
            mimeType = "image/png",
            capturedAt = capturedAt,
            type = EvidenceType.ChecklistCompletion,
            location = location,
        )
    }

    override suspend fun submitTimestamp(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult {
        return submitBytes(
            jobId = jobId,
            requirementId = requirementId,
            bytes = EvidenceAssets.acknowledgmentPng,
            mimeType = "image/png",
            capturedAt = capturedAt,
            type = EvidenceType.Timestamp,
            location = location,
        )
    }

    private suspend fun uploadPreparedEvidence(
        evidence: Evidence,
        bytes: ByteArray,
        mimeType: String,
        requirementId: String,
        organizationId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        val file = evidence.file ?: throw EvidenceFailure.Rejected
        try {
            persistSyncStatus(evidence, EvidenceSyncStatus.Uploading)
            onProgress(0)
            var uploaded = false
            client.storage.from(EvidenceMapper.EVIDENCE_BUCKET).uploadAsFlow(file.objectPath, bytes) {
                contentType = ContentType.parse(mimeType)
                upsert = true
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
                markFailed(evidence)
                return EvidenceSubmitResult(uploaded = false, pending = true)
            }
            val synced = evidence.copy(
                syncStatus = EvidenceSyncStatus.Uploaded,
                file = file.copy(
                    syncStatus = EvidenceSyncStatus.Uploaded,
                    uploadedAt = evidence.capturedAt,
                ),
            )
            local.upsert(synced)
            markUploaded(evidence.id, file.id, requirementId, organizationId)
            return EvidenceSubmitResult(uploaded = true, pending = false)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            markFailed(evidence)
            if (error is EvidenceFailure) throw error
            throw if (error.isOfflineFailure()) EvidenceFailure.Network else EvidenceFailure.Rejected
        }
    }

    private suspend fun markFailed(evidence: Evidence) {
        val file = evidence.file
        val failed = evidence.copy(
            syncStatus = EvidenceSyncStatus.Failed,
            file = file?.copy(syncStatus = EvidenceSyncStatus.Failed),
        )
        persistSyncStatus(failed, EvidenceSyncStatus.Failed)
    }

    private suspend fun persistSyncStatus(evidence: Evidence, status: EvidenceSyncStatus) {
        val updated = evidence.copy(
            syncStatus = status,
            file = evidence.file?.copy(syncStatus = status),
        )
        local.upsert(updated)
        try {
            updateRemoteSyncStatus(updated, status)
        } catch (_: Throwable) {
            // Local state is still authoritative for UI retry.
        }
    }

    private suspend fun ensureRemoteRows(evidence: Evidence, organizationId: String, isNew: Boolean) {
        if (isNew) {
            client.from("evidence_records").insert(
                EvidenceMapper.recordInsert(evidence, EvidenceSyncStatus.STORAGE_PENDING),
            )
            val file = evidence.file ?: throw EvidenceFailure.Rejected
            client.from("evidence_files").insert(
                EvidenceMapper.fileInsert(file, organizationId, EvidenceSyncStatus.STORAGE_PENDING),
            )
        } else {
            updateRemoteSyncStatus(evidence, EvidenceSyncStatus.Pending)
        }
    }

    private suspend fun updateRemoteSyncStatus(evidence: Evidence, status: EvidenceSyncStatus) {
        val storage = EvidenceSyncStatus.toStorage(status)
        val file = evidence.file
        if (file != null) {
            client.from("evidence_files").update(
                {
                    set("sync_status", storage)
                },
            ) {
                filter {
                    eq("id", file.id)
                    eq("organization_id", evidence.organizationId)
                }
            }
        }
        client.from("evidence_records").update(
            {
                set("sync_status", storage)
            },
        ) {
            filter {
                eq("id", evidence.id)
                eq("organization_id", evidence.organizationId)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun submitBytes(
        jobId: String,
        requirementId: String,
        bytes: ByteArray,
        mimeType: String,
        capturedAt: String,
        type: EvidenceType,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult {
        val membership = organizations.currentMembership() ?: throw EvidenceFailure.Rejected
        val user = auth.currentUser() ?: throw EvidenceFailure.Rejected
        val job = serviceJobs.get(jobId) ?: throw EvidenceFailure.Rejected
        val recordId = Uuid.generateV4().toString().lowercase()
        val fileId = Uuid.generateV4().toString().lowercase()
        val objectPath = "${membership.organizationId}/$jobId/$requirementId/$recordId"
        val draft = when (type) {
            EvidenceType.Photo -> error("Use preparePhotoEvidence for photos.")
            EvidenceType.ChecklistCompletion,
            EvidenceType.Timestamp,
            -> EvidenceMapper.draftNonPhoto(
                recordId = recordId,
                fileId = fileId,
                job = job,
                requirementId = requirementId,
                userId = user.id,
                capturedAt = capturedAt,
                type = type,
                objectPath = objectPath,
                bytes = bytes,
                mimeType = mimeType,
                location = location,
            )
        }
        EvidenceRules.requireReadyToPersist(draft, job)
        try {
            local.upsert(draft)
            client.from("evidence_records").insert(
                EvidenceMapper.recordInsert(draft, EvidenceSyncStatus.STORAGE_PENDING),
            )
            val file = draft.file ?: throw EvidenceFailure.Rejected
            client.from("evidence_files").insert(
                EvidenceMapper.fileInsert(file, membership.organizationId, EvidenceSyncStatus.STORAGE_PENDING),
            )
            return uploadPreparedEvidence(
                evidence = draft,
                bytes = bytes,
                mimeType = mimeType,
                requirementId = requirementId,
                organizationId = membership.organizationId,
                onProgress = {},
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is EvidenceFailure) throw error
            throw if (error.isOfflineFailure()) EvidenceFailure.Network else EvidenceFailure.Rejected
        }
    }

    private suspend fun markUploaded(
        recordId: String,
        fileId: String,
        requirementId: String,
        organizationId: String,
    ) {
        client.from("evidence_files").update(
            {
                set("sync_status", EvidenceSyncStatus.STORAGE_UPLOADED)
            },
        ) {
            filter {
                eq("id", fileId)
                eq("organization_id", organizationId)
            }
        }
        client.from("evidence_records").update(
            {
                set("sync_status", EvidenceSyncStatus.STORAGE_UPLOADED)
            },
        ) {
            filter {
                eq("id", recordId)
                eq("organization_id", organizationId)
            }
        }
        client.from("service_job_requirements").update(
            {
                set("status", JobRequirementStatus.STORAGE_SATISFIED)
            },
        ) {
            filter {
                eq("id", requirementId)
                eq("organization_id", organizationId)
            }
        }
    }
}
