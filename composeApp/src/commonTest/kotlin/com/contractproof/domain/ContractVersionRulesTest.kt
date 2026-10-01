package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContractVersionRulesTest {
    private val org = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
    private val other = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"

    @Test
    fun nextNumberStartsAtOne() {
        assertEquals(1, ContractVersionRules.nextNumber(emptyList()))
        assertEquals(3, ContractVersionRules.nextNumber(listOf(version(1), version(2))))
    }

    @Test
    fun inReviewBlocksAnotherUpload() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireCanStartVersion(listOf(version(1)))
        }
        ContractVersionRules.requireCanStartVersion(
            listOf(version(1, status = ContractVersionRules.Approved)),
        )
    }

    @Test
    fun effectiveDateCannotPrecedePrevious() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireEffectiveOn("2026-09-01", "2026-10-01")
        }
        assertEquals("2026-11-01", ContractVersionRules.requireEffectiveOn("2026-11-01", "2026-10-01"))
    }

    @Test
    fun managerCannotActivate() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireActivate(
                Access.forRole(Role.Manager),
                version(1),
                org,
            )
        }
        ContractVersionRules.requireActivate(
            Access.forRole(Role.Owner),
            version(1),
            org,
        )
    }

    @Test
    fun foreignOrganizationCannotActivate() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireActivate(
                Access.forRole(Role.Owner),
                version(1),
                other,
            )
        }
    }

    @Test
    fun approveRequiresAtLeastOneRequirement() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireApprove(
                Access.forRole(Role.Owner),
                version(1, status = ContractVersionRules.Extracted),
                org,
                requirementCount = 0,
            )
        }
        ContractVersionRules.requireApprove(
            Access.forRole(Role.Owner),
            version(1, status = ContractVersionRules.Uploaded),
            org,
            requirementCount = 1,
        )
    }

    @Test
    fun managerCannotApprove() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireApprove(
                Access.forRole(Role.Manager),
                version(1),
                org,
                requirementCount = 1,
            )
        }
    }

    @Test
    fun activateDoesNotRetargetPinnedJob() {
        val jobs = listOf(
            ServiceJobPin(
                id = "j1",
                contractId = "c1",
                contractVersionId = "v1",
            ),
        )
        val afterActivate = ContractVersionRules.keepPinnedJobs(jobs)
        assertEquals("v1", afterActivate.single().contractVersionId)
        assertFalse(ContractVersionRules.isActive(version(1, id = "v1"), currentVersionId = "v2"))
        assertTrue(ContractVersionRules.isActive(version(2, id = "v2", status = ContractVersionRules.Approved), "v2"))
    }

    private fun version(
        number: Int,
        id: String = "v$number",
        status: String = ContractVersionRules.Uploaded,
    ): ContractVersionRecord {
        return ContractVersionRecord(
            id = id,
            organizationId = org,
            contractId = "c1",
            versionNumber = number,
            status = status,
            effectiveOn = "2026-10-0$number",
            createdBy = "11111111-1111-1111-1111-111111111111",
            documentPath = null,
            documentFileName = null,
            documentByteSize = null,
            documentMimeType = null,
        )
    }
}
