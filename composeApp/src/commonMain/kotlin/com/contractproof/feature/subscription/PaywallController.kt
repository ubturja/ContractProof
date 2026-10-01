package com.contractproof.feature.subscription

import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.domain.PlanOffering
import com.contractproof.domain.SubscriptionError
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionResult
import com.contractproof.domain.SubscriptionService
import com.contractproof.domain.SubscriptionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PaywallPhase {
    Loading,
    Ready,
    ProcessingPurchase,
    Restoring,
}

data class PaywallUiState(
    val phase: PaywallPhase = PaywallPhase.Loading,
    val snapshot: SubscriptionSnapshot? = null,
    val offerings: List<PlanOffering> = emptyList(),
    val banner: String? = null,
    val configured: Boolean = false,
) {
    val currentPlanLabel: String
        get() = when (snapshot?.effectivePlan()) {
            SubscriptionPlan.Business -> "Business"
            SubscriptionPlan.Pro -> "Pro"
            SubscriptionPlan.Free,
            null,
            -> "Free"
        }
}

class PaywallController(
    private val subscription: SubscriptionService,
    private val analytics: ProductAnalytics,
) {
    private val ui = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = ui.asStateFlow()

    suspend fun load() {
        ui.update {
            it.copy(
                phase = PaywallPhase.Loading,
                banner = null,
                configured = subscription.isConfigured(),
                snapshot = subscription.state.value,
            )
        }
        try {
            subscription.refresh()
            val offerings = subscription.loadOfferings()
            ui.update {
                it.copy(
                    phase = PaywallPhase.Ready,
                    offerings = offerings,
                    snapshot = subscription.state.value,
                    configured = subscription.isConfigured(),
                )
            }
            analytics.track(ProductEvent.PaywallViewed)
        } catch (_: Throwable) {
            ui.update {
                it.copy(
                    phase = PaywallPhase.Ready,
                    banner = "Plans could not be loaded. You can still restore a purchase.",
                    snapshot = subscription.state.value,
                )
            }
        }
    }

    suspend fun purchase(plan: SubscriptionPlan) {
        if (!subscription.isConfigured()) {
            ui.update { it.copy(banner = "Purchases are not available in this build.") }
            return
        }
        ui.update { it.copy(phase = PaywallPhase.ProcessingPurchase, banner = null) }
        when (val result = subscription.purchase(plan)) {
            is SubscriptionResult.Success -> {
                val effective = result.snapshot.effectivePlan()
                if (effective == SubscriptionPlan.Pro || effective == SubscriptionPlan.Business) {
                    analytics.track(ProductEvent.SubscriptionStarted(effective.name.lowercase()))
                }
                ui.update {
                    it.copy(
                        phase = PaywallPhase.Ready,
                        snapshot = result.snapshot,
                        banner = "Your plan is now ${planLabel(effective)}.",
                    )
                }
            }
            is SubscriptionResult.Failure -> {
                ui.update {
                    it.copy(
                        phase = PaywallPhase.Ready,
                        banner = failureMessage(result.error),
                    )
                }
            }
        }
    }

    suspend fun restore() {
        if (!subscription.isConfigured()) {
            ui.update { it.copy(banner = "Restore is not available in this build.") }
            return
        }
        ui.update { it.copy(phase = PaywallPhase.Restoring, banner = null) }
        when (val result = subscription.restore()) {
            is SubscriptionResult.Success -> {
                ui.update {
                    it.copy(
                        phase = PaywallPhase.Ready,
                        snapshot = result.snapshot,
                        banner = "Purchases restored. Current plan: ${planLabel(result.snapshot.effectivePlan())}.",
                    )
                }
            }
            is SubscriptionResult.Failure -> {
                ui.update {
                    it.copy(
                        phase = PaywallPhase.Ready,
                        banner = failureMessage(result.error),
                    )
                }
            }
        }
    }

    private fun failureMessage(error: SubscriptionError): String {
        return when (error) {
            SubscriptionError.NotConfigured -> "Purchases are not configured on this device."
            SubscriptionError.UserCancelled -> "Purchase cancelled."
            SubscriptionError.Network -> "You need a connection to complete this."
            is SubscriptionError.Message -> error.userMessage
        }
    }

    private fun planLabel(plan: SubscriptionPlan): String {
        return when (plan) {
            SubscriptionPlan.Pro -> "Pro"
            SubscriptionPlan.Business -> "Business"
            SubscriptionPlan.Free -> "Free"
        }
    }
}
