package com.contractproof.domain

object Entitlements {
    fun canExtractContracts(access: Access, subscription: SubscriptionSnapshot): Boolean {
        val plan = subscription.effectivePlan()
        return SubscriptionEntitlementRules.canRunContractExtraction(plan, access)
    }

    fun canGenerateReport(access: Access, subscription: SubscriptionSnapshot): Boolean {
        val plan = subscription.effectivePlan()
        return SubscriptionEntitlementRules.canGenerateDisputeReport(plan, access)
    }

    fun canCreateLocation(
        access: Access,
        subscription: SubscriptionSnapshot,
        activeLocationCount: Int,
    ): Boolean {
        if (!access.canAddLocation) {
            return false
        }
        val plan = subscription.effectivePlan()
        val max = SubscriptionEntitlementRules.maxActiveLocations(plan)
        return activeLocationCount < max
    }

    fun locationLimitReached(subscription: SubscriptionSnapshot, activeLocationCount: Int): Boolean {
        val plan = subscription.effectivePlan()
        val max = SubscriptionEntitlementRules.maxActiveLocations(plan)
        return activeLocationCount >= max
    }
}
