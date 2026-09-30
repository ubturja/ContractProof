package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocationRulesTest {
    private val org = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
    private val other = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"
    private val clientId = "c1"
    private val lobby = LocationRecord(
        id = "l1",
        organizationId = org,
        clientId = clientId,
        name = "Lobby",
        timezone = "America/New_York",
        address = "1 Main St",
        zoneCode = null,
        status = LocationRules.Active,
    )

    @Test
    fun emptyNameIsRejected() {
        assertFailsWith<LocationRuleViolation> { LocationRules.requireName("  ") }
    }

    @Test
    fun foreignOrganizationIsRejected() {
        assertFailsWith<LocationRuleViolation> {
            LocationRules.requireOrganization(other, org)
        }
    }

    @Test
    fun clientInAnotherOrganizationIsRejected() {
        assertFailsWith<LocationRuleViolation> {
            LocationRules.requireClient(other, org)
        }
    }

    @Test
    fun managerCannotWrite() {
        assertFailsWith<LocationRuleViolation> {
            LocationRules.requireWrite(Access.forRole(Role.Manager))
        }
    }

    @Test
    fun ownerCanCreateAndArchive() {
        LocationRules.requireWrite(Access.forRole(Role.Owner))
        val created = LocationRecord(
            id = "l2",
            organizationId = org,
            clientId = clientId,
            name = LocationRules.requireName(" Plaza "),
            timezone = LocationRules.requireTimezone(" America/Chicago "),
            address = LocationRules.optionalText("  "),
            zoneCode = LocationRules.optionalText(" zone-1 "),
            status = LocationRules.Active,
        )
        LocationRules.requireOrganization(created.organizationId, org)
        LocationRules.requireClient(org, org)
        val archived = LocationRules.archived(created)
        assertEquals("Plaza", created.name)
        assertEquals("America/Chicago", created.timezone)
        assertEquals(null, created.address)
        assertEquals("zone-1", created.zoneCode)
        assertEquals(LocationRules.Archived, archived.status)
        assertEquals(created.id, archived.id)
        assertEquals(created.organizationId, archived.organizationId)
        assertEquals(created.clientId, archived.clientId)
    }

    @Test
    fun twoLocationsOnOneClient() {
        val plaza = lobby.copy(id = "l3", name = "Plaza")
        val found = LocationRules.forClient(listOf(lobby, plaza, lobby.copy(id = "l4", clientId = "c2")), clientId)
        assertEquals(listOf(lobby, plaza), found)
    }
}
