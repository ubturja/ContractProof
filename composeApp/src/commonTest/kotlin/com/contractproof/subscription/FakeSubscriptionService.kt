package com.contractproof.subscription

import com.contractproof.domain.PlanOffering
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionResult
import com.contractproof.domain.SubscriptionService
import com.contractproof.domain.SubscriptionSnapshot
import com.contractproof.domain.SubscriptionSource
import com.contractproof.domain.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSubscriptionService(
    initial: SubscriptionSnapshot = SubscriptionSnapshot(
        plan = SubscriptionPlan.Pro,
        status = SubscriptionStatus.Active,
        source = SubscriptionSource.Disabled,
    ),
    private val configured: Boolean = true,
) : SubscriptionService {
    private val internal = MutableStateFlow(initial)
    override val state: StateFlow<SubscriptionSnapshot> = internal.asStateFlow()

    var restoreCalls: Int = 0
    var purchaseCalls: Int = 0
    var signedInId: String? = null

    override fun isConfigured(): Boolean = configured

    override suspend fun refresh() {
        // Tests drive [internal] directly when needed.
    }

    override suspend fun onSignedIn(organizationId: String) {
        signedInId = organizationId
    }

    override suspend fun onSignedOut() {
        signedInId = null
        internal.value = SubscriptionSnapshot(
            plan = SubscriptionPlan.Free,
            status = SubscriptionStatus.Active,
            source = SubscriptionSource.Disabled,
        )
    }

    override suspend fun restore(): SubscriptionResult {
        restoreCalls += 1
        return SubscriptionResult.Success(internal.value)
    }

    override suspend fun loadOfferings(): List<PlanOffering> {
        return listOf(
            PlanOffering(SubscriptionPlan.Pro, "Pro", "$49/mo", "pro"),
            PlanOffering(SubscriptionPlan.Business, "Business", "$99/mo", "business"),
        )
    }

    override suspend fun purchase(plan: SubscriptionPlan): SubscriptionResult {
        purchaseCalls += 1
        val snapshot = internal.value.copy(
            plan = plan,
            status = SubscriptionStatus.Active,
        )
        internal.value = snapshot
        return SubscriptionResult.Success(snapshot)
    }

    fun setSnapshot(snapshot: SubscriptionSnapshot) {
        internal.value = snapshot
    }
}
