package com.contractproof.domain

import com.contractproof.domain.Access.Companion.forMembership
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionEntitlementRulesTest {
    private val owner = forMembership("owner")
    private val manager = forMembership("manager")

    @Test
    fun maxActiveLocations() {
        assertEquals(1, SubscriptionEntitlementRules.maxActiveLocations(SubscriptionPlan.Free))
        assertEquals(Int.MAX_VALUE, SubscriptionEntitlementRules.maxActiveLocations(SubscriptionPlan.Pro))
    }

    @Test
    fun extractionRequiresProAndOwnerWrite() {
        assertFalse(
            SubscriptionEntitlementRules.canRunContractExtraction(SubscriptionPlan.Free, owner),
        )
        assertTrue(
            SubscriptionEntitlementRules.canRunContractExtraction(SubscriptionPlan.Pro, owner),
        )
        assertFalse(
            SubscriptionEntitlementRules.canRunContractExtraction(SubscriptionPlan.Pro, manager),
        )
    }

    @Test
    fun reportRequiresProAndDisputeAccess() {
        assertFalse(
            SubscriptionEntitlementRules.canGenerateDisputeReport(SubscriptionPlan.Free, owner),
        )
        assertTrue(
            SubscriptionEntitlementRules.canGenerateDisputeReport(SubscriptionPlan.Pro, owner),
        )
        assertTrue(
            SubscriptionEntitlementRules.canGenerateDisputeReport(SubscriptionPlan.Pro, manager),
        )
    }

    @Test
    fun expiredSnapshotActsAsFreeInEntitlements() {
        val expired = SubscriptionSnapshot(
            plan = SubscriptionPlan.Pro,
            status = SubscriptionStatus.Expired,
            source = SubscriptionSource.RevenueCat,
        )
        assertFalse(Entitlements.canExtractContracts(owner, expired))
        assertFalse(Entitlements.canGenerateReport(owner, expired))
        assertFalse(Entitlements.canCreateLocation(owner, expired, activeLocationCount = 1))
    }
}
