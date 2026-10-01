package com.contractproof.feature.contract

import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.domain.ContractHealthRules
import com.contractproof.domain.ContractHealthSnapshot
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractRules
import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class ContractHealthUiState(
    val detail: ContractHealthSnapshot? = null,
    val listSubtitles: Map<String, String> = emptyMap(),
    val loadingDetail: Boolean = false,
    val banner: String? = null,
)

class ContractHealthController(
    private val serviceJobs: ServiceJobRepository,
    private val disputes: DisputeGateway,
) {
    private val ui = MutableStateFlow(ContractHealthUiState())
    val state: StateFlow<ContractHealthUiState> = ui.asStateFlow()

    suspend fun load(contract: ContractRecord) {
        ui.update { it.copy(loadingDetail = true, banner = null) }
        val today = today()
        try {
            val jobs = serviceJobs.listForContract(contract.id, today, limit = 20)
            val disputeItems = disputes.list()
            val jobMap = buildJobContractMap(jobs, disputeItems)
            val snapshot = ContractHealthRules.assemble(contract, jobs, disputeItems, jobMap, today)
            ui.update { it.copy(loadingDetail = false, detail = snapshot, banner = null) }
        } catch (_: Exception) {
            ui.update {
                it.copy(
                    loadingDetail = false,
                    banner = "Contract health could not be loaded.",
                )
            }
        }
    }

    suspend fun refreshSummaries(contracts: List<ContractRecord>) {
        val active = contracts.filter { it.status == ContractRules.Active }
        if (active.isEmpty()) {
            ui.update { it.copy(listSubtitles = emptyMap()) }
            return
        }
        val today = today()
        val disputeItems = try {
            disputes.list()
        } catch (_: DisputeFailure) {
            emptyList()
        }
        val subtitles = mutableMapOf<String, String>()
        for (contract in active.take(25)) {
            val jobs = try {
                serviceJobs.listForContract(contract.id, today, limit = 20)
            } catch (_: Exception) {
                emptyList()
            }
            val jobMap = buildJobContractMap(jobs, disputeItems)
            val snapshot = ContractHealthRules.assemble(contract, jobs, disputeItems, jobMap, today)
            if (snapshot.listSubtitle.isNotEmpty()) {
                subtitles[contract.id] = snapshot.listSubtitle
            }
        }
        ui.update { it.copy(listSubtitles = subtitles) }
    }

    private suspend fun buildJobContractMap(
        jobs: List<ServiceJob>,
        disputeItems: List<Dispute>,
    ): Map<String, String> {
        val map = jobs.associate { it.id to it.contractId }.toMutableMap()
        val missingJobIds = disputeItems
            .filter { it.status == DisputeStatus.Open && it.serviceJobId != null }
            .mapNotNull { it.serviceJobId }
            .filter { it !in map }
            .distinct()
        for (jobId in missingJobIds) {
            val job = serviceJobs.get(jobId)
            if (job != null) {
                map[jobId] = job.contractId
            }
        }
        return map
    }

    private fun today(): kotlinx.datetime.LocalDate {
        return Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }
}
