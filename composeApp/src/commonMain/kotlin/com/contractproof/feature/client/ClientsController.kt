package com.contractproof.feature.client

import com.contractproof.data.ClientFailure
import com.contractproof.data.ClientGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ClientRecord
import com.contractproof.domain.ClientRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClientsUiState(
    val query: String = "",
    val items: List<ClientRecord> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
    val draftName: String = "",
) {
    val visible: List<ClientRecord>
        get() = ClientRules.search(items, query)

    val canCreate: Boolean
        get() = canWrite && draftName.trim().isNotEmpty() && !loading && !saving
}

class ClientsController(
    private val organizations: OrganizationGateway,
    private val clients: ClientGateway,
) {
    private val ui = MutableStateFlow(ClientsUiState())
    val state: StateFlow<ClientsUiState> = ui.asStateFlow()

    fun updateQuery(value: String) {
        ui.update { it.copy(query = value) }
    }

    fun updateDraftName(value: String) {
        ui.update { it.copy(draftName = value) }
    }

    suspend fun refresh() {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenClients) {
            ui.update {
                it.copy(
                    loading = false,
                    canWrite = false,
                    banner = "Clients are not available.",
                    items = emptyList(),
                )
            }
            return
        }
        ui.update { it.copy(loading = true, banner = null, canWrite = access.canWriteClients) }
        try {
            val items = clients.list()
            ui.update { it.copy(loading = false, items = items, banner = null) }
        } catch (failure: ClientFailure) {
            ui.update { latest ->
                latest.copy(
                    loading = false,
                    banner = when (failure) {
                        ClientFailure.Network ->
                            if (latest.items.isEmpty()) {
                                "You need a connection to load clients."
                            } else {
                                "You are offline. Showing the last loaded clients."
                            }
                        ClientFailure.Rejected -> "Clients could not be loaded."
                    },
                )
            }
        }
    }

    suspend fun create() {
        val current = ui.value
        if (!current.canCreate) {
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val created = clients.create(current.draftName)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    draftName = "",
                    items = latest.items + created,
                    banner = null,
                )
            }
        } catch (failure: ClientFailure) {
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        ClientFailure.Network -> "You need a connection to create a client."
                        ClientFailure.Rejected -> "The client was not saved."
                    },
                )
            }
        }
    }

    suspend fun save(id: String, name: String, status: String) {
        if (!ui.value.canWrite || ui.value.saving) {
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val updated = clients.update(id, name, status)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    items = latest.items.map { client ->
                        if (client.id == id) updated else client
                    },
                    banner = null,
                )
            }
        } catch (failure: ClientFailure) {
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        ClientFailure.Network -> "You need a connection to save this client."
                        ClientFailure.Rejected -> "The client was not saved."
                    },
                )
            }
        }
    }

    fun client(id: String): ClientRecord? {
        return ui.value.items.find { it.id == id }
    }
}
