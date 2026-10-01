package com.contractproof.feature.dispute

import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.DisputeAiSummary
import com.contractproof.domain.DisputeReconstructionBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

data class DisputeDetailUiState(
    val loading: Boolean = false,
    val bundle: DisputeReconstructionBundle? = null,
    val aiSummary: DisputeAiSummary? = null,
    val summaryLoading: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
    val canOpenReport: Boolean = false,
)

class DisputeDetailController(
    private val organizations: OrganizationGateway,
    private val disputes: DisputeGateway,
) {
    private val ui = MutableStateFlow(DisputeDetailUiState())
    val state: StateFlow<DisputeDetailUiState> = ui.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(disputeId: String) {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        ui.update {
            it.copy(
                loading = true,
                banner = null,
                canWrite = access.canWriteDisputes,
                canOpenReport = access.canOpenDisputes,
            )
        }
        try {
            val bundle = disputes.getReconstruction(disputeId)
            if (bundle == null) {
                ui.update { it.copy(loading = false, banner = "Dispute not found.") }
                return
            }
            if (bundle.items.isEmpty()) {
                ui.update {
                    it.copy(
                        loading = false,
                        bundle = bundle,
                        banner = "Dispute items are missing. Re-file the dispute or contact support.",
                    )
                }
                return
            }
            val cachedSummary = bundle.dispute.aiSummaryJson?.let { node ->
                runCatching {
                    json.decodeFromJsonElement(DisputeAiSummary.serializer(), node)
                }.getOrNull()
            }
            ui.update {
                it.copy(
                    loading = false,
                    bundle = bundle,
                    aiSummary = cachedSummary,
                )
            }
        } catch (error: DisputeFailure) {
            ui.update {
                it.copy(
                    loading = false,
                    banner = when (error) {
                        DisputeFailure.Network -> "Could not load dispute. Check your connection."
                        is DisputeFailure.Rejected -> error.userMessage
                    },
                )
            }
        }
    }

    suspend fun generateSummary(disputeId: String) {
        ui.update { it.copy(summaryLoading = true, banner = null) }
        try {
            val summary = disputes.requestSummary(disputeId)
            ui.update { it.copy(summaryLoading = false, aiSummary = summary) }
        } catch (error: DisputeFailure) {
            ui.update {
                it.copy(
                    summaryLoading = false,
                    banner = when (error) {
                        DisputeFailure.Network -> "Could not generate summary. Check your connection."
                        is DisputeFailure.Rejected -> error.userMessage
                    },
                )
            }
        }
    }
}
