package com.contractproof.feature.contract

import com.contractproof.data.ContractFailure
import com.contractproof.data.ContractGateway
import com.contractproof.data.Membership
import com.contractproof.data.NewContractDocument
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRuleViolation
import com.contractproof.domain.ContractRules
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.contractproof.core.analytics.NoOpProductAnalytics
import com.contractproof.core.observability.NoOpErrorReporter
import kotlinx.coroutines.runBlocking

class ContractsControllerTest {
    @Test
    fun ownerCreateThenList() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryContractGateway(organizations)
        val controller = ContractsController(organizations, gateway, NoOpProductAnalytics(), NoOpErrorReporter())
        controller.refresh()
        controller.updateDraftTitle("Weekday cleaning")
        controller.updateDraftClient("c1")
        controller.updateDraftLocation("l1")
        controller.updateDraftStartsOn("2026-10-01")

        val created = controller.create(lobby())

        assertEquals(1, controller.state.value.items.size)
        assertEquals("Weekday cleaning", created?.title)
        assertEquals(organizations.membership?.organizationId, created?.organizationId)
        assertEquals(ContractRules.Draft, created?.status)
        assertEquals("c1", created?.clientId)
        assertEquals("l1", created?.locationId)
    }

    @Test
    fun managerListWithoutWrites() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "manager")
        val gateway = MemoryContractGateway(organizations)
        gateway.seed(
            ContractRecord(
                id = "k1",
                organizationId = organizations.membership!!.organizationId,
                clientId = "c1",
                locationId = "l1",
                title = "Weekday cleaning",
                status = ContractRules.Draft,
                startsOn = "2026-10-01",
                endsOn = null,
                currentVersionId = null,
                documentPath = null,
            ),
        )
        val controller = ContractsController(organizations, gateway, NoOpProductAnalytics(), NoOpErrorReporter())
        controller.refresh()
        assertEquals(1, controller.state.value.items.size)
        assertFalse(controller.state.value.canWrite)
        controller.updateDraftTitle("Night cleaning")
        controller.updateDraftClient("c1")
        controller.updateDraftLocation("l1")
        controller.updateDraftStartsOn("2026-10-01")
        controller.create(lobby())
        assertEquals(1, controller.state.value.items.size)
        controller.save("k1", "Weekday cleaning", "2026-10-01", "", ContractRules.Ended)
        assertEquals(ContractRules.Draft, controller.state.value.items.single().status)
        assertTrue(gateway.writes == 0)
    }

    @Test
    fun insertWithOrganizationBDoesNotPersist() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryContractGateway(organizations)
        val controller = ContractsController(organizations, gateway, NoOpProductAnalytics(), NoOpErrorReporter())
        controller.refresh()
        controller.updateDraftTitle("Foreign")
        controller.updateDraftClient("c1")
        controller.updateDraftLocation("l1")
        controller.updateDraftStartsOn("2026-10-01")
        gateway.rejectNext = true
        val created = controller.create(lobby())
        assertEquals(null, created)
        assertEquals(0, controller.state.value.items.size)
        assertEquals("The contract was not saved.", controller.state.value.banner)
    }

    @Test
    fun retryUploadKeepsTheSameContract() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryContractGateway(organizations)
        val controller = ContractsController(organizations, gateway, NoOpProductAnalytics(), NoOpErrorReporter())
        controller.refresh()
        controller.updateDraftTitle("Weekday cleaning")
        controller.updateDraftClient("c1")
        controller.updateDraftLocation("l1")
        controller.updateDraftStartsOn("2026-10-01")
        controller.updateDraftDocument("lobby.pdf", samplePdf())
        gateway.failNextUpload = true
        val first = controller.create(lobby())
        assertEquals(null, first)
        assertEquals(1, gateway.creates)
        assertEquals(0, gateway.attaches)
        assertEquals(1, controller.state.value.items.size)
        assertTrue(controller.state.value.canRetryUpload)
        val retried = controller.retryUpload(lobby())
        assertEquals("lobby.pdf", retried?.documentFileName)
        assertEquals(1, gateway.creates)
        assertEquals(1, gateway.attaches)
        assertEquals(1, controller.state.value.items.size)
        assertEquals(100, gateway.lastProgress)
    }

    @Test
    fun managerCannotAttachDocument() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "manager")
        val gateway = MemoryContractGateway(organizations)
        gateway.seed(
            ContractRecord(
                id = "k1",
                organizationId = organizations.membership!!.organizationId,
                clientId = "c1",
                locationId = "l1",
                title = "Weekday cleaning",
                status = ContractRules.Draft,
                startsOn = "2026-10-01",
                endsOn = null,
                currentVersionId = null,
                documentPath = null,
            ),
        )
        val controller = ContractsController(organizations, gateway, NoOpProductAnalytics(), NoOpErrorReporter())
        controller.refresh()
        controller.updateDraftDocument("lobby.pdf", samplePdf())
        controller.attach("k1")
        assertEquals(0, gateway.attaches)
        assertEquals(null, controller.state.value.items.single().documentPath)
    }

    @Test
    fun secondVersionWaitsUntilActivate() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryContractGateway(organizations)
        val controller = ContractsController(organizations, gateway, NoOpProductAnalytics(), NoOpErrorReporter())
        controller.refresh()
        controller.updateDraftTitle("Weekday cleaning")
        controller.updateDraftClient("c1")
        controller.updateDraftLocation("l1")
        controller.updateDraftStartsOn("2026-10-01")
        controller.updateDraftEffectiveOn("2026-10-01")
        controller.updateDraftDocument("lobby.pdf", samplePdf())
        val created = controller.create(lobby())
        assertEquals(1, gateway.versions.size)
        assertEquals(1, gateway.versions.single().versionNumber)
        controller.refreshVersions(created!!.id)
        controller.updateDraftEffectiveOn("2026-11-01")
        controller.updateDraftDocument("lobby-v2.pdf", samplePdf())
        val blocked = controller.attach(created.id)
        assertEquals(null, blocked)
        assertEquals(1, gateway.versions.size)
        controller.activate(created.id, gateway.versions.single().id)
        assertEquals(ContractRules.Active, controller.state.value.items.single().status)
        assertEquals(ContractVersionRules.Approved, gateway.versions.single().status)
        controller.updateDraftEffectiveOn("2026-11-01")
        controller.updateDraftDocument("lobby-v2.pdf", samplePdf())
        val second = controller.attach(created.id)
        assertEquals(2, gateway.versions.size)
        assertEquals(2, gateway.versions.maxOf { it.versionNumber })
        assertEquals("lobby-v2.pdf", second?.documentFileName)
        controller.refreshVersions(created.id)
        assertEquals(2, controller.state.value.versions.size)
    }
}

private fun samplePdf(): ByteArray = "%PDF-1.4\n".encodeToByteArray()

private fun lobby(): LocationRecord {
    return LocationRecord(
        id = "l1",
        organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
        clientId = "c1",
        name = "Lobby",
        timezone = "America/New_York",
        address = null,
        zoneCode = null,
        status = LocationRules.Active,
    )
}

private class FixedOrganizationGateway(
    role: String,
) : OrganizationGateway {
    override suspend fun currentMembership(): Membership? = membership

    val membership = Membership(
        organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
        organizationName = "Northside",
        role = role,
        userId = "11111111-1111-1111-1111-111111111111",
    )

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class MemoryContractGateway(
    private val organizations: FixedOrganizationGateway,
) : ContractGateway {
    private val rows = mutableListOf<ContractRecord>()
    val versions = mutableListOf<ContractVersionRecord>()
    var writes: Int = 0
    var creates: Int = 0
    var attaches: Int = 0
    var rejectNext: Boolean = false
    var failNextUpload: Boolean = false
    var lastProgress: Int = 0

    fun seed(contract: ContractRecord) {
        rows += contract
    }

    override suspend fun list(): List<ContractRecord> = rows.toList()

    override suspend fun listVersions(contractId: String): List<ContractVersionRecord> {
        return versions.filter { it.contractId == contractId }.sortedBy { it.versionNumber }
    }

    override suspend fun create(
        clientId: String,
        location: LocationRecord,
        title: String,
        startsOn: String,
        endsOn: String,
    ): ContractRecord {
        if (rejectNext) {
            throw ContractFailure.Rejected
        }
        writes += 1
        creates += 1
        val membership = organizations.membership ?: throw ContractFailure.Rejected
        if (location.organizationId != membership.organizationId) {
            throw ContractFailure.Rejected
        }
        val created = ContractRecord(
            id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
            organizationId = membership.organizationId,
            clientId = clientId,
            locationId = location.id,
            title = title,
            status = ContractRules.Draft,
            startsOn = startsOn,
            endsOn = endsOn.trim().ifEmpty { null },
            currentVersionId = null,
            documentPath = null,
        )
        rows += created
        return created
    }

    override suspend fun attachDocument(
        contractId: String,
        document: NewContractDocument,
        effectiveOn: String,
        onProgress: (Int) -> Unit,
    ): ContractRecord {
        onProgress(0)
        onProgress(40)
        lastProgress = 40
        if (failNextUpload) {
            failNextUpload = false
            throw ContractFailure.Upload
        }
        val existing = versions.filter { it.contractId == contractId }
        try {
            ContractVersionRules.requireCanStartVersion(existing)
            ContractVersionRules.requireEffectiveOn(
                effectiveOn,
                ContractVersionRules.previousEffectiveOn(existing),
            )
        } catch (_: ContractRuleViolation) {
            throw ContractFailure.Rejected
        }
        val start = ContractVersionRules.requireEffectiveOn(
            effectiveOn,
            ContractVersionRules.previousEffectiveOn(existing),
        )
        writes += 1
        attaches += 1
        onProgress(100)
        lastProgress = 100
        val membership = organizations.membership ?: throw ContractFailure.Rejected
        val versionId = if (existing.isEmpty()) {
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1"
        } else {
            "cccccccc-cccc-4ccc-8ccc-ccccccccccc1"
        }
        val version = ContractVersionRecord(
            id = versionId,
            organizationId = membership.organizationId,
            contractId = contractId,
            versionNumber = ContractVersionRules.nextNumber(existing),
            status = ContractVersionRules.Uploaded,
            effectiveOn = start,
            createdBy = membership.userId,
            documentPath = "${membership.organizationId}/$contractId/$versionId.pdf",
            documentFileName = document.fileName,
            documentByteSize = document.bytes.size.toLong(),
            documentMimeType = ContractRules.PdfMimeType,
        )
        versions += version
        val updated = rows.first { it.id == contractId }.copy(
            documentPath = version.documentPath,
            documentFileName = version.documentFileName,
            documentByteSize = version.documentByteSize,
            documentMimeType = version.documentMimeType,
        )
        rows.replaceAll { if (it.id == contractId) updated else it }
        return updated
    }

    var extractionsByVersion: MutableMap<String, com.contractproof.domain.ContractExtractionV1?> = mutableMapOf()
    var requirementCountForApprove: Int = 1

    override suspend fun getVersion(contractId: String, versionId: String): com.contractproof.data.ContractVersionDetail {
        val version = versions.first { it.id == versionId && it.contractId == contractId }
        return com.contractproof.data.ContractVersionDetail(
            id = version.id,
            contractId = version.contractId,
            organizationId = version.organizationId,
            status = version.status,
            documentFileName = version.documentFileName,
            documentPath = version.documentPath,
            extraction = extractionsByVersion[versionId],
        )
    }

    override suspend fun approveVersion(contractId: String, versionId: String): ContractRecord {
        writes += 1
        val version = versions.first { it.id == versionId && it.contractId == contractId }
        try {
            ContractVersionRules.requireApprove(
                Access.forMembership(organizations.membership.role),
                version,
                organizations.membership.organizationId,
                requirementCountForApprove,
            )
        } catch (_: ContractRuleViolation) {
            throw ContractFailure.Rejected
        }
        versions.replaceAll { row ->
            when {
                row.id == versionId -> row.copy(status = ContractVersionRules.Approved)
                row.contractId == contractId && row.status == ContractVersionRules.Approved ->
                    row.copy(status = ContractVersionRules.Superseded)
                else -> row
            }
        }
        val updated = rows.first { it.id == contractId }.copy(
            status = ContractRules.Active,
            currentVersionId = versionId,
        )
        rows.replaceAll { if (it.id == contractId) updated else it }
        return updated
    }

    override suspend fun activateVersion(contractId: String, versionId: String): ContractRecord {
        writes += 1
        val version = versions.first { it.id == versionId && it.contractId == contractId }
        try {
            ContractVersionRules.requireActivate(
                Access.forMembership(organizations.membership.role),
                version,
                organizations.membership.organizationId,
            )
        } catch (_: ContractRuleViolation) {
            throw ContractFailure.Rejected
        }
        versions.replaceAll { row ->
            when {
                row.id == versionId -> row.copy(status = ContractVersionRules.Approved)
                row.contractId == contractId && row.status == ContractVersionRules.Approved ->
                    row.copy(status = ContractVersionRules.Superseded)
                else -> row
            }
        }
        val updated = rows.first { it.id == contractId }.copy(
            status = ContractRules.Active,
            currentVersionId = versionId,
        )
        rows.replaceAll { if (it.id == contractId) updated else it }
        return updated
    }

    override suspend fun update(
        id: String,
        title: String,
        startsOn: String,
        endsOn: String,
        status: String,
    ): ContractRecord {
        writes += 1
        val updated = rows.first { it.id == id }.copy(
            title = title,
            startsOn = startsOn,
            endsOn = endsOn.trim().ifEmpty { null },
            status = status,
        )
        rows.replaceAll { if (it.id == id) updated else it }
        return updated
    }

    override suspend fun documentUrl(path: String): String {
        val membership = organizations.membership ?: throw ContractFailure.Rejected
        ContractRules.requirePathInOrganization(path, membership.organizationId)
        return "https://example.test/$path"
    }
}
