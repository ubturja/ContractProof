package com.contractproof.feature.dashboard

import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.domain.Access
import com.contractproof.domain.DashboardActivityItem
import com.contractproof.domain.DashboardAssemblyRules
import com.contractproof.domain.DashboardDisputeRow
import com.contractproof.domain.DashboardEvidenceGap
import com.contractproof.domain.DashboardSnapshot
import com.contractproof.domain.Dispute
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.feature.service.ServiceJobDisplay
import com.contractproof.feature.service.TodayJobCardUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class DashboardUiState(
    val organizationName: String = "",
    val loading: Boolean = false,
    val banner: String? = null,
    val needsAttention: Boolean = false,
    val attentionLine: String = "",
    val todayJobs: List<TodayJobCardUi> = emptyList(),
    val evidenceGaps: List<DashboardEvidenceGap> = emptyList(),
    val openDisputes: List<DashboardDisputeRow> = emptyList(),
    val completionLabel: String = "",
    val coverageLine: String = "",
    val recentActivity: List<DashboardActivityItem> = emptyList(),
) {
    val jobsEmpty: Boolean
        get() = !loading && todayJobs.isEmpty() && banner == null
}

class DashboardController(
    private val organizations: OrganizationGateway,
    private val serviceJobs: ServiceJobRepository,
    private val disputes: DisputeGateway,
) {
    private val ui = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = ui.asStateFlow()

    private var cachedJobs: List<ServiceJob> = emptyList()
    private var cachedDisputes: List<Dispute> = emptyList()

    suspend fun refresh() {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenDashboard) {
            ui.update {
                it.copy(
                    loading = false,
                    organizationName = membership?.organizationName.orEmpty(),
                    banner = "Dashboard is not available for your role.",
                )
            }
            return
        }
        val orgName = membership?.organizationName.orEmpty()
        ui.update {
            it.copy(
                loading = true,
                banner = null,
                organizationName = orgName,
            )
        }
        val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        var jobsBanner: String? = null
        var disputesBanner: String? = null
        val jobs = try {
            val loaded = serviceJobs.listForOrganizationOn(today)
            cachedJobs = loaded
            loaded
        } catch (failure: ServiceJobFailure) {
            when (failure) {
                ServiceJobFailure.Network -> {
                    jobsBanner = if (cachedJobs.isEmpty()) {
                        "You need a connection to load the dashboard."
                    } else {
                        "You are offline. Job data may be outdated."
                    }
                    cachedJobs
                }
                ServiceJobFailure.Rejected -> {
                    jobsBanner = "Today's jobs could not be loaded."
                    emptyList()
                }
            }
        }
        if (jobsBanner == "You need a connection to load the dashboard." && cachedJobs.isEmpty()) {
            ui.update { it.copy(loading = false, banner = jobsBanner) }
            return
        }
        val disputeItems = try {
            val loaded = disputes.list()
            cachedDisputes = loaded
            loaded
        } catch (failure: DisputeFailure) {
            when (failure) {
                DisputeFailure.Network -> {
                    disputesBanner = if (cachedDisputes.isEmpty()) {
                        "Could not load disputes. Check your connection."
                    } else {
                        "Could not refresh disputes. Showing the last loaded list."
                    }
                    cachedDisputes
                }
                is DisputeFailure.Rejected -> {
                    disputesBanner = failure.userMessage
                    emptyList()
                }
            }
        }
        val snapshot = DashboardAssemblyRules.assemble(jobs, disputeItems)
        val banner = jobsBanner ?: disputesBanner
        ui.update {
            it.copy(
                loading = false,
                banner = banner,
                needsAttention = snapshot.needsAttention,
                attentionLine = attentionLine(snapshot),
                todayJobs = snapshot.todayJobs.map(ServiceJobDisplay::toTodayJobCard),
                evidenceGaps = snapshot.evidenceGaps,
                openDisputes = snapshot.openDisputes,
                completionLabel = snapshot.completion.label,
                coverageLine = coverageLine(snapshot),
                recentActivity = snapshot.recentActivity,
            )
        }
    }

    private fun attentionLine(snapshot: DashboardSnapshot): String {
        return if (snapshot.needsAttention) {
            "${snapshot.attentionCount} items need attention"
        } else {
            "No service or evidence problems right now."
        }
    }

    private fun coverageLine(snapshot: DashboardSnapshot): String {
        val coverage = snapshot.coverage
        return when {
            coverage.jobsNeedingAttention == 0 ->
                "All active services today are at full evidence coverage."
            coverage.worstLocationName != null && coverage.worstCoveragePercent != null ->
                "${coverage.jobsNeedingAttention} job(s) below 100% coverage · Lowest: ${coverage.worstCoveragePercent}% at ${coverage.worstLocationName}"
            else ->
                "${coverage.jobsNeedingAttention} job(s) below 100% coverage today."
        }
    }

    fun seedForPreview(snapshot: DashboardSnapshot, organizationName: String = "Preview Co") {
        cachedJobs = snapshot.todayJobs
        ui.value = DashboardUiState(
            organizationName = organizationName,
            loading = false,
            banner = null,
            needsAttention = snapshot.needsAttention,
            attentionLine = attentionLine(snapshot),
            todayJobs = snapshot.todayJobs.map(ServiceJobDisplay::toTodayJobCard),
            evidenceGaps = snapshot.evidenceGaps,
            openDisputes = snapshot.openDisputes,
            completionLabel = snapshot.completion.label,
            coverageLine = coverageLine(snapshot),
            recentActivity = snapshot.recentActivity,
        )
    }
}
