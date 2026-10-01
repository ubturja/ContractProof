package com.contractproof.feature.client

import com.contractproof.data.ClientFailure
import com.contractproof.data.ClientGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ClientRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.contractproof.core.analytics.NoOpProductAnalytics
import kotlinx.coroutines.runBlocking

class ClientsControllerTest {
    @Test
    fun ownerCreateThenList() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "owner")
        val gateway = MemoryClientGateway(organizations)
        val controller = ClientsController(organizations, gateway, NoOpProductAnalytics())
        controller.refresh()
        controller.updateDraftName("Lobby")

        controller.create()

        assertEquals(1, controller.state.value.items.size)
        assertEquals("Lobby", controller.state.value.items.single().name)
        assertEquals(organizations.membership?.organizationId, controller.state.value.items.single().organizationId)
        assertEquals(ClientRules.Active, controller.state.value.items.single().status)
    }

    @Test
    fun managerListWithoutWrites() = runBlocking {
        val organizations = FixedOrganizationGateway(role = "manager")
        val gateway = MemoryClientGateway(organizations)
        gateway.seed(
            ClientRecord(
                id = "c1",
                organizationId = organizations.membership!!.organizationId,
                name = "Lobby",
                status = ClientRules.Active,
            ),
        )
        val controller = ClientsController(organizations, gateway, NoOpProductAnalytics())
        controller.refresh()
        assertEquals(1, controller.state.value.items.size)
        assertFalse(controller.state.value.canWrite)
        controller.updateDraftName("Plaza")
        controller.create()
        assertEquals(1, controller.state.value.items.size)
        controller.save("c1", "Lobby", ClientRules.Archived)
        assertEquals(ClientRules.Active, controller.state.value.items.single().status)
        assertTrue(gateway.writes == 0)
    }
}

private class FixedOrganizationGateway(
    role: String,
) : OrganizationGateway {
    override suspend fun currentMembership(): Membership? = membership

    val membership = Membership(
        organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
        organizationName = "Northside",
        role = role,
    )

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class MemoryClientGateway(
    private val organizations: FixedOrganizationGateway,
) : ClientGateway {
    private val rows = mutableListOf<ClientRecord>()
    var writes: Int = 0

    fun seed(client: ClientRecord) {
        rows += client
    }

    override suspend fun list(): List<ClientRecord> = rows.toList()

    override suspend fun create(name: String): ClientRecord {
        writes += 1
        val membership = organizations.membership ?: throw ClientFailure.Rejected
        val created = ClientRecord(
            id = "new",
            organizationId = membership.organizationId,
            name = name,
            status = ClientRules.Active,
        )
        rows += created
        return created
    }

    override suspend fun update(id: String, name: String, status: String): ClientRecord {
        writes += 1
        val updated = rows.first { it.id == id }.copy(name = name, status = status)
        rows.replaceAll { if (it.id == id) updated else it }
        return updated
    }
}
