package com.contractproof.feature.dispute

import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.Dispute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DisputesUiState(
    val items: List<Dispute> = emptyList(),
    val loading: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
)

class DisputesController(
    private val organizations: OrganizationGateway,
    private val disputes: DisputeGateway,
) {
    private val ui = MutableStateFlow(DisputesUiState())
    val state: StateFlow<DisputesUiState> = ui.asStateFlow()

    suspend fun load() {
        val membership = organizations.currentMembership()
        val canWrite = membership?.let { Access.forMembership(it.role).canWriteDisputes } ?: false
        ui.update { it.copy(loading = true, banner = null, canWrite = canWrite) }
        try {
            val items = disputes.list()
            ui.update { it.copy(items = items, loading = false) }
        } catch (error: DisputeFailure) {
            ui.update {
                it.copy(
                    loading = false,
                    banner = when (error) {
                        DisputeFailure.Network -> "Could not load disputes. Check your connection."
                        is DisputeFailure.Rejected -> error.userMessage
                    },
                )
            }
        }
    }
}
