package com.contractproof.domain

import com.contractproof.domain.fixtures.ExtractionFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExtractionApprovalGateTest {
    private val org = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"

    @Test
    fun invalidExtractionCannotProduceNormalizedCandidates() {
        assertFailsWith<ExtractionRuleViolation> {
            ExtractionRules.requireCompleted(ExtractionFixtures.duplicateRequirements())
        }
    }

    @Test
    fun validExtractionProducesDraftsButApproveRequiresRequirementCount() {
        val drafts = ExtractionReviewRules.draftsFromExtraction(ExtractionFixtures.normalContract())
        assertTrue(drafts.isNotEmpty())
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireApprove(
                Access.forRole(Role.Owner),
                version(ContractVersionRules.Extracted),
                org,
                requirementCount = 0,
            )
        }
    }

    @Test
    fun approveAllowsUploadedOrExtractedWithRequirements() {
        ContractVersionRules.requireApprove(
            Access.forRole(Role.Owner),
            version(ContractVersionRules.Uploaded),
            org,
            requirementCount = 1,
        )
        ContractVersionRules.requireApprove(
            Access.forRole(Role.Owner),
            version(ContractVersionRules.Extracted),
            org,
            requirementCount = 2,
        )
    }

    @Test
    fun approvedVersionCannotBeApprovedAgain() {
        assertFailsWith<ContractRuleViolation> {
            ContractVersionRules.requireApprove(
                Access.forRole(Role.Owner),
                version(ContractVersionRules.Approved),
                org,
                requirementCount = 1,
            )
        }
    }

    private fun version(status: String): ContractVersionRecord {
        return ContractVersionRecord(
            id = "v1",
            organizationId = org,
            contractId = "c1",
            versionNumber = 1,
            status = status,
            effectiveOn = "2026-10-01",
            createdBy = "11111111-1111-1111-1111-111111111111",
            documentPath = "org/c1/v1.pdf",
            documentFileName = "contract.pdf",
            documentByteSize = 1000,
            documentMimeType = "application/pdf",
        )
    }
}
