package com.contractproof.feature.service

import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.domain.Access
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

data class TodayUiState(
    val organizationName: String = "",
    val jobs: List<TodayJobCardUi> = emptyList(),
    val loading: Boolean = false,
    val banner: String? = null,
) {
    val isEmpty: Boolean
        get() = !loading && jobs.isEmpty() && banner == null
}

class TodayController(
    private val organizations: OrganizationGateway,
    private val serviceJobs: ServiceJobRepository,
) {
    private val ui = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = ui.asStateFlow()

    suspend fun refresh() {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenAssignedJobs) {
            ui.update {
                it.copy(
                    loading = false,
                    jobs = emptyList(),
                    organizationName = membership?.organizationName.orEmpty(),
                    banner = "Today's jobs are not available.",
                )
            }
            return
        }
        val userId = membership?.userId
        if (userId.isNullOrBlank()) {
            ui.update {
                it.copy(
                    loading = false,
                    jobs = emptyList(),
                    organizationName = membership?.organizationName.orEmpty(),
                    banner = "Today's jobs could not be loaded.",
                )
            }
            return
        }
        ui.update {
            it.copy(
                loading = true,
                banner = null,
                organizationName = membership.organizationName,
            )
        }
        val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        try {
            val items = serviceJobs.listAssignedOn(today, userId)
            ui.update {
                it.copy(
                    loading = false,
                    jobs = items.map(ServiceJobDisplay::toTodayJobCard),
                    banner = null,
                )
            }
        } catch (failure: ServiceJobFailure) {
            ui.update { latest ->
                latest.copy(
                    loading = false,
                    banner = when (failure) {
                        ServiceJobFailure.Network ->
                            if (latest.jobs.isEmpty()) {
                                "You need a connection to load today's jobs."
                            } else {
                                "You are offline. Showing the last loaded jobs."
                            }
                        ServiceJobFailure.Rejected -> "Today's jobs could not be loaded."
                    },
                )
            }
        }
    }

    fun seedForPreview(jobs: List<ServiceJob>, organizationName: String = "Preview Co") {
        ui.value = TodayUiState(
            organizationName = organizationName,
            jobs = jobs.map(ServiceJobDisplay::toTodayJobCard),
            loading = false,
            banner = null,
        )
    }
}
