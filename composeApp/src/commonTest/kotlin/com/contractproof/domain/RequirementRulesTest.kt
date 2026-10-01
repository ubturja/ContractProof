package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequirementRulesTest {
    private val org = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
    private val other = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"

    @Test
    fun emptyTaskIsRejected() {
        assertFailsWith<RequirementRuleViolation> { RequirementRules.requireTask("  ") }
    }

    @Test
    fun negativeSortIsRejected() {
        assertFailsWith<RequirementRuleViolation> { RequirementRules.requireSortOrder(-1) }
        assertEquals(0, RequirementRules.requireSortOrder(0))
    }

    @Test
    fun sourceMustBeKnown() {
        assertFailsWith<RequirementRuleViolation> { RequirementRules.requireSource("model") }
        assertFailsWith<RequirementRuleViolation> { RequirementRules.requireSource("template") }
        assertEquals(RequirementRules.SourceManual, RequirementRules.requireSource("manual"))
        assertEquals(RequirementRules.SourceExtraction, RequirementRules.requireSource("extraction"))
    }

    @Test
    fun managerCannotWrite() {
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireWrite(Access.forRole(Role.Manager))
        }
        RequirementRules.requireWrite(Access.forRole(Role.Owner))
    }

    @Test
    fun foreignOrganizationIsRejected() {
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireOrganization(other, org)
        }
        RequirementRules.requireOrganization(org, org)
    }

    @Test
    fun approvedAndSupersededAreFrozen() {
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireEditable(ContractVersionRules.Approved)
        }
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireEditable(ContractVersionRules.Superseded)
        }
        RequirementRules.requireEditable(ContractVersionRules.Uploaded)
        RequirementRules.requireEditable(ContractVersionRules.Extracted)
    }

    @Test
    fun visitWeekdayAndWindowMustBeValid() {
        val valid = visit()
        assertEquals("08:00:00", RequirementRules.requireVisit(valid.copy(startTime = "08:00")).startTime)
        assertFailsWith<RequirementRuleViolation> { RequirementRules.requireVisit(valid.copy(weekday = 0)) }
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireVisit(valid.copy(startTime = "09:00:00", endTime = "09:00:00"))
        }
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireVisit(valid.copy(startsOn = "2026-10-02", endsOn = "2026-10-01"))
        }
        assertFailsWith<RequirementRuleViolation> { RequirementRules.requireVisit(valid.copy(timezone = " ")) }
    }

    @Test
    fun optionalAndNoPhotoAreValid() {
        val record = sample(isMandatory = false, requiresPhoto = false)
        assertEquals("Optional", RequirementRules.requiredLabel(record.isMandatory))
        assertEquals("No photo required", RequirementRules.evidenceLabel(record.requiresPhoto))
        assertTrue(record.task.isNotEmpty())
    }

    @Test
    fun manualSourceAndWeeklyDayAreRequired() {
        RequirementRules.requireManualSource(RequirementRules.SourceManual)
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireManualSource(RequirementRules.SourceExtraction)
        }
        assertEquals(3, RequirementRules.requireWeekdayWhenWeekly(RequirementFrequency.Weekly, 3))
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.requireWeekdayWhenWeekly(RequirementFrequency.Weekly, null)
        }
        assertEquals("Wednesday", RequirementRules.weekdayLabel(3))
        assertEquals("Weekly", RequirementRules.frequencyLabel(RequirementFrequency.Weekly))
    }

    @Test
    fun reorderAssignsZeroBasedOrder() {
        val pairs = RequirementRules.reorderSortOrders(listOf("a", "b", "c"))
        assertEquals(listOf("a" to 0, "b" to 1, "c" to 2), pairs)
        assertFailsWith<RequirementRuleViolation> {
            RequirementRules.reorderSortOrders(listOf("a", "a"))
        }
        assertEquals(
            listOf("b", "a", "c"),
            RequirementRules.moveInOrder(listOf("a", "b", "c"), "a", 1),
        )
        assertEquals(
            listOf("a", "b", "c"),
            RequirementRules.moveInOrder(listOf("a", "b", "c"), "a", -1),
        )
    }

    @Test
    fun isActiveOnlyWhenParentIsApproved() {
        assertTrue(RequirementRules.isActive(ContractVersionRules.Approved))
        assertFalse(RequirementRules.isActive(ContractVersionRules.Uploaded))
        assertFalse(RequirementRules.isActive(ContractVersionRules.Extracted))
        assertFalse(RequirementRules.isActive(ContractVersionRules.Superseded))
        assertEquals("Active", RequirementRules.activeLabel(true))
        assertEquals("Inactive", RequirementRules.activeLabel(false))
    }

    private fun visit(): RequirementVisit {
        return RequirementVisit(
            weekday = 1,
            startTime = "08:00:00",
            endTime = "10:00:00",
            timezone = "America/New_York",
            startsOn = "2026-10-01",
            endsOn = "2026-12-31",
            status = RequirementRules.ScheduleActive,
        )
    }

    private fun sample(
        isMandatory: Boolean,
        requiresPhoto: Boolean,
    ): RequirementRecord {
        return RequirementRecord(
            id = "r1",
            organizationId = org,
            contractId = "c1",
            contractVersionId = "v1",
            locationId = "l1",
            locationName = "Lobby",
            zoneCode = "A",
            task = RequirementRules.requireTask(" Sweep lobby "),
            sortOrder = RequirementRules.requireSortOrder(0),
            requiresPhoto = requiresPhoto,
            isMandatory = isMandatory,
            source = RequirementRules.SourceManual,
            extractionKey = null,
            isActive = false,
            visits = RequirementRules.requireVisits(listOf(visit())),
        )
    }
}
