package com.contractproof.app

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.contractproof.core.design.ContractProofTheme
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.DashboardDisputeRow
import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeAiSummary
import com.contractproof.domain.DisputeContractRef
import com.contractproof.domain.DisputeItem
import com.contractproof.domain.DisputeOutcome
import com.contractproof.domain.DisputeReconstructionRules
import com.contractproof.domain.DisputeStatus
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceFile
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.EvidenceType
import com.contractproof.domain.JobExceptionRecord
import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.PlanOffering
import com.contractproof.domain.ReportEntityRefs
import com.contractproof.domain.ReportScheduledService
import com.contractproof.domain.ReportStatus
import com.contractproof.domain.ReviewRequirementDraft
import com.contractproof.domain.ServiceEvidenceReport
import com.contractproof.domain.ServiceEvidenceReportHeader
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRequirement
import com.contractproof.domain.ServiceJobRules
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionSnapshot
import com.contractproof.domain.SubscriptionSource
import com.contractproof.domain.SubscriptionStatus
import com.contractproof.domain.EvidenceReportRecord
import com.contractproof.feature.dashboard.DashboardScreen
import com.contractproof.feature.dashboard.DashboardUiState
import com.contractproof.feature.dispute.DisputeDetailScreen
import com.contractproof.feature.dispute.DisputeDetailUiState
import com.contractproof.feature.extraction.ExtractionReviewPhase
import com.contractproof.feature.extraction.ExtractionReviewScreen
import com.contractproof.feature.extraction.ExtractionReviewUiState
import com.contractproof.feature.report.ReportPreviewScreen
import com.contractproof.feature.report.ReportPreviewUiState
import com.contractproof.feature.service.CoveragePrimaryAction
import com.contractproof.feature.service.CoverageScreen
import com.contractproof.feature.service.CoverageUiState
import com.contractproof.feature.service.TodayJobCardUi
import com.contractproof.feature.service.TodayScreen
import com.contractproof.feature.service.TodayUiState
import com.contractproof.feature.subscription.PaywallPhase
import com.contractproof.feature.subscription.PaywallScreen
import com.contractproof.feature.subscription.PaywallUiState
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MarketingScreenshotsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exportHackathonScreenshots() {
        val outDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "marketing-screenshots",
        )
        outDir.mkdirs()

        export(outDir, "01-dashboard") {
            DashboardScreen(
                state = DashboardUiState(
                    organizationName = "ClearLine Facility Services",
                    attentionLine = "Today's facility overview",
                    todayJobs = listOf(
                        TodayJobCardUi(
                            jobId = "job-meridian",
                            locationName = "Meridian Lobby",
                            clientName = "Meridian Office Tower",
                            serviceTimeLabel = "8:00 AM",
                            statusLabel = "In progress",
                            coveragePercent = 62,
                        ),
                    ),
                    openDisputes = listOf(
                        DashboardDisputeRow(
                            disputeId = "dispute-northstar",
                            clientName = "Northstar Logistics",
                            locationName = "Dock Office",
                            serviceDate = "2026-09-28",
                        ),
                    ),
                    completionLabel = "1 of 2 services complete today",
                    coverageLine = "Average coverage 78% across active jobs",
                ),
                hasLocations = true,
                canAddLocation = true,
                canOpenLocations = true,
                canOpenContracts = true,
                canOpenDisputes = true,
                onOpenJob = {},
                onOpenDispute = {},
                onOpenLocations = {},
                onOpenContracts = {},
                onOpenDisputes = {},
                onOpenSettings = {},
                onRetry = {},
                onRefresh = {},
            )
        }

        export(outDir, "02-contract-extraction") {
            ExtractionReviewScreen(
                state = ExtractionReviewUiState(
                    documentFileName = "Meridian nightly clean",
                    versionStatus = ContractVersionRules.Extracted,
                    phase = ExtractionReviewPhase.Review,
                    drafts = listOf(
                        ReviewRequirementDraft(
                            localId = "d1",
                            extractionKey = "k1",
                            task = "Sanitize exam rooms",
                            requiresPhoto = true,
                            isMandatory = true,
                            confidence = 0.92,
                            evidenceQuote = "Weekly sanitization required",
                            fromExtraction = true,
                        ),
                        ReviewRequirementDraft(
                            localId = "d2",
                            extractionKey = "k2",
                            task = "Empty clinical waste bins",
                            requiresPhoto = true,
                            isMandatory = true,
                            confidence = 0.88,
                            evidenceQuote = "Mandatory weekly",
                            fromExtraction = true,
                        ),
                    ),
                    canWrite = true,
                    canExtract = false,
                ),
                onDraftTaskChange = {},
                onRequiresPhotoChange = {},
                onIsMandatoryChange = {},
                onSaveDraft = {},
                onNewDraft = {},
                onEditDraft = {},
                onCancelEdit = {},
                onDeleteDraft = {},
                onMoveUp = {},
                onMoveDown = {},
                onApprove = {},
                onRetryExtract = {},
                onStartExtract = {},
                onManualEntry = {},
                onRetryLoad = {},
                onOpenPlans = {},
                onBack = {},
            )
        }

        export(outDir, "03-cleaner-service") {
            TodayScreen(
                state = TodayUiState(
                    organizationName = "ClearLine Facility Services",
                    jobs = listOf(
                        TodayJobCardUi(
                            jobId = "job-meridian",
                            locationName = "Meridian Lobby",
                            clientName = "Meridian Office Tower",
                            serviceTimeLabel = "8:00 AM",
                            statusLabel = "Scheduled",
                            coveragePercent = 0,
                        ),
                    ),
                ),
                onOpenJob = {},
                onOpenSettings = {},
                onRetry = {},
            )
        }

        export(outDir, "04-evidence-coverage") {
            CoverageScreen(
                state = CoverageUiState(
                    title = "Evidence coverage",
                    coveragePercent = 85,
                    summaryLine = "4 of 4 mandatory tasks complete",
                    missingLines = emptyList(),
                    primaryAction = CoveragePrimaryAction.FinishService,
                    canFinish = true,
                ),
                onOpenTask = {},
                onRetryUpload = {},
                onFinish = {},
                onBack = {},
            )
        }

        export(outDir, "05-dispute-reconstruction") {
            DisputeDetailScreen(
                state = DisputeDetailUiState(
                    bundle = northstarDisputeBundle(),
                    aiSummary = DisputeAiSummary(
                        allegation = "Weekly dock service incomplete",
                        recordedEvidence = listOf(
                            "Dock loading bay photo uploaded before dispute filed",
                            "Break room exception logged with timestamp",
                        ),
                        neutralOverview = "Recorded evidence supports that dock service was performed with one logged exception.",
                    ),
                    canWrite = true,
                    canOpenReport = true,
                ),
                onRetry = {},
                onGenerateSummary = {},
                onOpenEvidenceReport = {},
                onBack = {},
            )
        }

        export(outDir, "06-evidence-report") {
            ReportPreviewScreen(
                state = ReportPreviewUiState(
                    preview = ServiceEvidenceReport(
                        refs = ReportEntityRefs(
                            disputeId = "dispute-northstar",
                            organizationId = "org",
                            contractId = "contract",
                            contractVersionId = "version",
                            serviceJobId = "job",
                            disputedServiceJobRequirementId = "req",
                            disputeItemIds = emptyList(),
                            serviceJobRequirementIds = listOf("req"),
                            evidenceRecordIds = emptyList(),
                            exceptionIds = emptyList(),
                            complaintAttachmentObjectPath = null,
                        ),
                        header = ServiceEvidenceReportHeader(
                            clientName = "Northstar Logistics",
                            locationName = "Dock Office",
                            serviceDate = LocalDate(2026, 9, 28),
                            complaint = "Weekly dock service incomplete",
                            disputedRequirementText = "Clean loading bay",
                            contractTitle = "Northstar weekly service",
                            contractVersionLabel = "Approved v1",
                        ),
                        contractRequirements = emptyList(),
                        scheduledService = ReportScheduledService(
                            scheduledStart = "2026-09-28T07:00:00Z",
                            scheduledEnd = "2026-09-28T09:00:00Z",
                            serviceDate = LocalDate(2026, 9, 28),
                        ),
                        assignedPersonnel = com.contractproof.domain.ReportAssignedPersonnel("Jordan Lee"),
                        timeline = emptyList(),
                        evidence = emptyList(),
                        exceptions = emptyList(),
                        acknowledgements = emptyList(),
                        coverage = emptyList(),
                        attachments = emptyList(),
                    ),
                    report = EvidenceReportRecord(
                        id = "report-1",
                        organizationId = "org",
                        disputeId = "dispute-northstar",
                        status = ReportStatus.Ready,
                        bucket = "reports",
                        objectPath = "reports/northstar.pdf",
                        generatedBy = "owner@clearline.demo",
                        generatedAt = "2026-09-29T10:05:00Z",
                        failureReason = null,
                    ),
                    generationStatus = CpWorkStatus.Uploaded,
                    signedUrl = "https://example.com/report.pdf",
                    canGenerate = true,
                ),
                onGenerate = {},
                onRetry = {},
                onOpen = {},
                onShare = {},
                onReload = {},
                onOpenPlans = {},
                onBack = {},
            )
        }

        export(outDir, "07-paywall") {
            PaywallScreen(
                state = PaywallUiState(
                    phase = PaywallPhase.Ready,
                    snapshot = SubscriptionSnapshot(
                        plan = SubscriptionPlan.Free,
                        status = SubscriptionStatus.Active,
                        source = SubscriptionSource.RevenueCat,
                    ),
                    offerings = listOf(
                        PlanOffering(
                            plan = SubscriptionPlan.Pro,
                            title = "ContractProof Pro",
                            priceLabel = "Unlimited extraction & dispute PDFs",
                            packageIdentifier = "pro_monthly",
                        ),
                        PlanOffering(
                            plan = SubscriptionPlan.Business,
                            title = "Business",
                            priceLabel = "Team seats & priority support",
                            packageIdentifier = "business_monthly",
                        ),
                    ),
                    configured = true,
                ),
                onPurchase = {},
                onRestore = {},
                onRetry = {},
                onBack = {},
            )
        }
    }

    private fun export(outDir: File, baseName: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            ContractProofTheme(content = content)
        }
        composeRule.waitForIdle()
        val file = File(outDir, "$baseName.png")
        FileOutputStream(file).use { stream ->
            composeRule.onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun northstarDisputeBundle(): com.contractproof.domain.DisputeReconstructionBundle {
        val job = ServiceJob(
            id = "job-northstar",
            organizationId = "org-1",
            client = ServiceJobClientRef("northstar", "Northstar Logistics"),
            location = ServiceJobLocationRef("dock", "Dock Office", "America/Chicago"),
            serviceDate = LocalDate(2026, 9, 28),
            scheduledStart = "2026-09-28T07:00:00Z",
            scheduledEnd = "2026-09-28T09:00:00Z",
            assignee = ServiceJobAssignee("cleaner", "Jordan Lee"),
            status = ServiceJobRules.Disputed,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "req-dock",
                    contractRequirementId = "cr-dock",
                    requirementText = "Clean loading bay",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Satisfied,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 1, 100, ServiceJobEvidenceState.ReadyToComplete),
            contractId = "ct-northstar",
            contractVersionId = "cv-northstar",
            scheduleId = null,
            startedAt = "2026-09-28T07:12:00Z",
            completedAt = "2026-09-28T08:40:00Z",
        )
        val dispute = Dispute(
            id = "dispute-northstar",
            organizationId = "org-1",
            clientId = "northstar",
            clientName = "Northstar Logistics",
            locationId = "dock",
            locationName = "Dock Office",
            serviceDate = job.serviceDate,
            complaint = "Weekly dock service incomplete",
            serviceJobId = job.id,
            disputedServiceJobRequirementId = "req-dock",
            complaintAttachmentObjectPath = null,
            complaintAttachmentMimeType = null,
            recordedBy = "client@clearline.demo",
            status = DisputeStatus.Open,
            syncStatus = EvidenceSyncStatus.Uploaded,
            createdAt = "2026-09-29T09:00:00Z",
        )
        val evidence = Evidence(
            id = "ev-dock",
            organizationId = "org-1",
            serviceJobId = job.id,
            serviceJobRequirementId = "req-dock",
            capturedByUserId = "cleaner",
            capturedAt = "2026-09-28T08:15:00Z",
            type = EvidenceType.Photo,
            location = null,
            file = EvidenceFile(
                id = "file-dock",
                evidenceRecordId = "ev-dock",
                bucket = "evidence",
                objectPath = "org/job/req/ev-dock",
                mimeType = "image/jpeg",
                byteSize = 120_000,
                sha256 = "abc",
                syncStatus = EvidenceSyncStatus.Uploaded,
                uploadedAt = "2026-09-28T08:16:00Z",
            ),
            syncStatus = EvidenceSyncStatus.Uploaded,
        )
        val exception = JobExceptionRecord(
            id = "ex-break",
            serviceJobRequirementId = "req-dock",
            reason = "Break room — client equipment blocking access",
            recordedAt = "2026-09-28T08:20:00Z",
            recordedBy = "cleaner",
            syncStatus = EvidenceSyncStatus.Uploaded,
        )
        return DisputeReconstructionRules.build(
            dispute = dispute,
            items = listOf(
                DisputeItem(
                    id = "item-1",
                    disputeId = dispute.id,
                    serviceJobId = job.id,
                    serviceJobRequirementId = "req-dock",
                    evidenceRecordId = "ev-dock",
                    exceptionId = null,
                    outcome = DisputeOutcome.Satisfied,
                    createdAt = dispute.createdAt,
                ),
            ),
            job = job,
            disputedRequirementText = "Clean loading bay",
            contract = DisputeContractRef("ct-northstar", "Northstar weekly service", "Approved v1"),
            evidence = listOf(evidence),
            exceptions = listOf(exception),
        )
    }
}
