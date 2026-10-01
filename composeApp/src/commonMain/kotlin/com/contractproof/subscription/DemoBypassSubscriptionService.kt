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

/**
 * Recording / demo builds only — never enable for production judge builds by default.
 */
class DemoBypassSubscriptionService(
    private val delegate: SubscriptionService,
) : SubscriptionService {
    private val demoSnapshot = SubscriptionSnapshot(
        plan = SubscriptionPlan.Pro,
        status = SubscriptionStatus.Active,
        source = SubscriptionSource.DemoBypass,
    )

    private val internal = MutableStateFlow(demoSnapshot)
    override val state: StateFlow<SubscriptionSnapshot> = internal.asStateFlow()

    override fun isConfigured(): Boolean = delegate.isConfigured()

    override suspend fun refresh() {
        delegate.refresh()
        internal.value = demoSnapshot
    }

    override suspend fun onSignedIn(organizationId: String) {
        delegate.onSignedIn(organizationId)
        internal.value = demoSnapshot
    }

    override suspend fun onSignedOut() {
        delegate.onSignedOut()
        internal.value = demoSnapshot
    }

    override suspend fun restore(): SubscriptionResult {
        internal.value = demoSnapshot
        return SubscriptionResult.Success(demoSnapshot)
    }

    override suspend fun loadOfferings(): List<PlanOffering> = delegate.loadOfferings()

    override suspend fun purchase(plan: SubscriptionPlan): SubscriptionResult {
        internal.value = when (plan) {
            SubscriptionPlan.Business -> demoSnapshot.copy(plan = SubscriptionPlan.Business)
            SubscriptionPlan.Pro -> demoSnapshot
            SubscriptionPlan.Free -> demoSnapshot.copy(plan = SubscriptionPlan.Free)
        }
        return SubscriptionResult.Success(internal.value)
    }
}
