package com.contractproof.feature.contract

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpCard
import com.contractproof.core.design.CpErrorState
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpStatusIndicator
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.core.platform.rememberPdfPickerLauncher
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ClientRules
import com.contractproof.domain.ContractHealthSnapshot
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRules
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules

@Composable
fun ContractsScreen(
    state: ContractsUiState,
    healthSubtitles: Map<String, String> = emptyMap(),
    clients: List<ClientRecord>,
    locations: List<LocationRecord>,
    clientName: (String) -> String,
    locationName: (String) -> String,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Contracts", onBack = onBack)
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
                    CpLabeledProgress(label = "Loading contracts")
                } else if (state.items.isEmpty() && state.banner != null && !state.loading) {
                    CpErrorState(
                        message = state.banner,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                    )
                } else {
                    if (state.canWrite) {
                        CpButton(label = "Add contract", onClick = onCreate)
                    }
                    if (state.items.isEmpty()) {
                        Text(text = "No contracts yet.", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        state.items.forEach { contract ->
                            ContractRow(
                                contract = contract,
                                clientName = clientName(contract.clientId),
                                locationName = locationName(contract.locationId),
                                healthSubtitle = healthSubtitles[contract.id],
                                onOpen = { onOpen(contract.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContractCreateScreen(
    state: ContractsUiState,
    clients: List<ClientRecord>,
    locations: List<LocationRecord>,
    onTitleChange: (String) -> Unit,
    onClientChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onStartsOnChange: (String) -> Unit,
    onEndsOnChange: (String) -> Unit,
    onEffectiveOnChange: (String) -> Unit,
    onDocumentPicked: (String, ByteArray) -> Unit,
    onClearDocument: () -> Unit,
    onCreate: () -> Unit,
    onRetryUpload: () -> Unit,
    onBack: () -> Unit,
) {
    val pickPdf = rememberPdfPickerLauncher { picked ->
        if (picked != null) {
            onDocumentPicked(picked.fileName, picked.bytes)
        }
    }
    val clientChoices = clients.filter { it.status == ClientRules.Active }
    val locationChoices = LocationRules.forClient(locations, state.draftClientId)
        .filter { it.status == LocationRules.Active }
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "New contract", onBack = onBack)
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
                CpTextField(
                    value = state.draftTitle,
                    onValueChange = onTitleChange,
                    label = "Title",
                )
                Text(text = "Client", style = MaterialTheme.typography.titleMedium)
                if (clientChoices.isEmpty()) {
                    Text(text = "Add a client before creating a contract.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    clientChoices.forEach { client ->
                        ChoiceRow(
                            title = client.name,
                            selected = client.id == state.draftClientId,
                            onClick = { onClientChange(client.id) },
                        )
                    }
                }
                Text(text = "Location", style = MaterialTheme.typography.titleMedium)
                if (state.draftClientId.isEmpty()) {
                    Text(text = "Choose a client to see locations.", style = MaterialTheme.typography.bodyLarge)
                } else if (locationChoices.isEmpty()) {
                    Text(text = "Add a location for this client.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    locationChoices.forEach { location ->
                        ChoiceRow(
                            title = location.name,
                            selected = location.id == state.draftLocationId,
                            onClick = { onLocationChange(location.id) },
                        )
                    }
                }
                CpTextField(
                    value = state.draftStartsOn,
                    onValueChange = onStartsOnChange,
                    label = "Start date (YYYY-MM-DD)",
                )
                CpTextField(
                    value = state.draftEndsOn,
                    onValueChange = onEndsOnChange,
                    label = "End date (optional)",
                )
                CpTextField(
                    value = state.draftEffectiveOn,
                    onValueChange = onEffectiveOnChange,
                    label = "Document effective date (YYYY-MM-DD)",
                )
                Text(
                    text = state.draftFileName ?: "No PDF selected. A PDF is optional.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.uploadStatus?.let { status ->
                    CpStatusIndicator(status = status)
                }
                if (state.saving && state.uploadPercent != null) {
                    CpLabeledProgress(
                        label = "Uploading PDF ${state.uploadPercent}%",
                        progress = state.uploadPercent / 100f,
                    )
                }
                CpButton(
                    label = "Choose PDF",
                    onClick = pickPdf,
                    style = CpButtonStyle.Secondary,
                    enabled = !state.saving,
                )
                if (state.draftFileName != null && !state.canRetryUpload) {
                    CpButton(
                        label = "Remove PDF",
                        onClick = onClearDocument,
                        style = CpButtonStyle.Secondary,
                        enabled = !state.saving,
                    )
                }
                if (state.canRetryUpload) {
                    CpButton(
                        label = "Retry upload",
                        onClick = onRetryUpload,
                    )
                } else {
                    CpButton(
                        label = when {
                            state.uploadStatus == CpWorkStatus.Uploading -> "Uploading PDF"
                            state.saving -> "Saving contract"
                            else -> "Save contract"
                        },
                        onClick = onCreate,
                        enabled = state.canCreate,
                    )
                }
            }
        }
    }
}

@Composable
fun ContractDetailScreen(
    contract: ContractRecord?,
    health: ContractHealthSnapshot? = null,
    healthLoading: Boolean = false,
    healthBanner: String? = null,
    versions: List<ContractVersionRecord>,
    currentUserId: String,
    clientName: String,
    locationName: String,
    canWrite: Boolean,
    saving: Boolean,
    viewing: Boolean,
    banner: String?,
    title: String,
    startsOn: String,
    endsOn: String,
    onTitleChange: (String) -> Unit,
    onStartsOnChange: (String) -> Unit,
    onEndsOnChange: (String) -> Unit,
    onSave: () -> Unit,
    onEnd: () -> Unit,
    onRestoreDraft: () -> Unit,
    onOpenVersion: (String) -> Unit,
    onPickDocument: () -> Unit,
    onAttachDocument: () -> Unit,
    onRetryUpload: () -> Unit,
    onActivate: (String) -> Unit,
    onEditRequirements: (String) -> Unit,
    onOpenExtractionReview: (String, Boolean) -> Unit,
    onEffectiveOnChange: (String) -> Unit,
    uploadStatus: CpWorkStatus?,
    uploadPercent: Int?,
    draftFileName: String?,
    draftEffectiveOn: String,
    canRetryUpload: Boolean,
    canUploadNextVersion: Boolean,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Contract", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (contract == null) {
                    Text(text = "That contract is not available.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    if (banner != null) {
                        Text(text = banner, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(text = clientName, style = MaterialTheme.typography.bodyLarge)
                    Text(text = locationName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = ContractRules.statusLabel(contract.status),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ContractHealthCard(
                        contract = contract,
                        health = health,
                        loading = healthLoading,
                        banner = healthBanner,
                    )
                    if (canWrite && contract.status != ContractRules.Active) {
                        CpTextField(value = title, onValueChange = onTitleChange, label = "Title")
                        CpTextField(
                            value = startsOn,
                            onValueChange = onStartsOnChange,
                            label = "Start date (YYYY-MM-DD)",
                        )
                        CpTextField(
                            value = endsOn,
                            onValueChange = onEndsOnChange,
                            label = "End date (optional)",
                        )
                        CpButton(
                            label = if (saving) "Saving contract" else "Save contract",
                            onClick = onSave,
                            enabled = title.trim().isNotEmpty() && startsOn.trim().isNotEmpty() && !saving,
                        )
                        if (contract.status == ContractRules.Draft) {
                            CpButton(
                                label = "End contract",
                                onClick = onEnd,
                                enabled = !saving,
                                style = CpButtonStyle.Secondary,
                            )
                        } else if (contract.status == ContractRules.Ended) {
                            CpButton(
                                label = "Return to draft",
                                onClick = onRestoreDraft,
                                enabled = !saving,
                                style = CpButtonStyle.Secondary,
                            )
                        }
                    } else {
                        Text(text = contract.title, style = MaterialTheme.typography.titleLarge)
                        Text(text = contract.startsOn, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = contract.endsOn ?: "No end date",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Text(
                        text = documentSummary(contract),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(text = "Versions", style = MaterialTheme.typography.titleMedium)
                    if (versions.isEmpty()) {
                        Text(text = "No versions yet.", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        versions.forEach { version ->
                            VersionRow(
                                version = version,
                                currentVersionId = contract.currentVersionId,
                                createdByLabel = ContractVersionRules.createdByLabel(
                                    version.createdBy,
                                    currentUserId,
                                ),
                                canWrite = canWrite,
                                saving = saving,
                                viewing = viewing,
                                onOpen = {
                                    version.documentPath?.let(onOpenVersion)
                                },
                                onActivate = { onActivate(version.id) },
                                onEditRequirements = { onEditRequirements(version.id) },
                                onOpenExtractionReview = { autoExtract ->
                                    onOpenExtractionReview(version.id, autoExtract)
                                },
                            )
                        }
                    }
                    uploadStatus?.let { status ->
                        CpStatusIndicator(status = status)
                    }
                    if (saving && uploadPercent != null) {
                        CpLabeledProgress(
                            label = "Uploading PDF $uploadPercent%",
                            progress = uploadPercent / 100f,
                        )
                    }
                    if (canWrite) {
                        if (ContractVersionRules.hasInReview(versions)) {
                            Text(
                                text = "Activate the current upload before adding another version.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            if (draftFileName != null) {
                                Text(text = draftFileName, style = MaterialTheme.typography.bodyLarge)
                            }
                            CpTextField(
                                value = draftEffectiveOn,
                                onValueChange = onEffectiveOnChange,
                                label = "Effective date (YYYY-MM-DD)",
                            )
                            CpButton(
                                label = "Choose PDF",
                                onClick = onPickDocument,
                                enabled = !saving,
                                style = CpButtonStyle.Secondary,
                            )
                            if (canRetryUpload) {
                                CpButton(
                                    label = "Retry upload",
                                    onClick = onRetryUpload,
                                    enabled = !saving,
                                )
                            } else {
                                CpButton(
                                    label = if (saving) "Uploading PDF" else "Upload next version",
                                    onClick = onAttachDocument,
                                    enabled = canUploadNextVersion,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionRow(
    version: ContractVersionRecord,
    currentVersionId: String?,
    createdByLabel: String,
    canWrite: Boolean,
    saving: Boolean,
    viewing: Boolean,
    onOpen: () -> Unit,
    onActivate: () -> Unit,
    onEditRequirements: (String) -> Unit,
    onOpenExtractionReview: (Boolean) -> Unit,
) {
    CpCard {
        Text(
            text = "Version ${version.versionNumber}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = ContractVersionRules.statusLabel(version, currentVersionId),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(text = "Effective ${version.effectiveOn}", style = MaterialTheme.typography.bodyLarge)
        Text(text = createdByLabel, style = MaterialTheme.typography.bodyLarge)
        val name = version.documentFileName
        val size = version.documentByteSize?.let { ContractRules.byteSizeLabel(it) }
        Text(
            text = when {
                name != null && size != null -> "$name · $size"
                name != null -> name
                else -> "No document"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!version.documentPath.isNullOrEmpty()) {
            CpButton(
                label = if (viewing) "Opening document" else "View document",
                onClick = onOpen,
                enabled = !viewing,
                style = CpButtonStyle.Secondary,
            )
        }
        if (canWrite && ContractVersionRules.isInReview(version)) {
            val hasPdf = !version.documentPath.isNullOrEmpty()
            if (hasPdf) {
                val label = when (version.status) {
                    ContractVersionRules.Extracted -> "Review AI requirements"
                    else -> "Read from contract"
                }
                CpButton(
                    label = label,
                    onClick = { onOpenExtractionReview(version.status == ContractVersionRules.Uploaded) },
                    enabled = !saving,
                )
            }
            CpButton(
                label = "Edit requirements manually",
                onClick = { onEditRequirements(version.id) },
                enabled = !saving,
                style = CpButtonStyle.Secondary,
            )
            if (!hasPdf) {
                CpButton(
                    label = if (saving) "Activating version" else "Activate version",
                    onClick = onActivate,
                    enabled = !saving,
                )
            }
        }
    }
}

@Composable
private fun ContractHealthCard(
    contract: ContractRecord,
    health: ContractHealthSnapshot?,
    loading: Boolean,
    banner: String?,
) {
    CpCard {
        Text(text = "Contract health", style = MaterialTheme.typography.titleMedium)
        when {
            contract.status != ContractRules.Active -> {
                Text(
                    text = when (contract.status) {
                        ContractRules.Draft -> "Health indicators apply after the contract is active."
                        ContractRules.Ended -> "This contract has ended. Health reflects historical context only."
                        else -> "Status: ${ContractRules.statusLabel(contract.status)}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            loading && health == null -> {
                CpLabeledProgress(label = "Loading contract health")
            }
            banner != null && health == null -> {
                Text(text = banner, style = MaterialTheme.typography.bodyLarge)
            }
            health != null -> {
                Text(
                    text = "Status: ${health.statusLabel}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = "Upcoming services: ${health.upcomingCount}", style = MaterialTheme.typography.bodyLarge)
                health.nextServiceDate?.let { next ->
                    Text(text = "Next service: $next", style = MaterialTheme.typography.bodyLarge)
                }
                Text(text = health.coverageLine, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Open disputes: ${health.openDisputeCount}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Unresolved exceptions: ${health.exceptionCount}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun ContractRow(
    contract: ContractRecord,
    clientName: String,
    locationName: String,
    healthSubtitle: String?,
    onOpen: () -> Unit,
) {
    val description = "${contract.title}, $clientName, $locationName"
    CpCard(
        modifier = Modifier
            .clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) {
                role = androidx.compose.ui.semantics.Role.Button
                contentDescription = description
            },
    ) {
        Text(text = contract.title, style = MaterialTheme.typography.titleMedium)
        Text(text = clientName, style = MaterialTheme.typography.bodyLarge)
        Text(text = locationName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = ContractRules.statusLabel(contract.status),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (contract.status == ContractRules.Active && !healthSubtitle.isNullOrBlank()) {
            Text(
                text = healthSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (ContractRules.hasDocument(contract)) "Document uploaded" else "No document",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CpCard(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = if (selected) "$title (selected)" else title,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun documentSummary(contract: ContractRecord): String {
    if (!ContractRules.hasDocument(contract)) {
        return "No document"
    }
    val name = contract.documentFileName
    val size = contract.documentByteSize?.let { ContractRules.byteSizeLabel(it) }
    return when {
        name != null && size != null -> "Document uploaded · $name · $size"
        name != null -> "Document uploaded · $name"
        else -> "Document uploaded"
    }
}
