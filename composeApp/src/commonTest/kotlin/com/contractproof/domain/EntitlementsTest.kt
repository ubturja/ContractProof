package com.contractproof.domain

import com.contractproof.domain.Access.Companion.forMembership
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitlementsTest {
    private val owner = forMembership("owner")
    private val pro = SubscriptionSnapshot(plan = SubscriptionPlan.Pro, status = SubscriptionStatus.Active)
    private val free = SubscriptionSnapshot(plan = SubscriptionPlan.Free, status = SubscriptionStatus.Active)

    @Test
    fun locationLimitOnFree() {
        assertTrue(Entitlements.canCreateLocation(owner, free, activeLocationCount = 0))
        assertFalse(Entitlements.canCreateLocation(owner, free, activeLocationCount = 1))
        assertTrue(Entitlements.locationLimitReached(free, activeLocationCount = 1))
    }

    @Test
    fun proAllowsMultipleLocations() {
        assertTrue(Entitlements.canCreateLocation(owner, pro, activeLocationCount = 5))
    }
}
