package com.contractproof.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

@Composable
fun DashboardScreen(
    organizationName: String,
    canAddLocation: Boolean,
    canOpenLocations: Boolean,
    canOpenService: Boolean,
    canOpenContracts: Boolean,
    canOpenDisputes: Boolean,
    canOpenClients: Boolean,
    onOpenLocations: () -> Unit,
    onOpenService: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenDisputes: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Dashboard")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                Text(text = organizationName, style = MaterialTheme.typography.titleLarge)
                CpCard {
                    Text(
                        text = "No locations yet.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (canAddLocation) {
                        Text(
                            text = "Add a location to start scheduling work.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (canOpenLocations) {
                    CpButton(label = "Open locations", onClick = onOpenLocations)
                }
                CpButton(
                    label = "Open settings",
                    onClick = onOpenSettings,
                    style = CpButtonStyle.Secondary,
                )
                if (canOpenClients) {
                    CpButton(
                        label = "Open clients",
                        onClick = onOpenClients,
                        style = CpButtonStyle.Secondary,
                    )
                }
                if (canOpenService) {
                    CpButton(
                        label = "Open service",
                        onClick = onOpenService,
                        style = CpButtonStyle.Secondary,
                    )
                }
                if (canOpenContracts) {
                    CpButton(
                        label = "Open contracts",
                        onClick = onOpenContracts,
                        style = CpButtonStyle.Secondary,
                    )
                }
                if (canOpenDisputes) {
                    CpButton(
                        label = "Open disputes",
                        onClick = onOpenDisputes,
                        style = CpButtonStyle.Secondary,
                    )
                }
            }
        }
    }
}
