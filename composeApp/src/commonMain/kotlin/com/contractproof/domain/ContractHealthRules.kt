package com.contractproof.domain

import kotlinx.datetime.LocalDate

data class ContractHealthSnapshot(
    val contractId: String,
    val statusLabel: String,
    val isActive: Boolean,
    val upcomingCount: Int,
    val nextServiceDate: LocalDate?,
    val coverageLine: String,
    val openDisputeCount: Int,
    val exceptionCount: Int,
    val listSubtitle: String,
)

object ContractHealthRules {
    private val upcomingStatuses = setOf(ServiceJobRules.Scheduled, ServiceJobRules.InProgress)
    private val nonTerminalStatuses = setOf(
        ServiceJobRules.Scheduled,
        ServiceJobRules.InProgress,
    )

    fun assemble(
        contract: ContractRecord,
        jobs: List<ServiceJob>,
        disputes: List<Dispute>,
        jobIdToContractId: Map<String, String>,
        today: LocalDate,
    ): ContractHealthSnapshot {
        val isActive = contract.status == ContractRules.Active
        val statusLabel = ContractRules.statusLabel(contract.status)
        val relevantJobs = jobs.filter { it.contractId == contract.id }
        val todayAndUpcoming = relevantJobs.filter { it.serviceDate >= today }
        val upcoming = todayAndUpcoming.filter { it.status in upcomingStatuses }
        val nextDate = upcoming.minByOrNull { it.serviceDate }?.serviceDate
        val coverageLine = coverageLine(todayAndUpcoming, today)
        val openDisputes = disputes.count { dispute ->
            dispute.status == DisputeStatus.Open &&
                dispute.serviceJobId != null &&
                jobIdToContractId[dispute.serviceJobId] == contract.id
        }
        val exceptions = relevantJobs
            .filter { it.status in nonTerminalStatuses }
            .sumOf { job ->
                job.requirements.count { it.status == JobRequirementStatus.Exception }
            }
        val subtitle = listSubtitle(upcoming.size, openDisputes, exceptions, isActive)
        return ContractHealthSnapshot(
            contractId = contract.id,
            statusLabel = statusLabel,
            isActive = isActive,
            upcomingCount = upcoming.size,
            nextServiceDate = nextDate,
            coverageLine = coverageLine,
            openDisputeCount = openDisputes,
            exceptionCount = exceptions,
            listSubtitle = subtitle,
        )
    }

    fun listSubtitle(
        upcomingCount: Int,
        openDisputeCount: Int,
        exceptionCount: Int,
        isActive: Boolean,
    ): String {
        if (!isActive) {
            return ""
        }
        val parts = mutableListOf<String>()
        parts.add("$upcomingCount upcoming")
        if (openDisputeCount > 0) {
            parts.add(
                if (openDisputeCount == 1) "1 open dispute" else "$openDisputeCount open disputes",
            )
        }
        if (exceptionCount > 0) {
            parts.add(
                if (exceptionCount == 1) "1 exception" else "$exceptionCount exceptions",
            )
        }
        return parts.joinToString(" · ")
    }

    private fun coverageLine(jobs: List<ServiceJob>, today: LocalDate): String {
        if (jobs.isEmpty()) {
            return "No upcoming services scheduled."
        }
        val todayJobs = jobs.filter { it.serviceDate == today }
        val minCoverage = jobs.minOfOrNull { it.evidenceStatus.coveragePercent }
        if (todayJobs.isNotEmpty() && minCoverage != null && minCoverage < 100) {
            return "Lowest today: $minCoverage%"
        }
        val allFull = jobs.all { it.evidenceStatus.coveragePercent == 100 }
        return if (allFull) {
            "All upcoming jobs at 100%"
        } else {
            "Lowest coverage: ${minCoverage ?: 0}%"
        }
    }
}
