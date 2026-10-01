package com.contractproof.feature.extraction

import com.contractproof.data.ContractFailure
import com.contractproof.data.ContractGateway
import com.contractproof.data.ContractVersionDetail
import com.contractproof.data.ExtractionFailure
import com.contractproof.data.ExtractionGateway
import com.contractproof.data.ExtractionRunResult
import com.contractproof.data.Membership
import com.contractproof.data.NewContractDocument
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.RequirementDraft
import com.contractproof.data.RequirementFailure
import com.contractproof.data.RequirementGateway
import com.contractproof.data.RequirementVersionHeader
import com.contractproof.data.ScheduleGateway
import com.contractproof.domain.fixtures.ExtractionFixtures
import com.contractproof.domain.ContractExtractionV1
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRules
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.ExtractionDocumentInfo
import com.contractproof.domain.ExtractionPipelineInfo
import com.contractproof.domain.ExtractionRequirementCandidate
import com.contractproof.domain.ExtractionRules
import com.contractproof.domain.ExtractionVisitCandidate
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.RequirementRecord
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.RequirementVisit
import com.contractproof.domain.ScheduleRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.contractproof.core.analytics.NoOpProductAnalytics
import com.contractproof.core.observability.NoOpErrorReporter
import com.contractproof.subscription.FakeSubscriptionService
import kotlinx.coroutines.runBlocking

class ExtractionReviewControllerTest {
    @Test
    fun extractedVersionLoadsDraftsIntoReview() = runBlocking {
        val extraction = sampleExtraction()
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = extraction
        val extractions = MemoryExtractions(extraction)
        val requirements = MemoryRequirements(organizations)
        val schedules = MemorySchedules()
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            extractions,
            requirements,
            schedules,
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertEquals(ExtractionReviewPhase.Review, controller.state.value.phase)
        assertEquals(1, controller.state.value.drafts.size)
        assertTrue(controller.state.value.drafts.single().fromExtraction)
        assertTrue(controller.state.value.canWrite)
    }

    @Test
    fun startExtractionMovesThroughProcessingToReview() = runBlocking {
        val extraction = sampleExtraction()
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Uploaded
        contracts.documentPath = "org/c1/v1.pdf"
        val extractions = MemoryExtractions(extraction)
        val requirements = MemoryRequirements(organizations)
        val schedules = MemorySchedules()
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            extractions,
            requirements,
            schedules,
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        controller.startExtraction()
        assertEquals(ExtractionReviewPhase.Review, controller.state.value.phase)
        assertEquals(1, extractions.runCalls)
        assertEquals(1, controller.state.value.drafts.size)
    }

    @Test
    fun extractionErrorShowsRetryableMessage() = runBlocking {
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.documentPath = "org/c1/v1.pdf"
        val extractions = MemoryExtractions(null, failWith = ExtractionFailure.Rejected("Temporary issue.", retryable = true))
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            extractions,
            MemoryRequirements(organizations),
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        controller.startExtraction()
        assertEquals(ExtractionReviewPhase.Error, controller.state.value.phase)
        assertEquals("Temporary issue.", controller.state.value.banner)
    }

    @Test
    fun emptyDraftsBlockApprove() = runBlocking {
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = sampleExtraction().copy(requirements = emptyList())
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(null),
            MemoryRequirements(organizations),
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertEquals(ExtractionReviewPhase.Empty, controller.state.value.phase)
        assertFalse(controller.state.value.canApprove)
        assertFalse(controller.approve())
    }

    @Test
    fun approveReplacesRequirementsAndApprovesVersion() = runBlocking {
        val extraction = sampleExtraction()
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = extraction
        val requirements = MemoryRequirements(organizations)
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(extraction),
            requirements,
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertTrue(controller.approve())
        assertEquals(1, requirements.replaceCalls)
        assertEquals(1, contracts.approveCalls)
    }

    @Test
    fun managerIsReadOnly() = runBlocking {
        val extraction = sampleExtraction()
        val organizations = FixedOrg(role = "manager")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = extraction
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(extraction),
            MemoryRequirements(organizations),
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertFalse(controller.state.value.canWrite)
        assertFalse(controller.state.value.canApprove)
    }

    @Test
    fun invalidStoredExtraction_doesNotProduceApprovableDrafts() = runBlocking {
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = ExtractionFixtures.duplicateRequirements()
        val requirements = MemoryRequirements(organizations)
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(null),
            requirements,
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertEquals(ExtractionReviewPhase.Empty, controller.state.value.phase)
        assertEquals(0, requirements.replaceCalls)
        assertFalse(controller.approve())
    }

    @Test
    fun reviewableWarnings_surfaceOnLoad() = runBlocking {
        val organizations = FixedOrg(role = "owner")
        val ambiguous = ExtractionFixtures.ambiguousFrequency()
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = ambiguous
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(ambiguous),
            MemoryRequirements(organizations),
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertTrue(controller.state.value.warnings.isNotEmpty())
        assertTrue(controller.state.value.usedFallback)

        val wording = ExtractionFixtures.unsupportedWording()
        contracts.extraction = wording
        controller.load("c1", "v1")
        assertTrue(controller.state.value.warnings.any { it.contains("Unsupported phrasing") })
    }

    @Test
    fun missingEvidence_requiresOwnerEditBeforeApprove() = runBlocking {
        val extraction = ExtractionFixtures.missingEvidenceRequirement()
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = extraction
        val requirements = MemoryRequirements(organizations)
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(extraction),
            requirements,
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        val draft = controller.state.value.drafts.single()
        assertFalse(draft.requiresPhoto)
        controller.startEditDraft(draft.localId)
        controller.updateDraftRequiresPhoto(true)
        controller.saveDraft()
        assertTrue(controller.approve())
        assertEquals(true, requirements.lastReplacedItems.single().requiresPhoto)
        assertEquals("req_trash", requirements.lastReplacedItems.single().extractionKey)
    }

    @Test
    fun humanCorrections_persistEditedDraftsOnApprove() = runBlocking {
        val extraction = ExtractionFixtures.humanCorrectionsBaseline()
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.versionStatus = ContractVersionRules.Extracted
        contracts.extraction = extraction
        val requirements = MemoryRequirements(organizations)
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(extraction),
            requirements,
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        val req1 = controller.state.value.drafts.first { it.extractionKey == "req_1" }
        val req2 = controller.state.value.drafts.first { it.extractionKey == "req_2" }
        controller.startEditDraft(req1.localId)
        controller.updateDraftTask("Sweep lobby and entry")
        controller.saveDraft()
        controller.deleteDraft(req2.localId)
        controller.updateDraftTask("Sanitize restrooms")
        controller.saveDraft()
        assertTrue(controller.approve())
        assertEquals(2, requirements.lastReplacedItems.size)
        assertEquals("Sweep lobby and entry", requirements.lastReplacedItems[0].task)
        assertEquals("req_1", requirements.lastReplacedItems[0].extractionKey)
        assertEquals("Sanitize restrooms", requirements.lastReplacedItems[1].task)
        assertEquals(null, requirements.lastReplacedItems[1].extractionKey)
    }

    @Test
    fun networkOnLoadShowsConnectionBanner() = runBlocking {
        val organizations = FixedOrg(role = "owner")
        val contracts = MemoryContracts(organizations)
        contracts.failGetVersion = true
        val controller = ExtractionReviewController(
            organizations,
            contracts,
            MemoryExtractions(null),
            MemoryRequirements(organizations),
            MemorySchedules(),
            FakeSubscriptionService(),
            NoOpProductAnalytics(),
            NoOpErrorReporter(),
        )
        controller.load("c1", "v1")
        assertEquals(ExtractionReviewPhase.Error, controller.state.value.phase)
        assertEquals("You need a connection to open this contract.", controller.state.value.banner)
    }

    private fun sampleExtraction(): ContractExtractionV1 {
        return ContractExtractionV1(
            schemaVersion = ExtractionRules.SchemaVersion,
            status = ExtractionRules.StatusCompleted,
            extractedAt = "2026-10-01T12:00:00Z",
            pipeline = ExtractionPipelineInfo(
                textEngine = "unpdf",
                llmProvider = "gemini",
                llmModel = "gemini-2.0-flash",
                usedFallback = false,
            ),
            document = ExtractionDocumentInfo(
                pageCount = 1,
                textCharCount = 50,
                ocrPageIndexes = emptyList(),
                warnings = listOf("OCR used on page 2"),
            ),
            visits = listOf(
                ExtractionVisitCandidate(
                    weekday = 2,
                    startTime = "08:00:00",
                    endTime = "10:00:00",
                    timezone = "America/New_York",
                    startsOn = "2026-10-01",
                    endsOn = null,
                    confidence = 0.9,
                ),
            ),
            requirements = listOf(
                ExtractionRequirementCandidate(
                    key = "req_1",
                    task = "Sweep lobby",
                    requiresPhoto = true,
                    isMandatory = true,
                    confidence = 0.85,
                    evidenceQuote = "Weekly sweep",
                ),
            ),
        )
    }
}

private class FixedOrg(role: String) : OrganizationGateway {
    val membership = Membership(
        organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
        organizationName = "Northside",
        role = role,
        userId = "11111111-1111-1111-1111-111111111111",
    )

    override suspend fun currentMembership(): Membership? = membership

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class MemoryContracts(
    private val organizations: FixedOrg,
) : ContractGateway {
    var versionStatus: String = ContractVersionRules.Uploaded
    var documentPath: String? = null
    var documentFileName: String? = "contract.pdf"
    var extraction: ContractExtractionV1? = null
    var failGetVersion = false
    var approveCalls = 0

    override suspend fun list(): List<ContractRecord> = emptyList()

    override suspend fun listVersions(contractId: String): List<ContractVersionRecord> = emptyList()

    override suspend fun create(
        clientId: String,
        location: LocationRecord,
        title: String,
        startsOn: String,
        endsOn: String,
    ): ContractRecord {
        throw ContractFailure.Rejected
    }

    override suspend fun attachDocument(
        contractId: String,
        document: NewContractDocument,
        effectiveOn: String,
        onProgress: (Int) -> Unit,
    ): ContractRecord {
        throw ContractFailure.Rejected
    }

    override suspend fun activateVersion(contractId: String, versionId: String): ContractRecord {
        throw ContractFailure.Rejected
    }

    override suspend fun getVersion(contractId: String, versionId: String): ContractVersionDetail {
        if (failGetVersion) {
            throw ContractFailure.Network
        }
        return ContractVersionDetail(
            id = versionId,
            contractId = contractId,
            organizationId = organizations.membership.organizationId,
            status = versionStatus,
            documentFileName = documentFileName,
            documentPath = documentPath,
            extraction = extraction,
        )
    }

    override suspend fun approveVersion(contractId: String, versionId: String): ContractRecord {
        approveCalls += 1
        return ContractRecord(
            id = contractId,
            organizationId = organizations.membership.organizationId,
            clientId = "cl1",
            locationId = "l1",
            title = "Test",
            status = ContractRules.Active,
            startsOn = "2026-10-01",
            endsOn = null,
            currentVersionId = versionId,
            documentPath = documentPath,
        )
    }

    override suspend fun update(
        id: String,
        title: String,
        startsOn: String,
        endsOn: String,
        status: String,
    ): ContractRecord {
        throw ContractFailure.Rejected
    }

    override suspend fun documentUrl(path: String): String = "https://example.test/$path"
}

private class MemoryExtractions(
    private var stored: ContractExtractionV1?,
    private val failWith: ExtractionFailure? = null,
) : ExtractionGateway {
    var runCalls = 0

    override suspend fun runExtraction(contractVersionId: String, force: Boolean): ExtractionRunResult {
        runCalls += 1
        failWith?.let { throw it }
        return ExtractionRunResult(
            contractVersionId = contractVersionId,
            requirementCount = stored?.requirements?.size ?: 0,
            visitCount = stored?.visits?.size ?: 0,
            warnings = stored?.document?.warnings.orEmpty(),
        )
    }

    override suspend fun loadExtraction(contractVersionId: String): ContractExtractionV1? = stored
}

private class MemoryRequirements(
    private val organizations: FixedOrg,
) : RequirementGateway {
    var replaceCalls = 0
    var lastReplacedItems: List<RequirementDraft> = emptyList()

    override suspend fun headerForVersion(contractVersionId: String): RequirementVersionHeader {
        return RequirementVersionHeader(
            contractId = "c1",
            locationId = "l1",
            locationName = "Lobby",
            zoneCode = null,
            contractStartsOn = "2026-10-01",
            versionStatus = ContractVersionRules.Extracted,
        )
    }

    override suspend fun listForVersion(contractVersionId: String): List<RequirementRecord> = emptyList()

    override suspend fun create(
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord {
        throw RequirementFailure.Rejected
    }

    override suspend fun update(
        id: String,
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord {
        throw RequirementFailure.Rejected
    }

    override suspend fun delete(id: String, contractVersionId: String) {
    }

    override suspend fun reorder(contractVersionId: String, orderedIds: List<String>) {
    }

    override suspend fun replaceAllForVersion(contractVersionId: String, items: List<RequirementDraft>) {
        replaceCalls += 1
        lastReplacedItems = items
    }
}

private class MemorySchedules : ScheduleGateway {
    override suspend fun listForContract(contractId: String): List<ScheduleRecord> = emptyList()

    override suspend fun upsertWeeklyVisit(
        contractVersionId: String,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord {
        return upsertSchedule(contractVersionId, com.contractproof.domain.ScheduleFrequency.Weekly, scheduleId, visit)
    }

    override suspend fun upsertSchedule(
        contractVersionId: String,
        frequency: com.contractproof.domain.ScheduleFrequency,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord {
        return ScheduleRecord(
            id = "s1",
            contractId = "c1",
            locationId = "l1",
            frequency = frequency.toStorageValue(),
            visit = visit,
        )
    }

    override suspend fun setStatus(
        contractVersionId: String,
        scheduleId: String,
        status: String,
    ): ScheduleRecord {
        throw com.contractproof.data.ScheduleFailure.Rejected
    }
}
