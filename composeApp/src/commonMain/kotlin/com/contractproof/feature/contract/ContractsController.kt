package com.contractproof.feature.contract

import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.core.observability.ErrorLevel
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.data.ContractFailure
import com.contractproof.data.ContractGateway
import com.contractproof.data.NewContractDocument
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRuleViolation
import com.contractproof.domain.ContractRules
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.LocationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ContractsUiState(
    val items: List<ContractRecord> = emptyList(),
    val versions: List<ContractVersionRecord> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
    val currentUserId: String = "",
    val draftTitle: String = "",
    val draftClientId: String = "",
    val draftLocationId: String = "",
    val draftStartsOn: String = "",
    val draftEndsOn: String = "",
    val draftEffectiveOn: String = "",
    val draftFileName: String? = null,
    val viewing: Boolean = false,
    val uploadPercent: Int? = null,
    val uploadStatus: CpWorkStatus? = null,
) {
    val canCreate: Boolean
        get() = canWrite &&
            draftTitle.trim().isNotEmpty() &&
            draftClientId.isNotEmpty() &&
            draftLocationId.isNotEmpty() &&
            draftStartsOn.trim().isNotEmpty() &&
            !loading &&
            !saving

    val canRetryUpload: Boolean
        get() = canWrite &&
            uploadStatus == CpWorkStatus.Failed &&
            draftFileName != null &&
            !saving

    val canUploadNextVersion: Boolean
        get() = canWrite &&
            draftFileName != null &&
            draftEffectiveOn.trim().isNotEmpty() &&
            !ContractVersionRules.hasInReview(versions) &&
            !saving
}

class ContractsController(
    private val organizations: OrganizationGateway,
    private val contracts: ContractGateway,
    private val analytics: ProductAnalytics,
    private val errorReporter: ErrorReporter,
) {
    private val ui = MutableStateFlow(ContractsUiState())
    val state: StateFlow<ContractsUiState> = ui.asStateFlow()

    private var pendingDocument: NewContractDocument? = null
    private var pendingContractId: String? = null

    fun updateDraftTitle(value: String) {
        if (pendingContractId != null) {
            return
        }
        ui.update { it.copy(draftTitle = value) }
    }

    fun updateDraftClient(id: String) {
        if (pendingContractId != null) {
            return
        }
        ui.update { latest ->
            latest.copy(
                draftClientId = id,
                draftLocationId = if (latest.draftClientId == id) latest.draftLocationId else "",
            )
        }
    }

    fun updateDraftLocation(id: String) {
        if (pendingContractId != null) {
            return
        }
        ui.update { it.copy(draftLocationId = id) }
    }

    fun updateDraftStartsOn(value: String) {
        if (pendingContractId != null) {
            return
        }
        ui.update { it.copy(draftStartsOn = value) }
    }

    fun updateDraftEndsOn(value: String) {
        if (pendingContractId != null) {
            return
        }
        ui.update { it.copy(draftEndsOn = value) }
    }

    fun updateDraftEffectiveOn(value: String) {
        ui.update { it.copy(draftEffectiveOn = value) }
    }

    fun updateDraftDocument(fileName: String, bytes: ByteArray) {
        try {
            val name = ContractRules.requirePdfFileName(fileName)
            val pdf = ContractRules.requirePdfBytes(bytes)
            pendingDocument = NewContractDocument(fileName = name, bytes = pdf)
            ui.update {
                it.copy(
                    draftFileName = name,
                    banner = null,
                    uploadStatus = CpWorkStatus.Pending,
                    uploadPercent = null,
                )
            }
        } catch (error: ContractRuleViolation) {
            pendingDocument = null
            ui.update {
                it.copy(
                    draftFileName = null,
                    uploadStatus = CpWorkStatus.Failed,
                    uploadPercent = null,
                    banner = error.message,
                )
            }
        }
    }

    fun clearDraftDocument() {
        if (pendingContractId != null) {
            return
        }
        pendingDocument = null
        ui.update {
            it.copy(
                draftFileName = null,
                uploadStatus = null,
                uploadPercent = null,
            )
        }
    }

    suspend fun refresh() {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenContracts) {
            ui.update {
                it.copy(
                    loading = false,
                    canWrite = false,
                    currentUserId = "",
                    banner = "Contracts are not available.",
                    items = emptyList(),
                    versions = emptyList(),
                )
            }
            return
        }
        ui.update {
            it.copy(
                loading = true,
                banner = null,
                canWrite = access.canWriteContracts,
                currentUserId = membership?.userId.orEmpty(),
            )
        }
        try {
            val items = contracts.list()
            ui.update { it.copy(loading = false, items = items, banner = null) }
        } catch (failure: ContractFailure) {
            ui.update { latest ->
                latest.copy(
                    loading = false,
                    banner = when (failure) {
                        ContractFailure.Network ->
                            if (latest.items.isEmpty()) {
                                "You need a connection to load contracts."
                            } else {
                                "You are offline. Showing the last loaded contracts."
                            }
                        ContractFailure.Rejected -> "Contracts could not be loaded."
                        ContractFailure.Upload -> "The document could not be uploaded."
                    },
                )
            }
        }
    }

    suspend fun refreshVersions(contractId: String) {
        try {
            val versions = contracts.listVersions(contractId)
            ui.update { it.copy(versions = versions, banner = null) }
        } catch (failure: ContractFailure) {
            ui.update { latest ->
                latest.copy(
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to load versions."
                        ContractFailure.Rejected -> "Contract versions could not be loaded."
                        ContractFailure.Upload -> "The document could not be uploaded."
                    },
                )
            }
        }
    }

    suspend fun create(location: LocationRecord): ContractRecord? {
        val current = ui.value
        if (!current.canWrite || current.saving) {
            return null
        }
        val existingId = pendingContractId
        if (existingId == null && !current.canCreate) {
            return null
        }
        ui.update {
            it.copy(
                saving = true,
                banner = null,
                uploadStatus = if (pendingDocument != null) CpWorkStatus.Uploading else it.uploadStatus,
                uploadPercent = if (pendingDocument != null) 0 else it.uploadPercent,
            )
        }
        return try {
            val created = if (existingId == null) {
                val inserted = contracts.create(
                    clientId = current.draftClientId,
                    location = location,
                    title = current.draftTitle,
                    startsOn = current.draftStartsOn,
                    endsOn = current.draftEndsOn,
                )
                pendingContractId = inserted.id
                ui.update { latest ->
                    latest.copy(items = latest.items.filterNot { it.id == inserted.id } + inserted)
                }
                inserted
            } else {
                ui.value.items.find { it.id == existingId } ?: contracts.list().find { it.id == existingId }
                    ?: throw ContractFailure.Rejected
            }
            val document = pendingDocument
            val saved = if (document == null) {
                created
            } else {
                ui.update { it.copy(uploadStatus = CpWorkStatus.Uploading) }
                val effective = current.draftEffectiveOn.trim().ifEmpty { current.draftStartsOn }
                contracts.attachDocument(created.id, document, effective) { percent ->
                    ui.update { latest ->
                        latest.copy(
                            uploadPercent = percent,
                            uploadStatus = if (percent >= 100) CpWorkStatus.Uploaded else CpWorkStatus.Uploading,
                        )
                    }
                }
            }
            pendingDocument = null
            pendingContractId = null
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    draftTitle = "",
                    draftClientId = "",
                    draftLocationId = "",
                    draftStartsOn = "",
                    draftEndsOn = "",
                    draftEffectiveOn = "",
                    draftFileName = null,
                    uploadPercent = null,
                    uploadStatus = if (ContractRules.hasDocument(saved)) CpWorkStatus.Uploaded else null,
                    items = latest.items.map { contract ->
                        if (contract.id == saved.id) saved else contract
                    }.let { rows ->
                        if (rows.any { it.id == saved.id }) rows else rows + saved
                    },
                    banner = null,
                )
            }
            if (ContractRules.hasDocument(saved)) {
                analytics.track(ProductEvent.ContractUploaded(saved.id))
            }
            saved
        } catch (failure: ContractFailure) {
            reportContractFailure(failure)
            val retrying = pendingContractId != null && pendingDocument != null
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    uploadStatus = if (retrying || pendingDocument != null) CpWorkStatus.Failed else latest.uploadStatus,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to save this contract."
                        ContractFailure.Rejected -> "The contract was not saved."
                        ContractFailure.Upload -> "The document could not be uploaded. Retry keeps this contract and the same file."
                    },
                )
            }
            null
        }
    }

    suspend fun retryUpload(location: LocationRecord): ContractRecord? {
        if (!ui.value.canRetryUpload) {
            return null
        }
        ui.update { it.copy(uploadStatus = CpWorkStatus.Retrying, banner = null) }
        return create(location)
    }

    suspend fun attach(id: String): ContractRecord? {
        val document = pendingDocument ?: return null
        val current = ui.value
        if (!current.canWrite || current.saving) {
            return null
        }
        ui.update {
            it.copy(
                saving = true,
                banner = null,
                uploadStatus = CpWorkStatus.Uploading,
                uploadPercent = 0,
            )
        }
        return try {
            val effective = current.draftEffectiveOn.trim().ifEmpty {
                current.items.find { it.id == id }?.startsOn.orEmpty()
            }
            val saved = contracts.attachDocument(id, document, effective) { percent ->
                ui.update { latest ->
                    latest.copy(
                        uploadPercent = percent,
                        uploadStatus = if (percent >= 100) CpWorkStatus.Uploaded else CpWorkStatus.Uploading,
                    )
                }
            }
            pendingDocument = null
            val versions = contracts.listVersions(id)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    draftFileName = null,
                    draftEffectiveOn = "",
                    uploadPercent = null,
                    uploadStatus = CpWorkStatus.Uploaded,
                    versions = versions,
                    items = latest.items.map { contract ->
                        if (contract.id == id) saved else contract
                    },
                    banner = null,
                )
            }
            analytics.track(ProductEvent.ContractUploaded(saved.id))
            saved
        } catch (failure: ContractFailure) {
            reportContractFailure(failure)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    uploadStatus = CpWorkStatus.Failed,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to upload this document."
                        ContractFailure.Rejected -> "The document was not uploaded."
                        ContractFailure.Upload -> "The document could not be uploaded. Retry keeps this contract and the same file."
                    },
                )
            }
            null
        }
    }

    suspend fun activate(contractId: String, versionId: String) {
        if (!ui.value.canWrite || ui.value.saving) {
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val updated = contracts.activateVersion(contractId, versionId)
            val versions = contracts.listVersions(contractId)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    items = latest.items.map { contract ->
                        if (contract.id == contractId) updated else contract
                    },
                    versions = versions,
                    banner = null,
                )
            }
        } catch (failure: ContractFailure) {
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to activate this version."
                        ContractFailure.Rejected -> "The version was not activated."
                        ContractFailure.Upload -> "The document could not be uploaded."
                    },
                )
            }
        }
    }

    suspend fun save(id: String, title: String, startsOn: String, endsOn: String, status: String) {
        if (!ui.value.canWrite || ui.value.saving) {
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val updated = contracts.update(id, title, startsOn, endsOn, status)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    items = latest.items.map { contract ->
                        if (contract.id == id) updated else contract
                    },
                    banner = null,
                )
            }
        } catch (failure: ContractFailure) {
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to save this contract."
                        ContractFailure.Rejected -> "The contract was not saved."
                        ContractFailure.Upload -> "The document could not be uploaded."
                    },
                )
            }
        }
    }

    suspend fun openDocument(path: String): String? {
        ui.update { it.copy(viewing = true, banner = null) }
        return try {
            val url = contracts.documentUrl(path)
            ui.update { it.copy(viewing = false) }
            url
        } catch (failure: ContractFailure) {
            ui.update { latest ->
                latest.copy(
                    viewing = false,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to open the document."
                        ContractFailure.Rejected -> "The document could not be opened."
                        ContractFailure.Upload -> "The document could not be opened."
                    },
                )
            }
            null
        }
    }

    fun contract(id: String): ContractRecord? {
        return ui.value.items.find { it.id == id }
    }

    fun clientName(clients: List<ClientRecord>, clientId: String): String {
        return clients.find { it.id == clientId }?.name ?: "Unknown client"
    }

    fun locationName(locations: List<LocationRecord>, locationId: String): String {
        return locations.find { it.id == locationId }?.name ?: "Unknown location"
    }

    suspend fun latestVersionId(contractId: String): String? {
        return try {
            contracts.listVersions(contractId).maxByOrNull { it.versionNumber }?.id
        } catch (_: ContractFailure) {
            null
        }
    }

    private fun reportContractFailure(failure: ContractFailure) {
        when (failure) {
            ContractFailure.Rejected,
            ContractFailure.Upload,
            -> {
                errorReporter.captureMessage(
                    message = "contract_operation_rejected",
                    level = ErrorLevel.Error,
                    tags = mapOf("feature" to "contracts", "failure" to failure.toString()),
                )
            }
            ContractFailure.Network -> Unit
        }
    }
}
