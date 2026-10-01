package com.contractproof.data

import com.contractproof.domain.EvidenceReportRecord
import com.contractproof.domain.ReportGenerationRequest
import com.contractproof.domain.ReportGenerationResult
import com.contractproof.domain.ServiceEvidenceReport

sealed class ReportFailure : Exception() {
    data object Network : ReportFailure()

    data class Rejected(val userMessage: String) : ReportFailure()
}

interface ReportGateway {
    suspend fun getForDispute(disputeId: String): EvidenceReportRecord?

    suspend fun assemblePreview(disputeId: String): ServiceEvidenceReport?

    suspend fun requestGeneration(request: ReportGenerationRequest): ReportGenerationResult

    suspend fun signedPdfUrl(objectPath: String): String
}
