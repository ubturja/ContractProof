package com.contractproof.data

import com.contractproof.domain.EvidenceReportRecord
import com.contractproof.domain.ReportStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ReportMapper {
    fun record(row: ReportRow): EvidenceReportRecord {
        return EvidenceReportRecord(
            id = row.id,
            organizationId = row.organizationId,
            disputeId = row.disputeId,
            status = ReportStatus.fromStorage(row.status),
            bucket = row.bucket,
            objectPath = row.objectPath,
            generatedBy = row.generatedBy,
            generatedAt = row.generatedAt,
            failureReason = row.failureReason,
        )
    }
}

@Serializable
data class ReportRow(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("dispute_id") val disputeId: String,
    val status: String,
    val bucket: String,
    @SerialName("object_path") val objectPath: String? = null,
    @SerialName("generated_by") val generatedBy: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
)
