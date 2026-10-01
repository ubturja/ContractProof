package com.contractproof.data.local

import com.contractproof.domain.Evidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightEvidenceStore(
    private val database: ContractProofDatabase,
) {
    private val queries = database.evidenceQueries

    suspend fun getByRequirement(organizationId: String, serviceJobRequirementId: String): Evidence? {
        return withContext(Dispatchers.Default) {
            val record = queries.selectByRequirement(organizationId, serviceJobRequirementId).executeAsOneOrNull()
                ?: return@withContext null
            val file = queries.selectFileByRecordId(record.id).executeAsOneOrNull()
            EvidenceLocalMapper.toDomain(record, file)
        }
    }

    suspend fun listForJob(organizationId: String, serviceJobId: String): List<Evidence> {
        return withContext(Dispatchers.Default) {
            queries.selectForJob(organizationId, serviceJobId).executeAsList().map { record ->
                val file = queries.selectFileByRecordId(record.id).executeAsOneOrNull()
                EvidenceLocalMapper.toDomain(record, file)
            }
        }
    }

    suspend fun upsert(evidence: Evidence) {
        withContext(Dispatchers.Default) {
            val record = EvidenceLocalMapper.recordValues(evidence)
            queries.upsertRecord(
                id = record.id,
                organization_id = record.organizationId,
                service_job_id = record.serviceJobId,
                service_job_requirement_id = record.serviceJobRequirementId,
                captured_by_user_id = record.capturedByUserId,
                captured_at = record.capturedAt,
                evidence_type = record.evidenceType,
                location_id = record.locationId,
                latitude = record.latitude,
                longitude = record.longitude,
                horizontal_accuracy_meters = record.horizontalAccuracyMeters,
                sync_status = record.syncStatus,
                received_at = record.receivedAt,
                is_demo = record.isDemo,
            )
            val file = evidence.file
            if (file == null) {
                queries.deleteFileForRecord(evidence.id)
            } else {
                val values = EvidenceLocalMapper.fileValues(evidence.organizationId, file)
                queries.upsertFile(
                    id = values.id,
                    organization_id = values.organizationId,
                    evidence_record_id = values.evidenceRecordId,
                    bucket = values.bucket,
                    object_path = values.objectPath,
                    mime_type = values.mimeType,
                    byte_size = values.byteSize,
                    sha256 = values.sha256,
                    sync_status = values.syncStatus,
                    uploaded_at = values.uploadedAt,
                    local_staging_path = values.localStagingPath,
                    upload_percent = values.uploadPercent,
                )
            }
        }
    }

    suspend fun updateUploadPercent(evidenceRecordId: String, percent: Int?) {
        withContext(Dispatchers.Default) {
            queries.updateFileUploadPercent(
                upload_percent = percent?.toLong(),
                evidence_record_id = evidenceRecordId,
            )
        }
    }
}
