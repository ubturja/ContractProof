package com.contractproof.feature.clientportal

import com.contractproof.data.ClientServiceFailure
import com.contractproof.data.ClientServiceGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceRecordSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClientServiceListItem(
    val jobId: String,
    val locationName: String,
    val serviceDate: String,
    val coveragePercent: Int,
)

data class ClientServiceListUiState(
    val organizationName: String = "",
    val items: List<ClientServiceListItem> = emptyList(),
    val loading: Boolean = false,
    val banner: String? = null,
)

data class ClientServiceRecordUiState(
    val record: ServiceRecordSnapshot? = null,
    val loading: Boolean = false,
    val banner: String? = null,
)

class ClientServiceController(
    private val organizations: OrganizationGateway,
    private val clientServices: ClientServiceGateway,
) {
    private val listUi = MutableStateFlow(ClientServiceListUiState())
    val listState: StateFlow<ClientServiceListUiState> = listUi.asStateFlow()

    private val recordUi = MutableStateFlow(ClientServiceRecordUiState())
    val recordState: StateFlow<ClientServiceRecordUiState> = recordUi.asStateFlow()

    suspend fun refreshList() {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenClientServiceRecords) {
            listUi.update {
                it.copy(
                    loading = false,
                    banner = "Service records are not available.",
                )
            }
            return
        }
        listUi.update {
            it.copy(
                loading = true,
                banner = null,
                organizationName = membership?.organizationName.orEmpty(),
            )
        }
        try {
            val jobs = clientServices.listCompletedForClient()
            listUi.update {
                it.copy(
                    loading = false,
                    items = jobs.map(::toListItem),
                )
            }
        } catch (failure: ClientServiceFailure) {
            listUi.update {
                it.copy(
                    loading = false,
                    banner = when (failure) {
                        ClientServiceFailure.Network -> "You need a connection to load service records."
                        ClientServiceFailure.Rejected -> "Service records could not be loaded."
                    },
                )
            }
        }
    }

    suspend fun loadRecord(jobId: String) {
        recordUi.update { it.copy(loading = true, banner = null) }
        try {
            val record = clientServices.loadRecord(jobId)
            recordUi.update {
                it.copy(
                    loading = false,
                    record = record,
                    banner = if (record == null) "That service record is not available." else null,
                )
            }
        } catch (failure: ClientServiceFailure) {
            recordUi.update {
                it.copy(
                    loading = false,
                    banner = when (failure) {
                        ClientServiceFailure.Network -> "You need a connection to load this record."
                        ClientServiceFailure.Rejected -> "This service record could not be loaded."
                    },
                )
            }
        }
    }

    private fun toListItem(job: ServiceJob): ClientServiceListItem {
        return ClientServiceListItem(
            jobId = job.id,
            locationName = job.location.name,
            serviceDate = job.serviceDate.toString(),
            coveragePercent = job.evidenceStatus.coveragePercent,
        )
    }
}
