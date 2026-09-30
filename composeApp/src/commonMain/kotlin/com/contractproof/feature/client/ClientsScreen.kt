package com.contractproof.feature.client

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
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ClientRules
import com.contractproof.domain.LocationRecord
import com.contractproof.feature.location.ClientLocationsSection

@Composable
fun ClientsScreen(
    state: ClientsUiState,
    onQueryChange: (String) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Clients", onBack = onBack)
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
                    CpLabeledProgress(label = "Loading clients")
                } else if (state.items.isEmpty() && state.banner != null && !state.loading) {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                    )
                } else {
                    CpTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        label = "Search",
                    )
                    if (state.canWrite) {
                        CpTextField(
                            value = state.draftName,
                            onValueChange = onDraftNameChange,
                            label = "New client",
                        )
                        CpButton(
                            label = if (state.saving) "Saving client" else "Add client",
                            onClick = onCreate,
                            enabled = state.canCreate,
                        )
                    }
                    if (state.visible.isEmpty()) {
                        Text(
                            text = if (state.items.isEmpty()) "No clients yet." else "No matching clients.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        state.visible.forEach { client ->
                            ClientRow(client = client, onOpen = { onOpen(client.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientDetailScreen(
    client: ClientRecord?,
    canWrite: Boolean,
    saving: Boolean,
    banner: String?,
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    locations: List<LocationRecord>,
    canWriteLocations: Boolean,
    locationSaving: Boolean,
    canCreateLocation: Boolean,
    draftLocationName: String,
    draftTimezone: String,
    draftAddress: String,
    draftZoneCode: String,
    onDraftLocationNameChange: (String) -> Unit,
    onDraftTimezoneChange: (String) -> Unit,
    onDraftAddressChange: (String) -> Unit,
    onDraftZoneCodeChange: (String) -> Unit,
    onCreateLocation: () -> Unit,
    onOpenLocation: (String) -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Client", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (client == null) {
                    Text(text = "That client is not available.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    if (banner != null) {
                        Text(text = banner, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (canWrite) {
                        CpTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = "Name",
                        )
                        Text(
                            text = if (client.status == ClientRules.Archived) "Archived" else "Active",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        CpButton(
                            label = if (saving) "Saving client" else "Save client",
                            onClick = onSave,
                            enabled = name.trim().isNotEmpty() && !saving,
                        )
                        if (client.status == ClientRules.Active) {
                            CpButton(
                                label = "Archive client",
                                onClick = onArchive,
                                enabled = !saving,
                            )
                        } else {
                            CpButton(
                                label = "Restore client",
                                onClick = onRestore,
                                enabled = !saving,
                            )
                        }
                    } else {
                        Text(text = client.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (client.status == ClientRules.Archived) "Archived" else "Active",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    ClientLocationsSection(
                        locations = locations,
                        canWrite = canWriteLocations,
                        saving = locationSaving,
                        canCreate = canCreateLocation,
                        draftName = draftLocationName,
                        draftTimezone = draftTimezone,
                        draftAddress = draftAddress,
                        draftZoneCode = draftZoneCode,
                        onDraftNameChange = onDraftLocationNameChange,
                        onDraftTimezoneChange = onDraftTimezoneChange,
                        onDraftAddressChange = onDraftAddressChange,
                        onDraftZoneCodeChange = onDraftZoneCodeChange,
                        onCreate = onCreateLocation,
                        onOpen = onOpenLocation,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientRow(
    client: ClientRecord,
    onOpen: () -> Unit,
) {
    CpCard(modifier = Modifier.clickable(onClick = onOpen)) {
        Text(text = client.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (client.status == ClientRules.Archived) "Archived" else "Active",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
