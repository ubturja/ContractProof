package com.contractproof.domain

object SubscriptionEntitlementRules {
    fun maxActiveLocations(plan: SubscriptionPlan): Int {
        return when (plan) {
            SubscriptionPlan.Free -> 1
            SubscriptionPlan.Pro,
            SubscriptionPlan.Business,
            -> Int.MAX_VALUE
        }
    }

    fun canRunContractExtraction(plan: SubscriptionPlan, access: Access): Boolean {
        return access.canWriteContracts && plan != SubscriptionPlan.Free
    }

    fun canGenerateDisputeReport(plan: SubscriptionPlan, access: Access): Boolean {
        return access.canOpenDisputes && plan != SubscriptionPlan.Free
    }

    fun canManageMultipleManagers(plan: SubscriptionPlan): Boolean {
        return plan == SubscriptionPlan.Business
    }
}
