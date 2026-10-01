package com.contractproof.domain

data class DashboardEvidenceGap(
    val jobId: String,
    val locationName: String,
    val coveragePercent: Int,
)

data class DashboardDisputeRow(
    val disputeId: String,
    val clientName: String,
    val locationName: String,
    val serviceDate: String,
)

data class DashboardCompletionSummary(
    val completedCount: Int,
    val totalCount: Int,
    val label: String,
)

data class DashboardCoverageSummary(
    val jobsNeedingAttention: Int,
    val worstCoveragePercent: Int?,
    val worstLocationName: String?,
)

enum class DashboardActivityKind {
    DisputeOpened,
    ServiceCompleted,
    ServiceStarted,
}

data class DashboardActivityItem(
    val kind: DashboardActivityKind,
    val occurredAt: String,
    val label: String,
    val referenceId: String,
)

data class DashboardSnapshot(
    val todayJobs: List<ServiceJob>,
    val evidenceGaps: List<DashboardEvidenceGap>,
    val openDisputes: List<DashboardDisputeRow>,
    val completion: DashboardCompletionSummary,
    val coverage: DashboardCoverageSummary,
    val recentActivity: List<DashboardActivityItem>,
    val needsAttention: Boolean,
    val attentionCount: Int,
)

object DashboardAssemblyRules {
    private const val MAX_GAPS = 5
    private const val MAX_DISPUTES = 5
    private const val MAX_ACTIVITY = 8

    fun assemble(todayJobs: List<ServiceJob>, disputes: List<Dispute>): DashboardSnapshot {
        val gaps = evidenceGaps(todayJobs).take(MAX_GAPS)
        val open = openDisputes(disputes).take(MAX_DISPUTES)
        val completion = completionSummary(todayJobs)
        val coverage = coverageSummary(todayJobs)
        val activity = recentActivity(todayJobs, disputes).take(MAX_ACTIVITY)
        val attention = gaps.size + open.size + incompleteServiceCount(todayJobs)
        return DashboardSnapshot(
            todayJobs = todayJobs,
            evidenceGaps = gaps,
            openDisputes = open,
            completion = completion,
            coverage = coverage,
            recentActivity = activity,
            needsAttention = attention > 0,
            attentionCount = attention,
        )
    }

    private fun evidenceGaps(jobs: List<ServiceJob>): List<DashboardEvidenceGap> {
        return jobs.filter { hasEvidenceGap(it) }
            .sortedBy { it.evidenceStatus.coveragePercent }
            .map {
                DashboardEvidenceGap(
                    jobId = it.id,
                    locationName = it.location.name,
                    coveragePercent = it.evidenceStatus.coveragePercent,
                )
            }
    }

    private fun hasEvidenceGap(job: ServiceJob): Boolean {
        if (job.status == ServiceJobRules.Cancelled) {
            return false
        }
        if (job.evidenceStatus.state != ServiceJobEvidenceState.ReadyToComplete) {
            return true
        }
        return job.requirements.any { requirement ->
            requirement.isMandatory &&
                requirement.requiresPhoto &&
                requirement.status == JobRequirementStatus.Missing
        }
    }

    private fun openDisputes(disputes: List<Dispute>): List<DashboardDisputeRow> {
        return disputes.filter { it.status == DisputeStatus.Open }
            .sortedByDescending { it.createdAt }
            .map {
                DashboardDisputeRow(
                    disputeId = it.id,
                    clientName = it.clientName,
                    locationName = it.locationName,
                    serviceDate = it.serviceDate.toString(),
                )
            }
    }

    private fun completionSummary(jobs: List<ServiceJob>): DashboardCompletionSummary {
        val relevant = jobs.filter { it.status != ServiceJobRules.Cancelled }
        val completed = relevant.count { it.status == ServiceJobRules.Completed || it.status == ServiceJobRules.Disputed }
        val total = relevant.size
        val label = if (total == 0) {
            "No services scheduled today"
        } else {
            "$completed of $total services complete today"
        }
        return DashboardCompletionSummary(
            completedCount = completed,
            totalCount = total,
            label = label,
        )
    }

    private fun coverageSummary(jobs: List<ServiceJob>): DashboardCoverageSummary {
        val active = jobs.filter {
            it.status == ServiceJobRules.Scheduled ||
                it.status == ServiceJobRules.InProgress ||
                it.status == ServiceJobRules.Completed
        }
        val needing = active.filter {
            it.evidenceStatus.coveragePercent < 100 ||
                it.evidenceStatus.state == ServiceJobEvidenceState.Partial ||
                it.evidenceStatus.state == ServiceJobEvidenceState.NotStarted
        }
        val worst = needing.minByOrNull { it.evidenceStatus.coveragePercent }
        return DashboardCoverageSummary(
            jobsNeedingAttention = needing.size,
            worstCoveragePercent = worst?.evidenceStatus?.coveragePercent,
            worstLocationName = worst?.location?.name,
        )
    }

    private fun incompleteServiceCount(jobs: List<ServiceJob>): Int {
        return jobs.count {
            it.status != ServiceJobRules.Completed &&
                it.status != ServiceJobRules.Disputed &&
                it.status != ServiceJobRules.Cancelled
        }
    }

    private fun recentActivity(jobs: List<ServiceJob>, disputes: List<Dispute>): List<DashboardActivityItem> {
        val items = mutableListOf<DashboardActivityItem>()
        disputes.forEach { dispute ->
            items.add(
                DashboardActivityItem(
                    kind = DashboardActivityKind.DisputeOpened,
                    occurredAt = dispute.createdAt,
                    label = "Dispute opened · ${dispute.locationName}",
                    referenceId = dispute.id,
                ),
            )
        }
        jobs.forEach { job ->
            job.completedAt?.let { completedAt ->
                items.add(
                    DashboardActivityItem(
                        kind = DashboardActivityKind.ServiceCompleted,
                        occurredAt = completedAt,
                        label = "Service completed · ${job.location.name}",
                        referenceId = job.id,
                    ),
                )
            }
            job.startedAt?.let { startedAt ->
                items.add(
                    DashboardActivityItem(
                        kind = DashboardActivityKind.ServiceStarted,
                        occurredAt = startedAt,
                        label = "Service started · ${job.location.name}",
                        referenceId = job.id,
                    ),
                )
            }
        }
        return items.sortedByDescending { it.occurredAt }
    }
}
