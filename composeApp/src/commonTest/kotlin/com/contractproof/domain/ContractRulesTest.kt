package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContractRulesTest {
    private val org = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
    private val other = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"
    private val location = LocationRecord(
        id = "l1",
        organizationId = org,
        clientId = "c1",
        name = "Lobby",
        timezone = "America/New_York",
        address = null,
        zoneCode = null,
        status = LocationRules.Active,
    )

    @Test
    fun emptyTitleIsRejected() {
        assertFailsWith<ContractRuleViolation> { ContractRules.requireTitle("  ") }
    }

    @Test
    fun foreignOrganizationIsRejected() {
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requireOrganization(other, org)
        }
    }

    @Test
    fun locationMustBelongToClient() {
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requireLocationForClient(location, "c2", org)
        }
    }

    @Test
    fun endDateCannotPrecedeStart() {
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requireTermOrder("2026-10-02", "2026-10-01")
        }
    }

    @Test
    fun managerCannotWrite() {
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requireWrite(Access.forRole(Role.Manager))
        }
    }

    @Test
    fun ownerCannotSetActive() {
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requireWritableStatus(ContractRules.Active)
        }
        ContractRules.requireWrite(Access.forRole(Role.Owner))
        ContractRules.requireWritableStatus(ContractRules.Draft)
        val title = ContractRules.requireTitle(" Weekday cleaning ")
        val start = ContractRules.requireIsoDate("2026-10-01", "Start date")
        val end = ContractRules.optionalEndDate("2026-12-31", start)
        ContractRules.requireLocationForClient(location, "c1", org)
        assertEquals("Weekday cleaning", title)
        assertEquals("2026-10-01", start)
        assertEquals("2026-12-31", end)
        assertFalse(ContractRules.hasDocument(sample(documentPath = null)))
        assertTrue(
            ContractRules.hasDocument(
                sample(
                    documentPath = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/c0c0c0c0-c0c0-40c0-80c0-c0c0c0c0c0c0/d0d0d0d0-d0d0-40d0-80d0-d0d0d0d0d0d0.pdf",
                ),
            ),
        )
    }

    @Test
    fun pdfMustBeValidAndBounded() {
        val pdf = "%PDF-1.4\n".encodeToByteArray()
        assertEquals("lobby.pdf", ContractRules.requirePdfFileName(" lobby.pdf "))
        ContractRules.requirePdfBytes(pdf)
        assertFailsWith<ContractRuleViolation> { ContractRules.requirePdfFileName("  ") }
        assertFailsWith<ContractRuleViolation> { ContractRules.requirePdfBytes("not-pdf".encodeToByteArray()) }
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requirePdfBytes(ByteArray(ContractRules.MaxPdfBytes + 1) { 0x25 })
        }
    }

    @Test
    fun documentPathStaysInsideOrganization() {
        val path = ContractRules.objectPath(
            organizationId = org,
            contractId = "c0c0c0c0-c0c0-40c0-80c0-c0c0c0c0c0c0",
            versionId = "d0d0d0d0-d0d0-40d0-80d0-d0d0d0d0d0d0",
        )
        assertEquals(
            "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0/c0c0c0c0-c0c0-40c0-80c0-c0c0c0c0c0c0/d0d0d0d0-d0d0-40d0-80d0-d0d0d0d0d0d0.pdf",
            path,
        )
        ContractRules.requirePathInOrganization(path, org)
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requirePathInOrganization(path, other)
        }
    }

    private fun sample(documentPath: String?): ContractRecord {
        return ContractRecord(
            id = "k1",
            organizationId = org,
            clientId = "c1",
            locationId = "l1",
            title = "Weekday cleaning",
            status = ContractRules.Draft,
            startsOn = "2026-10-01",
            endsOn = null,
            currentVersionId = null,
            documentPath = documentPath,
        )
    }
}
