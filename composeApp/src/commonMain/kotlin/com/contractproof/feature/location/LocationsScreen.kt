package com.contractproof.feature.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules

@Composable
fun LocationsScreen(
    state: LocationsUiState,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Locations", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (state.loading && state.items.isEmpty()) {
                    CpLabeledProgress(label = "Loading locations")
                } else if (state.items.isEmpty() && state.banner != null && !state.loading) {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                    )
                } else {
                    if (state.items.isEmpty()) {
                        Text(text = "No locations yet.", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        state.items.forEach { location ->
                            LocationRow(location = location, onOpen = { onOpen(location.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationDetailScreen(
    location: LocationRecord?,
    canWrite: Boolean,
    saving: Boolean,
    banner: String?,
    name: String,
    timezone: String,
    address: String,
    zoneCode: String,
    onNameChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onZoneCodeChange: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Location", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (location == null) {
                    Text(text = "That location is not available.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    if (banner != null) {
                        Text(text = banner, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (canWrite) {
                        CpTextField(value = name, onValueChange = onNameChange, label = "Name")
                        CpTextField(value = timezone, onValueChange = onTimezoneChange, label = "Timezone")
                        CpTextField(value = address, onValueChange = onAddressChange, label = "Service address")
                        CpTextField(value = zoneCode, onValueChange = onZoneCodeChange, label = "Zone")
                        Text(
                            text = if (location.status == LocationRules.Archived) "Archived" else "Active",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        CpButton(
                            label = if (saving) "Saving location" else "Save location",
                            onClick = onSave,
                            enabled = name.trim().isNotEmpty() && timezone.trim().isNotEmpty() && !saving,
                        )
                        if (location.status == LocationRules.Active) {
                            CpButton(
                                label = "Archive location",
                                onClick = onArchive,
                                enabled = !saving,
                            )
                        } else {
                            CpButton(
                                label = "Restore location",
                                onClick = onRestore,
                                enabled = !saving,
                            )
                        }
                    } else {
                        Text(text = location.name, style = MaterialTheme.typography.titleLarge)
                        Text(text = location.timezone, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = location.address ?: "No service address",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (location.zoneCode != null) {
                            Text(text = location.zoneCode, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            text = if (location.status == LocationRules.Archived) "Archived" else "Active",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientLocationsSection(
    locations: List<LocationRecord>,
    canWrite: Boolean,
    saving: Boolean,
    canCreate: Boolean,
    draftName: String,
    draftTimezone: String,
    draftAddress: String,
    draftZoneCode: String,
    onDraftNameChange: (String) -> Unit,
    onDraftTimezoneChange: (String) -> Unit,
    onDraftAddressChange: (String) -> Unit,
    onDraftZoneCodeChange: (String) -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Text(text = "Locations", style = MaterialTheme.typography.titleMedium)
    if (locations.isEmpty()) {
        Text(text = "No locations yet.", style = MaterialTheme.typography.bodyLarge)
    } else {
        locations.forEach { location ->
            LocationRow(location = location, onOpen = { onOpen(location.id) })
        }
    }
    if (canWrite) {
        CpTextField(value = draftName, onValueChange = onDraftNameChange, label = "Name")
        CpTextField(value = draftTimezone, onValueChange = onDraftTimezoneChange, label = "Timezone")
        CpTextField(value = draftAddress, onValueChange = onDraftAddressChange, label = "Service address")
        CpTextField(value = draftZoneCode, onValueChange = onDraftZoneCodeChange, label = "Zone")
        CpButton(
            label = if (saving) "Saving location" else "Add location",
            onClick = onCreate,
            enabled = canCreate,
        )
    }
}

@Composable
private fun LocationRow(
    location: LocationRecord,
    onOpen: () -> Unit,
) {
    CpCard(modifier = Modifier.clickable(onClick = onOpen)) {
        Text(text = location.name, style = MaterialTheme.typography.titleMedium)
        Text(text = location.timezone, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (location.status == LocationRules.Archived) "Archived" else "Active",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
