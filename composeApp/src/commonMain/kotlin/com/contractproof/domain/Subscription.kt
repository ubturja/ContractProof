package com.contractproof.domain

enum class SubscriptionPlan {
    Free,
    Pro,
    Business,
}

enum class SubscriptionStatus {
    Active,
    Expired,
    BillingIssue,
    Unknown,
}

enum class SubscriptionSource {
    RevenueCat,
    Disabled,
    DemoBypass,
}

data class SubscriptionSnapshot(
    val plan: SubscriptionPlan = SubscriptionPlan.Free,
    val status: SubscriptionStatus = SubscriptionStatus.Active,
    val source: SubscriptionSource = SubscriptionSource.Disabled,
    val expiresAt: String? = null,
) {
    fun effectivePlan(): SubscriptionPlan {
        return when (status) {
            SubscriptionStatus.Active -> plan
            SubscriptionStatus.BillingIssue -> plan
            SubscriptionStatus.Expired,
            SubscriptionStatus.Unknown,
            -> SubscriptionPlan.Free
        }
    }
}

data class PlanOffering(
    val plan: SubscriptionPlan,
    val title: String,
    val priceLabel: String,
    val packageIdentifier: String?,
)

sealed class SubscriptionResult {
    data class Success(val snapshot: SubscriptionSnapshot) : SubscriptionResult()

    data class Failure(val error: SubscriptionError) : SubscriptionResult()
}

sealed class SubscriptionError {
    data object NotConfigured : SubscriptionError()

    data object UserCancelled : SubscriptionError()

    data object Network : SubscriptionError()

    data class Message(val userMessage: String) : SubscriptionError()
}
