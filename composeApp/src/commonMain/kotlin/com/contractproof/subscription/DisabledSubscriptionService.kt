package com.contractproof.subscription

import com.contractproof.domain.PlanOffering
import com.contractproof.domain.SubscriptionError
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionResult
import com.contractproof.domain.SubscriptionService
import com.contractproof.domain.SubscriptionSnapshot
import com.contractproof.domain.SubscriptionSource
import com.contractproof.domain.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisabledSubscriptionService(
    initial: SubscriptionSnapshot = SubscriptionSnapshot(
        plan = SubscriptionPlan.Free,
        status = SubscriptionStatus.Active,
        source = SubscriptionSource.Disabled,
    ),
) : SubscriptionService {
    private val internal = MutableStateFlow(initial)
    override val state: StateFlow<SubscriptionSnapshot> = internal.asStateFlow()

    override fun isConfigured(): Boolean = false

    override suspend fun refresh() {
        // No remote subscription provider.
    }

    override suspend fun onSignedIn(organizationId: String) {
        // No-op.
    }

    override suspend fun onSignedOut() {
        internal.value = SubscriptionSnapshot(
            plan = SubscriptionPlan.Free,
            status = SubscriptionStatus.Active,
            source = SubscriptionSource.Disabled,
        )
    }

    override suspend fun restore(): SubscriptionResult {
        return SubscriptionResult.Failure(SubscriptionError.NotConfigured)
    }

    override suspend fun loadOfferings(): List<PlanOffering> = emptyList()

    override suspend fun purchase(plan: SubscriptionPlan): SubscriptionResult {
        return SubscriptionResult.Failure(SubscriptionError.NotConfigured)
    }
}
