package com.contractproof.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.PlanOffering
import com.contractproof.domain.SubscriptionPlan

@Composable
fun PaywallScreen(
    state: PaywallUiState,
    onPurchase: (SubscriptionPlan) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(PaywallTestTags.Screen),
        ) {
            CpTitleBar(title = "Plans", onBack = onBack)
            when (state.phase) {
                PaywallPhase.Loading -> CpLabeledProgress(
                    label = "Loading plans…",
                    modifier = Modifier.padding(CpSpacing.md),
                )
                PaywallPhase.ProcessingPurchase -> CpLabeledProgress(
                    label = "Processing purchase…",
                    modifier = Modifier.padding(CpSpacing.md),
                )
                PaywallPhase.Restoring -> CpLabeledProgress(
                    label = "Restoring purchases…",
                    modifier = Modifier.padding(CpSpacing.md),
                )
                PaywallPhase.Ready -> PaywallContent(
                    state = state,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun PaywallContent(
    state: PaywallUiState,
    onPurchase: (SubscriptionPlan) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(CpSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
    ) {
        Text(
            text = "Current plan: ${state.currentPlanLabel}",
            style = MaterialTheme.typography.titleMedium,
        )
        state.banner?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        PlanCard(
            title = "Free",
            price = "Included",
            bullets = listOf(
                "One active location",
                "Core service workflow and evidence capture",
                "No AI contract extraction",
                "No dispute evidence PDF reports",
            ),
            actionLabel = null,
            enabled = false,
            onAction = {},
        )
        val pro = state.offerings.find { it.plan == SubscriptionPlan.Pro }
        PlanCard(
            title = "Pro",
            price = pro?.priceLabel ?: "Test Store · $49/mo",
            bullets = listOf(
                "Multiple locations",
                "AI contract extraction and requirement review",
                "Evidence validation and dispute evidence reports",
                "Client portal when available",
            ),
            actionLabel = if (state.snapshot?.effectivePlan() == SubscriptionPlan.Pro) "Current plan" else "Upgrade to Pro",
            enabled = state.configured && state.snapshot?.effectivePlan() != SubscriptionPlan.Pro,
            onAction = { onPurchase(SubscriptionPlan.Pro) },
        )
        val business = state.offerings.find { it.plan == SubscriptionPlan.Business }
        PlanCard(
            title = "Business",
            price = business?.priceLabel ?: "Test Store · $99/mo",
            bullets = listOf(
                "Everything in Pro",
                "Multiple managers",
                "Branded reports and expanded analytics scope",
            ),
            actionLabel = if (state.snapshot?.effectivePlan() == SubscriptionPlan.Business) {
                "Current plan"
            } else {
                "Upgrade to Business"
            },
            enabled = state.configured && state.snapshot?.effectivePlan() != SubscriptionPlan.Business,
            onAction = { onPurchase(SubscriptionPlan.Business) },
        )
        if (!state.configured) {
            Text(
                text = "Purchases use the RevenueCat Test Store when configured. This build has no billing key.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        CpButton(
            label = "Restore purchases",
            onClick = onRestore,
            style = CpButtonStyle.Secondary,
            enabled = state.configured,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PaywallTestTags.Restore),
        )
        if (state.offerings.isEmpty() && state.configured) {
            CpButton(
                label = "Retry loading plans",
                onClick = onRetry,
                style = CpButtonStyle.Secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PaywallTestTags.Retry),
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    bullets: List<String>,
    actionLabel: String?,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    CpCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(CpSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CpSpacing.sm),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = price, style = MaterialTheme.typography.bodyLarge)
            bullets.forEach { line ->
                Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null) {
                CpButton(
                    label = actionLabel,
                    onClick = onAction,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
