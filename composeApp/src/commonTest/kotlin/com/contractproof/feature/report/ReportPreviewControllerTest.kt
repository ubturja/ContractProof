package com.contractproof.feature.report

import com.contractproof.data.Membership
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ReportFailure
import com.contractproof.data.ReportGateway
import com.contractproof.domain.EvidenceReportRecord
import com.contractproof.domain.ReportGenerationRequest
import com.contractproof.domain.ReportGenerationResult
import com.contractproof.domain.ReportStatus
import com.contractproof.domain.ServiceEvidenceReport
import kotlin.test.Test
import kotlin.test.assertEquals
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionSnapshot
import com.contractproof.domain.SubscriptionStatus
import com.contractproof.core.analytics.NoOpProductAnalytics
import com.contractproof.core.observability.NoOpErrorReporter
import com.contractproof.subscription.FakeSubscriptionService
import kotlinx.coroutines.runBlocking

class ReportPreviewControllerTest {
    @Test
    fun loadSetsPreviewFromGateway() = runBlocking {
        val gateway = FakeReportGateway()
        val controller = ReportPreviewController(
            FakeOrganizations(),
            gateway,
            FakeSubscriptionService(
                initial = SubscriptionSnapshot(
                    plan = SubscriptionPlan.Pro,
                    status = SubscriptionStatus.Active,
                ),
            ),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("d1")
        assertEquals("Acme", controller.state.value.preview?.header?.clientName)
    }

    private class FakeOrganizations : OrganizationGateway {
        override suspend fun currentMembership(): Membership {
            return Membership(
                organizationId = "org",
                organizationName = "Org",
                userId = "u",
                role = "manager",
                locationIds = emptyList(),
            )
        }

        override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
            error("unused")
        }
    }

    private class FakeReportGateway : ReportGateway {
        override suspend fun getForDispute(disputeId: String): EvidenceReportRecord? = null

        override suspend fun assemblePreview(disputeId: String): ServiceEvidenceReport? {
            return ServiceEvidenceReport(
                refs = com.contractproof.domain.ReportEntityRefs(
                    disputeId = disputeId,
                    organizationId = "org",
                    contractId = "c",
                    contractVersionId = "v",
                    serviceJobId = "j",
                    disputedServiceJobRequirementId = "r",
                    disputeItemIds = emptyList(),
                    serviceJobRequirementIds = listOf("r"),
                    evidenceRecordIds = emptyList(),
                    exceptionIds = emptyList(),
                    complaintAttachmentObjectPath = null,
                ),
                header = com.contractproof.domain.ServiceEvidenceReportHeader(
                    clientName = "Acme",
                    locationName = "Site",
                    serviceDate = kotlinx.datetime.LocalDate(2026, 1, 1),
                    complaint = "Issue",
                    disputedRequirementText = "Clean",
                    contractTitle = "Contract",
                    contractVersionLabel = "v1",
                ),
                contractRequirements = emptyList(),
                scheduledService = com.contractproof.domain.ReportScheduledService(
                    scheduledStart = "t0",
                    scheduledEnd = "t1",
                    serviceDate = kotlinx.datetime.LocalDate(2026, 1, 1),
                ),
                assignedPersonnel = com.contractproof.domain.ReportAssignedPersonnel("Worker"),
                timeline = emptyList(),
                evidence = emptyList(),
                exceptions = emptyList(),
                acknowledgements = emptyList(),
                coverage = emptyList(),
                attachments = emptyList(),
            )
        }

        override suspend fun requestGeneration(request: ReportGenerationRequest): ReportGenerationResult {
            throw ReportFailure.Rejected("not used")
        }

        override suspend fun signedPdfUrl(objectPath: String): String = "https://example.com/report.pdf"
    }
}
