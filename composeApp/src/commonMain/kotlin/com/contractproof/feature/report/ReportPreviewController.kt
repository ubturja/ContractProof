package com.contractproof.feature.report

import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.core.observability.ErrorLevel
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ReportFailure
import com.contractproof.data.ReportGateway
import com.contractproof.domain.Access
import com.contractproof.domain.Entitlements
import com.contractproof.domain.SubscriptionService
import com.contractproof.domain.EvidenceReportRecord
import com.contractproof.domain.ReportGenerationRequest
import com.contractproof.domain.ReportStatus
import com.contractproof.domain.ServiceEvidenceReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReportPreviewUiState(
    val loading: Boolean = false,
    val preview: ServiceEvidenceReport? = null,
    val report: EvidenceReportRecord? = null,
    val generationStatus: CpWorkStatus = CpWorkStatus.Missing,
    val signedUrl: String? = null,
    val banner: String? = null,
    val canGenerate: Boolean = false,
    val needsUpgrade: Boolean = false,
) {
    val canOpen: Boolean
        get() = signedUrl != null && report?.status == ReportStatus.Ready

    val canRetry: Boolean
        get() = report?.status == ReportStatus.Failed && canGenerate

    val isGenerating: Boolean
        get() = generationStatus == CpWorkStatus.Uploading || report?.status == ReportStatus.Generating
}

class ReportPreviewController(
    private val organizations: OrganizationGateway,
    private val reports: ReportGateway,
    private val subscription: SubscriptionService,
    private val analytics: ProductAnalytics,
    private val errorReporter: ErrorReporter,
) {
    private val ui = MutableStateFlow(ReportPreviewUiState())
    private val reportedDisputes = mutableSetOf<String>()
    val state: StateFlow<ReportPreviewUiState> = ui.asStateFlow()

    suspend fun load(disputeId: String) {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        val canGenerate = Entitlements.canGenerateReport(access, subscription.state.value)
        ui.update {
            it.copy(
                loading = true,
                banner = null,
                canGenerate = canGenerate,
                needsUpgrade = !canGenerate && access.canOpenDisputes,
            )
        }
        try {
            val preview = reports.assemblePreview(disputeId)
            val report = reports.getForDispute(disputeId)
            val signedUrl = report?.objectPath?.let { path ->
                if (report.status == ReportStatus.Ready) reports.signedPdfUrl(path) else null
            }
            ui.update {
                it.copy(
                    loading = false,
                    preview = preview,
                    report = report,
                    signedUrl = signedUrl,
                    generationStatus = workStatusFor(report),
                )
            }
        } catch (error: ReportFailure) {
            ui.update {
                it.copy(
                    loading = false,
                    banner = failureMessage(error),
                )
            }
        }
    }

    fun clearUpgradeSignal() {
        ui.update { it.copy(needsUpgrade = false) }
    }

    suspend fun generate(disputeId: String, force: Boolean = false) {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        val canGenerate = Entitlements.canGenerateReport(access, subscription.state.value)
        if (!canGenerate) {
            ui.update {
                it.copy(
                    banner = "Dispute evidence reports require Pro. Open plans to upgrade.",
                    needsUpgrade = access.canOpenDisputes,
                    canGenerate = false,
                )
            }
            return
        }
        ui.update { it.copy(generationStatus = CpWorkStatus.Uploading, banner = null) }
        try {
            val result = reports.requestGeneration(
                ReportGenerationRequest(disputeId = disputeId, forceRegenerate = force),
            )
            ui.update {
                it.copy(
                    report = result.report,
                    signedUrl = result.signedUrl,
                    generationStatus = workStatusFor(result.report),
                )
            }
            trackReportGeneratedOnce(disputeId, result.report)
            if (result.report.status == ReportStatus.Generating) {
                pollUntilSettled(disputeId)
            }
        } catch (error: ReportFailure) {
            if (error is ReportFailure.Rejected) {
                errorReporter.captureMessage(
                    message = "report_generation_rejected",
                    level = ErrorLevel.Error,
                    tags = mapOf("feature" to "reports", "dispute_id" to disputeId),
                )
            }
            ui.update {
                it.copy(
                    generationStatus = CpWorkStatus.Failed,
                    banner = failureMessage(error),
                )
            }
            refreshReport(disputeId)
        }
    }

    private suspend fun pollUntilSettled(disputeId: String) {
        repeat(15) {
            delay(2000)
            val report = reports.getForDispute(disputeId)
            if (report == null || report.status != ReportStatus.Generating) {
                val signedUrl = report?.objectPath?.let { reports.signedPdfUrl(it) }
                ui.update {
                    it.copy(
                        report = report,
                        signedUrl = signedUrl,
                        generationStatus = workStatusFor(report),
                    )
                }
                trackReportGeneratedOnce(disputeId, report)
                return
            }
        }
        ui.update {
            it.copy(
                generationStatus = CpWorkStatus.Failed,
                banner = "Report generation is taking longer than expected. Try again.",
            )
        }
    }

    private fun trackReportGeneratedOnce(disputeId: String, report: EvidenceReportRecord?) {
        if (report?.status == ReportStatus.Ready && reportedDisputes.add(disputeId)) {
            analytics.track(ProductEvent.ReportGenerated(disputeId))
        }
    }

    private suspend fun refreshReport(disputeId: String) {
        val report = reports.getForDispute(disputeId)
        ui.update {
            it.copy(
                report = report,
                generationStatus = workStatusFor(report),
            )
        }
    }

    private fun workStatusFor(report: EvidenceReportRecord?): CpWorkStatus {
        return when (report?.status) {
            ReportStatus.Generating -> CpWorkStatus.Uploading
            ReportStatus.Ready -> CpWorkStatus.Uploaded
            ReportStatus.Failed -> CpWorkStatus.Failed
            null -> CpWorkStatus.Missing
        }
    }

    private fun failureMessage(error: ReportFailure): String {
        return when (error) {
            ReportFailure.Network -> "Could not reach the server. Check your connection."
            is ReportFailure.Rejected -> error.userMessage
        }
    }
}
