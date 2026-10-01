package com.contractproof.feature.settings

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
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

object SettingsTestTags {
    const val screen = "settings_screen"
    const val retry = "settings_retry"
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    canManageSubscription: Boolean,
    currentPlanLabel: String,
    onOpenSubscription: () -> Unit,
    showNotificationPermission: Boolean = false,
    onRequestNotificationPermission: () -> Unit = {},
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SettingsTestTags.screen),
        ) {
            CpTitleBar(title = "Settings", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.loading && state.organizationName.isEmpty() && state.email.isEmpty()) {
                    CpLabeledProgress(label = "Loading settings")
                } else if (state.banner != null && state.organizationName.isEmpty() && !state.loading) {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                        modifier = Modifier.testTag(SettingsTestTags.retry),
                    )
                } else {
                    CpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(CpSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(CpSpacing.sm),
                        ) {
                            Text(text = "Account", style = MaterialTheme.typography.titleMedium)
                            if (state.email.isNotEmpty()) {
                                Text(text = state.email, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (state.organizationName.isNotEmpty()) {
                                Text(text = state.organizationName, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (state.roleLabel.isNotEmpty()) {
                                Text(
                                    text = state.roleLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    CpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(CpSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(CpSpacing.sm),
                        ) {
                            Text(text = "Sync", style = MaterialTheme.typography.titleMedium)
                            Text(text = state.syncSummaryLine, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (canManageSubscription) {
                    CpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(CpSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(CpSpacing.sm),
                        ) {
                            CpButton(
                                label = "Subscription · $currentPlanLabel",
                                onClick = onOpenSubscription,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (showNotificationPermission) {
                    CpButton(
                        label = "Enable notifications",
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CpButton(
                    label = "Sign out",
                    onClick = onSignOut,
                    style = CpButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
