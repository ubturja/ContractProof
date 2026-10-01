package com.contractproof.feature.requirement

import com.contractproof.data.ContractFailure
import com.contractproof.data.ContractGateway
import com.contractproof.data.ContractVersionDetail
import com.contractproof.data.LocationFailure
import com.contractproof.data.NewContractDocument
import com.contractproof.data.RequirementDraft
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRules
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.data.LocationGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.RequirementFailure
import com.contractproof.data.RequirementGateway
import com.contractproof.data.RequirementVersionHeader
import com.contractproof.data.ScheduleFailure
import com.contractproof.data.ScheduleGateway
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.ScheduleFrequency
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules
import com.contractproof.domain.RequirementRecord
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.RequirementVisit
import com.contractproof.domain.ScheduleRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class RequirementsControllerTest {
    @Test
    fun ownerCreatesAndReordersRequirements() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val requirements = MemoryRequirementGateway(organizations)
        val schedules = MemoryScheduleGateway()
        val locations = MemoryLocationGateway(organizations)
        val contracts = MemoryContractGatewayForRequirements(organizations)
        val controller = RequirementsController(organizations, requirements, schedules, locations, contracts)
        controller.refresh("c1", "v1")
        assertTrue(controller.state.value.canWrite)
        controller.updateDraftTask("Sweep lobby")
        controller.saveRequirement()
        assertEquals(1, controller.state.value.items.size)
        controller.updateDraftTask("Mop kitchen")
        controller.saveRequirement()
        assertEquals(2, controller.state.value.items.size)
        val first = controller.state.value.items.minBy { it.sortOrder }.id
        val second = controller.state.value.items.maxBy { it.sortOrder }.id
        controller.moveRequirement(second, -1)
        assertEquals(second, controller.state.value.items.minBy { it.sortOrder }.id)
        controller.deleteRequirement(first)
        assertEquals(1, controller.state.value.items.size)
    }

    @Test
    fun managerIsReadOnly() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "manager")
        val requirements = MemoryRequirementGateway(organizations)
        val schedules = MemoryScheduleGateway()
        val locations = MemoryLocationGateway(organizations)
        requirements.seed(
            sampleRequirement(id = "r1", task = "Sweep"),
        )
        val contracts = MemoryContractGatewayForRequirements(organizations)
        val controller = RequirementsController(organizations, requirements, schedules, locations, contracts)
        controller.refresh("c1", "v1")
        assertFalse(controller.state.value.canWrite)
        assertFalse(controller.state.value.editable)
        controller.updateDraftTask("Changed")
        controller.saveRequirement()
        assertEquals("Sweep", controller.state.value.items.single().task)
    }

    @Test
    fun approvedVersionIsReadOnly() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val requirements = MemoryRequirementGateway(organizations, versionStatus = ContractVersionRules.Approved)
        val schedules = MemoryScheduleGateway()
        val locations = MemoryLocationGateway(organizations)
        val contracts = MemoryContractGatewayForRequirements(organizations)
        val controller = RequirementsController(organizations, requirements, schedules, locations, contracts)
        controller.refresh("c1", "v1")
        assertFalse(controller.state.value.editable)
        assertTrue(controller.state.value.banner?.contains("read-only") == true)
    }

    @Test
    fun saveSchedulePassesSelectedFrequency() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val requirements = MemoryRequirementGateway(organizations)
        val schedules = MemoryScheduleGateway()
        val locations = MemoryLocationGateway(organizations)
        val contracts = MemoryContractGatewayForRequirements(organizations)
        val controller = RequirementsController(organizations, requirements, schedules, locations, contracts)
        controller.refresh("c1", "v1")
        controller.updateDraftFrequency(ScheduleFrequency.Daily)
        controller.updateDraftStartsOn("2026-10-01")
        controller.saveSchedule()
        assertEquals(ScheduleFrequency.Daily, schedules.lastFrequency)
    }

    fun networkFailureShowsBanner() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val requirements = MemoryRequirementGateway(organizations)
        requirements.failNextList = true
        val schedules = MemoryScheduleGateway()
        val locations = MemoryLocationGateway(organizations)
        val contracts = MemoryContractGatewayForRequirements(organizations)
        val controller = RequirementsController(organizations, requirements, schedules, locations, contracts)
        controller.refresh("c1", "v1")
        assertEquals("You need a connection to load requirements.", controller.state.value.banner)
    }

    @Test
    fun ownerApprovesAfterRequirementsSaved() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val requirements = MemoryRequirementGateway(organizations)
        val schedules = MemoryScheduleGateway()
        val locations = MemoryLocationGateway(organizations)
        val contracts = MemoryContractGatewayForRequirements(organizations)
        val controller = RequirementsController(organizations, requirements, schedules, locations, contracts)
        controller.refresh("c1", "v1")
        controller.updateDraftTask("Sweep lobby")
        controller.saveRequirement()
        contracts.requirementCountForApprove = controller.state.value.items.size
        assertTrue(controller.approve("c1", "v1"))
        assertEquals(1, contracts.approveCalls)
    }

    private fun sampleRequirement(id: String, task: String): RequirementRecord {
        return RequirementRecord(
            id = id,
            organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
            contractId = "c1",
            contractVersionId = "v1",
            locationId = "l1",
            locationName = "Lobby",
            zoneCode = null,
            task = task,
            sortOrder = 0,
            requiresPhoto = true,
            isMandatory = true,
            source = RequirementRules.SourceManual,
            extractionKey = null,
            isActive = false,
            visits = emptyList(),
        )
    }
}

private class FixedOrganizationGateway(
    role: String,
) : OrganizationGateway {
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

private class MemoryLocationGateway(
    private val organizations: FixedOrganizationGateway,
) : LocationGateway {
    private val rows = mutableListOf(
        LocationRecord(
            id = "l1",
            organizationId = organizations.membership!!.organizationId,
            clientId = "cl1",
            name = "Lobby",
            timezone = "America/New_York",
            address = null,
            zoneCode = null,
            status = LocationRules.Active,
        ),
    )

    override suspend fun list(): List<LocationRecord> = rows.toList()

    override suspend fun create(
        clientId: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
    ): LocationRecord {
        throw LocationFailure.Rejected
    }

    override suspend fun update(
        id: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
        status: String,
    ): LocationRecord {
        val index = rows.indexOfFirst { it.id == id }
        val updated = rows[index].copy(zoneCode = zoneCode.trim().ifEmpty { null })
        rows[index] = updated
        return updated
    }
}

private class MemoryRequirementGateway(
    private val organizations: FixedOrganizationGateway,
    private val versionStatus: String = ContractVersionRules.Uploaded,
) : RequirementGateway {
    private val rows = mutableListOf<RequirementRecord>()
    var failNextList = false

    fun seed(record: RequirementRecord) {
        rows += record
    }

    override suspend fun headerForVersion(contractVersionId: String): RequirementVersionHeader {
        if (failNextList) throw RequirementFailure.Network
        return RequirementVersionHeader(
            contractId = "c1",
            locationId = "l1",
            locationName = "Lobby",
            zoneCode = null,
            contractStartsOn = "2026-10-01",
            versionStatus = versionStatus,
        )
    }

    override suspend fun listForVersion(contractVersionId: String): List<RequirementRecord> {
        if (failNextList) throw RequirementFailure.Network
        return rows.filter { it.contractVersionId == contractVersionId }.sortedBy { it.sortOrder }
    }

    override suspend fun create(
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord {
        val record = sampleRequirement(
            id = "r${rows.size + 1}",
            task = task,
            sortOrder = sortOrder,
            contractVersionId = contractVersionId,
        ).copy(requiresPhoto = requiresPhoto, isMandatory = isMandatory)
        rows += record
        return record
    }

    override suspend fun update(
        id: String,
        contractVersionId: String,
        task: String,
        requiresPhoto: Boolean,
        isMandatory: Boolean,
        sortOrder: Int,
    ): RequirementRecord {
        val index = rows.indexOfFirst { it.id == id }
        val updated = rows[index].copy(
            task = task,
            requiresPhoto = requiresPhoto,
            isMandatory = isMandatory,
            sortOrder = sortOrder,
        )
        rows[index] = updated
        return updated
    }

    override suspend fun delete(id: String, contractVersionId: String) {
        rows.removeAll { it.id == id }
    }

    override suspend fun reorder(contractVersionId: String, orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            val rowIndex = rows.indexOfFirst { it.id == id }
            rows[rowIndex] = rows[rowIndex].copy(sortOrder = index)
        }
    }

    override suspend fun replaceAllForVersion(contractVersionId: String, items: List<RequirementDraft>) {
        rows.removeAll { it.contractVersionId == contractVersionId }
        items.forEachIndexed { index, item ->
            rows += sampleRequirement(
                id = "r${rows.size + 1}",
                task = item.task,
                sortOrder = index,
                contractVersionId = contractVersionId,
            )
        }
    }

    private fun sampleRequirement(
        id: String,
        task: String,
        sortOrder: Int = 0,
        contractVersionId: String = "v1",
    ): RequirementRecord {
        return RequirementRecord(
            id = id,
            organizationId = organizations.membership!!.organizationId,
            contractId = "c1",
            contractVersionId = contractVersionId,
            locationId = "l1",
            locationName = "Lobby",
            zoneCode = null,
            task = task,
            sortOrder = sortOrder,
            requiresPhoto = true,
            isMandatory = true,
            source = RequirementRules.SourceManual,
            extractionKey = null,
            isActive = false,
            visits = emptyList(),
        )
    }
}

private class MemoryContractGatewayForRequirements(
    private val organizations: FixedOrganizationGateway,
) : ContractGateway {
    var approveCalls = 0
    var requirementCountForApprove: Int = 0

    override suspend fun list(): List<ContractRecord> = emptyList()

    override suspend fun listVersions(contractId: String): List<ContractVersionRecord> = emptyList()

    override suspend fun create(
        clientId: String,
        location: com.contractproof.domain.LocationRecord,
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
        return ContractVersionDetail(
            id = versionId,
            contractId = contractId,
            organizationId = organizations.membership!!.organizationId,
            status = ContractVersionRules.Uploaded,
            documentFileName = null,
            documentPath = null,
            extraction = null,
        )
    }

    override suspend fun approveVersion(contractId: String, versionId: String): ContractRecord {
        approveCalls += 1
        if (requirementCountForApprove < 1) {
            throw ContractFailure.Rejected
        }
        return ContractRecord(
            id = contractId,
            organizationId = organizations.membership!!.organizationId,
            clientId = "cl1",
            locationId = "l1",
            title = "Test",
            status = ContractRules.Active,
            startsOn = "2026-10-01",
            endsOn = null,
            currentVersionId = versionId,
            documentPath = null,
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

private class MemoryScheduleGateway : ScheduleGateway {
    private val rows = mutableListOf<ScheduleRecord>()
    var lastFrequency: ScheduleFrequency? = null

    override suspend fun listForContract(contractId: String): List<ScheduleRecord> {
        return rows.filter { it.contractId == contractId }
    }

    override suspend fun upsertWeeklyVisit(
        contractVersionId: String,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord {
        return upsertSchedule(contractVersionId, ScheduleFrequency.Weekly, scheduleId, visit)
    }

    override suspend fun upsertSchedule(
        contractVersionId: String,
        frequency: ScheduleFrequency,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord {
        lastFrequency = frequency
        if (scheduleId != null) {
            val index = rows.indexOfFirst { it.id == scheduleId }
            val updated = rows[index].copy(frequency = frequency.toStorageValue(), visit = visit)
            rows[index] = updated
            return updated
        }
        val record = ScheduleRecord(
            id = "s${rows.size + 1}",
            contractId = "c1",
            locationId = "l1",
            frequency = frequency.toStorageValue(),
            visit = visit,
        )
        rows += record
        return record
    }

    override suspend fun setStatus(
        contractVersionId: String,
        scheduleId: String,
        status: String,
    ): ScheduleRecord {
        val index = rows.indexOfFirst { it.id == scheduleId }
        val updated = rows[index].copy(visit = rows[index].visit.copy(status = status))
        rows[index] = updated
        return updated
    }
}
