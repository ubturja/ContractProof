package com.contractproof.data.local

import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceCoordinates
import com.contractproof.domain.EvidenceFile
import com.contractproof.domain.EvidenceLocation
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType

object EvidenceLocalMapper {
    fun toDomain(record: Evidence_record, file: Evidence_file?): Evidence {
        val coordinates = if (record.latitude != null && record.longitude != null) {
            EvidenceCoordinates(
                latitude = record.latitude,
                longitude = record.longitude,
                horizontalAccuracyMeters = record.horizontal_accuracy_meters,
            )
        } else {
            null
        }
        val location = if (record.location_id != null || coordinates != null) {
            EvidenceLocation(locationId = record.location_id, coordinates = coordinates)
        } else {
            null
        }
        return Evidence(
            id = record.id,
            organizationId = record.organization_id,
            serviceJobId = record.service_job_id,
            serviceJobRequirementId = record.service_job_requirement_id,
            capturedByUserId = record.captured_by_user_id,
            capturedAt = record.captured_at,
            type = EvidenceType.fromStorage(record.evidence_type),
            location = location,
            file = file?.let { toDomainFile(it) },
            syncStatus = EvidenceSyncStatus.fromStorage(record.sync_status),
            receivedAt = record.received_at,
            isDemo = record.is_demo != 0L,
        )
    }

    fun toDomainFile(file: Evidence_file): EvidenceFile {
        return EvidenceFile(
            id = file.id,
            evidenceRecordId = file.evidence_record_id,
            bucket = file.bucket,
            objectPath = file.object_path,
            mimeType = file.mime_type,
            byteSize = file.byte_size,
            sha256 = file.sha256,
            syncStatus = EvidenceSyncStatus.fromStorage(file.sync_status),
            uploadedAt = file.uploaded_at,
            localStagingPath = file.local_staging_path,
            uploadPercent = file.upload_percent?.toInt(),
        )
    }

    fun recordValues(evidence: Evidence): EvidenceRecordValues {
        val coords = evidence.location?.coordinates
        return EvidenceRecordValues(
            id = evidence.id,
            organizationId = evidence.organizationId,
            serviceJobId = evidence.serviceJobId,
            serviceJobRequirementId = evidence.serviceJobRequirementId,
            capturedByUserId = evidence.capturedByUserId,
            capturedAt = evidence.capturedAt,
            evidenceType = EvidenceType.toStorage(evidence.type),
            locationId = evidence.location?.locationId,
            latitude = coords?.latitude,
            longitude = coords?.longitude,
            horizontalAccuracyMeters = coords?.horizontalAccuracyMeters,
            syncStatus = EvidenceSyncStatus.toStorage(evidence.syncStatus),
            receivedAt = evidence.receivedAt,
            isDemo = if (evidence.isDemo) 1L else 0L,
        )
    }

    fun fileValues(organizationId: String, file: EvidenceFile): EvidenceFileValues {
        return EvidenceFileValues(
            id = file.id,
            organizationId = organizationId,
            evidenceRecordId = file.evidenceRecordId,
            bucket = file.bucket,
            objectPath = file.objectPath,
            mimeType = file.mimeType,
            byteSize = file.byteSize,
            sha256 = file.sha256,
            syncStatus = EvidenceSyncStatus.toStorage(file.syncStatus),
            uploadedAt = file.uploadedAt,
            localStagingPath = file.localStagingPath,
            uploadPercent = file.uploadPercent?.toLong(),
        )
    }
}

data class EvidenceRecordValues(
    val id: String,
    val organizationId: String,
    val serviceJobId: String,
    val serviceJobRequirementId: String,
    val capturedByUserId: String,
    val capturedAt: String,
    val evidenceType: String,
    val locationId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val horizontalAccuracyMeters: Double?,
    val syncStatus: String,
    val receivedAt: String?,
    val isDemo: Long,
)

data class EvidenceFileValues(
    val id: String,
    val organizationId: String,
    val evidenceRecordId: String,
    val bucket: String,
    val objectPath: String,
    val mimeType: String,
    val byteSize: Long,
    val sha256: String,
    val syncStatus: String,
    val uploadedAt: String?,
    val localStagingPath: String?,
    val uploadPercent: Long?,
)
