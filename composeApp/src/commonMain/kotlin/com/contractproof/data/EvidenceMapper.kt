package com.contractproof.data

import com.contractproof.core.crypto.sha256Hex
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFile
import com.contractproof.domain.EvidenceLocation
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType
import com.contractproof.domain.ServiceJob
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object EvidenceMapper {
    fun recordInsert(
        evidence: Evidence,
        syncStatus: String = EvidenceSyncStatus.STORAGE_PENDING,
    ): EvidenceRecordInsert {
        val coords = evidence.location?.coordinates
        return EvidenceRecordInsert(
            id = evidence.id,
            organizationId = evidence.organizationId,
            serviceJobId = evidence.serviceJobId,
            serviceJobRequirementId = evidence.serviceJobRequirementId,
            capturedBy = evidence.capturedByUserId,
            capturedAt = evidence.capturedAt,
            evidenceType = EvidenceType.toStorage(evidence.type),
            locationId = evidence.location?.locationId,
            latitude = coords?.latitude,
            longitude = coords?.longitude,
            horizontalAccuracyMeters = coords?.horizontalAccuracyMeters,
            syncStatus = syncStatus,
        )
    }

    fun fileInsert(
        file: EvidenceFile,
        organizationId: String,
        syncStatus: String = EvidenceSyncStatus.STORAGE_PENDING,
    ): EvidenceFileInsert {
        return EvidenceFileInsert(
            id = file.id,
            organizationId = organizationId,
            evidenceRecordId = file.evidenceRecordId,
            objectPath = file.objectPath,
            mimeType = file.mimeType,
            byteSize = file.byteSize,
            sha256 = file.sha256,
            syncStatus = syncStatus,
        )
    }

    fun draftPhoto(
        recordId: String,
        fileId: String,
        job: ServiceJob,
        requirementId: String,
        userId: String,
        capturedAt: String,
        objectPath: String,
        mimeType: String,
        bytes: ByteArray,
        location: EvidenceLocation?,
        localStagingPath: String? = null,
    ): Evidence {
        val file = EvidenceFile(
            id = fileId,
            evidenceRecordId = recordId,
            bucket = EVIDENCE_BUCKET,
            objectPath = objectPath,
            mimeType = mimeType,
            byteSize = bytes.size.toLong(),
            sha256 = sha256Hex(bytes),
            syncStatus = EvidenceSyncStatus.Pending,
            uploadedAt = null,
            localStagingPath = localStagingPath,
        )
        return Evidence(
            id = recordId,
            organizationId = job.organizationId,
            serviceJobId = job.id,
            serviceJobRequirementId = requirementId,
            capturedByUserId = userId,
            capturedAt = capturedAt,
            type = EvidenceType.Photo,
            location = location ?: EvidenceLocation(locationId = job.location.id, coordinates = null),
            file = file,
            syncStatus = EvidenceSyncStatus.Pending,
            isDemo = false,
        )
    }

    fun draftNonPhoto(
        recordId: String,
        fileId: String,
        job: ServiceJob,
        requirementId: String,
        userId: String,
        capturedAt: String,
        type: EvidenceType,
        objectPath: String,
        bytes: ByteArray,
        mimeType: String,
        location: EvidenceLocation?,
    ): Evidence {
        val file = EvidenceFile(
            id = fileId,
            evidenceRecordId = recordId,
            bucket = EVIDENCE_BUCKET,
            objectPath = objectPath,
            mimeType = mimeType,
            byteSize = bytes.size.toLong(),
            sha256 = sha256Hex(bytes),
            syncStatus = EvidenceSyncStatus.Pending,
            uploadedAt = null,
        )
        return Evidence(
            id = recordId,
            organizationId = job.organizationId,
            serviceJobId = job.id,
            serviceJobRequirementId = requirementId,
            capturedByUserId = userId,
            capturedAt = capturedAt,
            type = type,
            location = location ?: EvidenceLocation(locationId = job.location.id, coordinates = null),
            file = file,
            syncStatus = EvidenceSyncStatus.Pending,
            isDemo = false,
        )
    }

    const val EVIDENCE_BUCKET = "evidence"
}

@Serializable
data class EvidenceRecordInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("service_job_requirement_id") val serviceJobRequirementId: String,
    @SerialName("captured_by") val capturedBy: String,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("evidence_type") val evidenceType: String,
    @SerialName("location_id") val locationId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("horizontal_accuracy_meters") val horizontalAccuracyMeters: Double? = null,
    @SerialName("sync_status") val syncStatus: String,
)

@Serializable
data class EvidenceFileInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("evidence_record_id") val evidenceRecordId: String,
    @SerialName("object_path") val objectPath: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("byte_size") val byteSize: Long,
    val sha256: String,
    @SerialName("sync_status") val syncStatus: String,
)
