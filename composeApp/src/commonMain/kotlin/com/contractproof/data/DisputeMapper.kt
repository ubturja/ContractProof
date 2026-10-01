package com.contractproof.data

import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeItem
import com.contractproof.domain.DisputeOutcome
import com.contractproof.domain.DisputeDraft
import com.contractproof.domain.DisputeItemDraft
import com.contractproof.domain.DisputeStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFile
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType
import com.contractproof.domain.JobExceptionRecord
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

object DisputeMapper {
    fun dispute(
        row: DisputeRow,
        clientName: String,
        locationName: String,
    ): Dispute {
        return Dispute(
            id = row.id,
            organizationId = row.organizationId,
            clientId = row.clientId,
            clientName = clientName,
            locationId = row.locationId,
            locationName = locationName,
            serviceDate = LocalDate.parse(row.serviceDate),
            complaint = row.complaint,
            serviceJobId = row.serviceJobId,
            disputedServiceJobRequirementId = row.disputedServiceJobRequirementId,
            complaintAttachmentObjectPath = row.complaintAttachmentObjectPath,
            complaintAttachmentMimeType = row.complaintAttachmentMimeType,
            recordedBy = row.recordedBy,
            status = DisputeStatus.fromStorage(row.status),
            syncStatus = EvidenceSyncStatus.fromStorage(row.syncStatus),
            createdAt = row.createdAt,
            aiSummaryJson = row.aiSummaryJson,
            aiSummaryGeneratedAt = row.aiSummaryGeneratedAt,
        )
    }

    fun item(row: DisputeItemRow): DisputeItem {
        return DisputeItem(
            id = row.id,
            disputeId = row.disputeId,
            serviceJobId = row.serviceJobId,
            serviceJobRequirementId = row.serviceJobRequirementId,
            evidenceRecordId = row.evidenceRecordId,
            exceptionId = row.exceptionId,
            outcome = DisputeOutcome.fromStorage(row.outcome),
            createdAt = row.createdAt,
        )
    }

    fun evidence(row: EvidenceRecordRow, file: EvidenceFileRow?): Evidence {
        val evidenceFile = file?.let {
            EvidenceFile(
                id = it.id,
                evidenceRecordId = it.evidenceRecordId,
                bucket = it.bucket,
                objectPath = it.objectPath,
                mimeType = it.mimeType,
                byteSize = it.byteSize,
                sha256 = it.sha256,
                syncStatus = EvidenceSyncStatus.fromStorage(it.syncStatus),
                uploadedAt = it.uploadedAt,
            )
        }
        return Evidence(
            id = row.id,
            organizationId = row.organizationId,
            serviceJobId = row.serviceJobId,
            serviceJobRequirementId = row.serviceJobRequirementId,
            capturedByUserId = row.capturedBy,
            capturedAt = row.capturedAt,
            type = EvidenceType.fromStorage(row.evidenceType),
            location = null,
            file = evidenceFile,
            syncStatus = EvidenceSyncStatus.fromStorage(row.syncStatus),
            receivedAt = row.receivedAt,
            isDemo = row.isDemo,
        )
    }

    fun exception(row: ExceptionRecordRow): JobExceptionRecord {
        return JobExceptionRecord(
            id = row.id,
            serviceJobRequirementId = row.serviceJobRequirementId,
            reason = row.reason,
            recordedAt = row.recordedAt,
            recordedBy = row.recordedBy,
            syncStatus = EvidenceSyncStatus.fromStorage(row.syncStatus),
        )
    }
}

@Serializable
data class DisputeRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("location_id") val locationId: String,
    @SerialName("service_date") val serviceDate: String,
    val complaint: String,
    @SerialName("service_job_id") val serviceJobId: String? = null,
    @SerialName("disputed_service_job_requirement_id") val disputedServiceJobRequirementId: String? = null,
    @SerialName("complaint_attachment_object_path") val complaintAttachmentObjectPath: String? = null,
    @SerialName("complaint_attachment_mime_type") val complaintAttachmentMimeType: String? = null,
    @SerialName("recorded_by") val recordedBy: String,
    val status: String,
    @SerialName("sync_status") val syncStatus: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("ai_summary_json") val aiSummaryJson: JsonObject? = null,
    @SerialName("ai_summary_generated_at") val aiSummaryGeneratedAt: String? = null,
)

@Serializable
data class DisputeItemRow(
    val id: String,
    @SerialName("dispute_id") val disputeId: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("service_job_requirement_id") val serviceJobRequirementId: String,
    @SerialName("evidence_record_id") val evidenceRecordId: String? = null,
    @SerialName("exception_id") val exceptionId: String? = null,
    val outcome: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class EvidenceRecordRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("service_job_requirement_id") val serviceJobRequirementId: String,
    @SerialName("captured_by") val capturedBy: String,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("evidence_type") val evidenceType: String,
    @SerialName("sync_status") val syncStatus: String,
    @SerialName("received_at") val receivedAt: String? = null,
    @SerialName("is_demo") val isDemo: Boolean = false,
)

@Serializable
data class EvidenceFileRow(
    val id: String,
    @SerialName("evidence_record_id") val evidenceRecordId: String,
    val bucket: String,
    @SerialName("object_path") val objectPath: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("byte_size") val byteSize: Long,
    val sha256: String,
    @SerialName("sync_status") val syncStatus: String,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
)

@Serializable
data class ExceptionRecordRow(
    val id: String,
    @SerialName("service_job_requirement_id") val serviceJobRequirementId: String,
    val reason: String,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("recorded_by") val recordedBy: String,
    @SerialName("sync_status") val syncStatus: String,
)

@Serializable
internal data class DisputeInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("location_id") val locationId: String,
    @SerialName("service_date") val serviceDate: String,
    val complaint: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("disputed_service_job_requirement_id") val disputedServiceJobRequirementId: String,
    @SerialName("complaint_attachment_object_path") val complaintAttachmentObjectPath: String? = null,
    @SerialName("complaint_attachment_mime_type") val complaintAttachmentMimeType: String? = null,
    @SerialName("recorded_by") val recordedBy: String,
    val status: String = DisputeStatus.STORAGE_OPEN,
    @SerialName("sync_status") val syncStatus: String,
)

@Serializable
internal data class DisputeItemInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("dispute_id") val disputeId: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("service_job_requirement_id") val serviceJobRequirementId: String,
    @SerialName("evidence_record_id") val evidenceRecordId: String? = null,
    @SerialName("exception_id") val exceptionId: String? = null,
    val outcome: String,
)

@Serializable
internal data class ContractTitleRow(
    val title: String,
)

@Serializable
internal data class ContractVersionLabelRow(
    @SerialName("version_number") val versionNumber: Int,
)

internal object DisputeInsertMapper {
    fun dispute(
        id: String,
        organizationId: String,
        job: ServiceJob,
        draft: DisputeDraft,
        recordedBy: String,
        attachmentPath: String?,
        attachmentMime: String?,
    ): DisputeInsert {
        return DisputeInsert(
            id = id,
            organizationId = organizationId,
            clientId = job.client.id,
            locationId = job.location.id,
            serviceDate = draft.serviceDate.toString(),
            complaint = draft.complaint.trim(),
            serviceJobId = job.id,
            disputedServiceJobRequirementId = draft.disputedServiceJobRequirementId,
            complaintAttachmentObjectPath = attachmentPath,
            complaintAttachmentMimeType = attachmentMime,
            recordedBy = recordedBy,
            syncStatus = EvidenceSyncStatus.STORAGE_UPLOADED,
        )
    }

    fun item(
        id: String,
        organizationId: String,
        disputeId: String,
        jobId: String,
        draft: DisputeItemDraft,
    ): DisputeItemInsert {
        return DisputeItemInsert(
            id = id,
            organizationId = organizationId,
            disputeId = disputeId,
            serviceJobId = jobId,
            serviceJobRequirementId = draft.serviceJobRequirementId,
            evidenceRecordId = draft.evidenceRecordId,
            exceptionId = draft.exceptionId,
            outcome = DisputeOutcome.toStorage(draft.outcome),
        )
    }
}
