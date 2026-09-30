package com.contractproof.feature.location

import com.contractproof.data.LocationFailure
import com.contractproof.data.LocationGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class LocationsControllerTest {
    @Test
    fun ownerCrudInOrganizationA() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryLocationGateway(organizations)
        gateway.registerClient("c1", organizations.membership.organizationId)
        val controller = LocationsController(organizations, gateway)
        controller.refresh()
        controller.updateDraftName("Lobby")
        controller.updateDraftTimezone("America/New_York")
        controller.updateDraftAddress("1 Main")
        controller.create("c1")
        assertEquals(1, controller.state.value.items.size)
        val created = controller.state.value.items.single()
        assertEquals("Lobby", created.name)
        assertEquals(organizations.membership.organizationId, created.organizationId)
        assertEquals("c1", created.clientId)
        controller.save(
            id = created.id,
            name = "Lobby North",
            timezone = "America/Chicago",
            address = "2 Main",
            zoneCode = "z1",
            status = LocationRules.Active,
        )
        assertEquals("Lobby North", controller.state.value.items.single().name)
        controller.save(
            id = created.id,
            name = "Lobby North",
            timezone = "America/Chicago",
            address = "2 Main",
            zoneCode = "z1",
            status = LocationRules.Archived,
        )
        assertEquals(LocationRules.Archived, controller.state.value.items.single().status)
        assertEquals(1, controller.state.value.items.size)
    }

    @Test
    fun insertWithOrganizationBDoesNotPersist() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryLocationGateway(organizations)
        gateway.registerClient("c1", organizations.membership.organizationId)
        gateway.persist(
            LocationRecord(
                id = "foreign",
                organizationId = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0",
                clientId = "c2",
                name = "Other",
                timezone = "America/Chicago",
                address = null,
                zoneCode = null,
                status = LocationRules.Active,
            ),
        )
        val controller = LocationsController(organizations, gateway)
        controller.refresh()
        assertTrue(controller.state.value.items.isEmpty())
        assertEquals(0, gateway.rows.size)
    }

    @Test
    fun managerListsAssignedLocationsOnlyAndCannotArchive() = runBlocking {
        val organizations = FixedOrganizationGateway(
            role = "manager",
            locationIds = listOf("assigned"),
        )
        val gateway = MemoryLocationGateway(organizations)
        val orgId = organizations.membership.organizationId
        gateway.persist(
            LocationRecord(
                id = "assigned",
                organizationId = orgId,
                clientId = "c1",
                name = "Lobby",
                timezone = "America/New_York",
                address = null,
                zoneCode = null,
                status = LocationRules.Active,
            ),
        )
        gateway.persist(
            LocationRecord(
                id = "other",
                organizationId = orgId,
                clientId = "c1",
                name = "Plaza",
                timezone = "America/New_York",
                address = null,
                zoneCode = null,
                status = LocationRules.Active,
            ),
        )
        val controller = LocationsController(organizations, gateway)
        controller.refresh()
        assertEquals(listOf("assigned"), controller.state.value.items.map { it.id })
        assertFalse(controller.state.value.canWrite)
        controller.updateDraftName("Garage")
        controller.updateDraftTimezone("America/New_York")
        controller.create("c1")
        assertEquals(1, controller.state.value.items.size)
        controller.save(
            id = "assigned",
            name = "Lobby",
            timezone = "America/New_York",
            address = "",
            zoneCode = "",
            status = LocationRules.Archived,
        )
        assertEquals(LocationRules.Active, controller.state.value.items.single().status)
        assertEquals(0, gateway.writes)
    }
}

private class FixedOrganizationGateway(
    role: String,
    locationIds: List<String> = emptyList(),
) : OrganizationGateway {
    val membership = Membership(
        organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
        organizationName = "Northside",
        role = role,
        locationIds = locationIds,
    )

    override suspend fun currentMembership(): Membership = membership

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class MemoryLocationGateway(
    private val organizations: FixedOrganizationGateway,
) : LocationGateway {
    val rows = mutableListOf<LocationRecord>()
    private val clients = mutableMapOf<String, String>()
    var writes: Int = 0

    fun registerClient(clientId: String, organizationId: String) {
        clients[clientId] = organizationId
    }

    fun persist(record: LocationRecord) {
        if (record.organizationId != organizations.membership.organizationId) {
            return
        }
        rows += record
    }

    override suspend fun list(): List<LocationRecord> = rows.toList()

    override suspend fun create(
        clientId: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
    ): LocationRecord {
        writes += 1
        val membership = organizations.membership
        val clientOrg = clients[clientId] ?: throw LocationFailure.Rejected
        LocationRules.requireWrite(Access.forMembership(membership.role))
        LocationRules.requireClient(clientOrg, membership.organizationId)
        val created = LocationRecord(
            id = "new",
            organizationId = membership.organizationId,
            clientId = clientId,
            name = LocationRules.requireName(name),
            timezone = LocationRules.requireTimezone(timezone),
            address = LocationRules.optionalText(address),
            zoneCode = LocationRules.optionalText(zoneCode),
            status = LocationRules.Active,
        )
        LocationRules.requireOrganization(created.organizationId, membership.organizationId)
        rows += created
        return created
    }

    override suspend fun update(
        id: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
        status: String,
    ): LocationRecord {
        writes += 1
        val membership = organizations.membership
        LocationRules.requireWrite(Access.forMembership(membership.role))
        val updated = rows.first { it.id == id }.copy(
            name = LocationRules.requireName(name),
            timezone = LocationRules.requireTimezone(timezone),
            address = LocationRules.optionalText(address),
            zoneCode = LocationRules.optionalText(zoneCode),
            status = status,
        )
        LocationRules.requireOrganization(updated.organizationId, membership.organizationId)
        rows.replaceAll { if (it.id == id) updated else it }
        return updated
    }
}
