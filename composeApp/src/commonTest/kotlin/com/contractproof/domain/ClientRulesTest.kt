package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClientRulesTest {
    private val org = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
    private val other = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"
    private val lobby = ClientRecord(
        id = "c1",
        organizationId = org,
        name = "Lobby",
        status = ClientRules.Active,
    )

    @Test
    fun emptyNameIsRejected() {
        assertFailsWith<ClientRuleViolation> { ClientRules.requireName("  ") }
    }

    @Test
    fun foreignOrganizationIsRejected() {
        assertFailsWith<ClientRuleViolation> {
            ClientRules.requireOrganization(other, org)
        }
    }

    @Test
    fun managerCannotWrite() {
        assertFailsWith<ClientRuleViolation> {
            ClientRules.requireWrite(Access.forRole(Role.Manager))
        }
    }

    @Test
    fun ownerCanCreateAndArchive() {
        ClientRules.requireWrite(Access.forRole(Role.Owner))
        val created = ClientRecord(
            id = "c2",
            organizationId = org,
            name = ClientRules.requireName(" Northside "),
            status = ClientRules.Active,
        )
        ClientRules.requireOrganization(created.organizationId, org)
        val archived = ClientRules.archived(created)
        assertEquals("Northside", created.name)
        assertEquals(ClientRules.Archived, archived.status)
        assertEquals(created.id, archived.id)
        assertEquals(created.organizationId, archived.organizationId)
    }

    @Test
    fun searchFiltersByName() {
        val plaza = lobby.copy(id = "c3", name = "Plaza")
        val found = ClientRules.search(listOf(lobby, plaza), "lob")
        assertEquals(listOf(lobby), found)
    }

    @Test
    fun archiveIsStatusNotDelete() {
        val archived = ClientRules.archived(lobby)
        assertEquals(ClientRules.Archived, archived.status)
        assertEquals(lobby.id, archived.id)
        assertTrue(listOf(lobby, archived).size == 2)
    }
}
