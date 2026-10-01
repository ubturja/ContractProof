package com.contractproof.domain

class ServiceJobRuleViolation(message: String) : IllegalArgumentException(message)

object ServiceJobRules {
    const val Scheduled = "scheduled"
    const val InProgress = "in_progress"
    const val Completed = "completed"
    const val Cancelled = "cancelled"
    const val Incomplete = "incomplete"
    const val Disputed = "disputed"

    private val knownStatuses = setOf(
        Scheduled,
        InProgress,
        Completed,
        Cancelled,
        Incomplete,
        Disputed,
    )

    private val terminalStatuses = setOf(Cancelled, Incomplete, Disputed)

    fun requireStorageStatus(status: String): String {
        if (status !in knownStatuses) {
            throw ServiceJobRuleViolation("Service job status is invalid.")
        }
        return status
    }

    fun parseStorageStatus(status: String): String = requireStorageStatus(status)

    fun computeEvidenceStatus(requirements: List<ServiceJobRequirement>): ServiceJobEvidenceStatus {
        val mandatory = requirements.filter { it.isMandatory }
        val mandatoryTotal = mandatory.size
        val mandatorySatisfied = mandatory.count {
            it.status == JobRequirementStatus.Satisfied || it.status == JobRequirementStatus.Exception
        }
        val coveragePercent = if (mandatoryTotal == 0) {
            100
        } else {
            (mandatorySatisfied * 100) / mandatoryTotal
        }
        val state = when {
            mandatoryTotal == 0 -> ServiceJobEvidenceState.ReadyToComplete
            mandatorySatisfied == 0 -> ServiceJobEvidenceState.NotStarted
            mandatorySatisfied < mandatoryTotal -> ServiceJobEvidenceState.Partial
            else -> ServiceJobEvidenceState.ReadyToComplete
        }
        return ServiceJobEvidenceStatus(
            mandatoryTotal = mandatoryTotal,
            mandatorySatisfied = mandatorySatisfied,
            coveragePercent = coveragePercent,
            state = state,
        )
    }

    fun canComplete(requirements: List<ServiceJobRequirement>): Boolean {
        return computeEvidenceStatus(requirements).state == ServiceJobEvidenceState.ReadyToComplete
    }

    fun requireStatusTransition(from: String, to: String) {
        requireStorageStatus(from)
        requireStorageStatus(to)
        if (from == to) {
            return
        }
        if (from in terminalStatuses) {
            throw ServiceJobRuleViolation("This job cannot change status.")
        }
        val allowed = when (from) {
            Scheduled -> to == InProgress || to == Cancelled
            InProgress -> to == Completed || to == Incomplete || to == Cancelled
            Completed -> to == Disputed
            else -> false
        }
        if (!allowed) {
            throw ServiceJobRuleViolation("This status change is not allowed.")
        }
    }

    fun requireStart(currentStatus: String, startedAt: String) {
        requireStatusTransition(currentStatus, InProgress)
        if (startedAt.isBlank()) {
            throw ServiceJobRuleViolation("Start time is required.")
        }
    }

    fun requireComplete(
        currentStatus: String,
        requirements: List<ServiceJobRequirement>,
        completedAt: String,
        completedBy: String,
    ) {
        requireStatusTransition(currentStatus, Completed)
        if (!canComplete(requirements)) {
            throw ServiceJobRuleViolation("Mandatory requirements are not complete.")
        }
        if (completedAt.isBlank()) {
            throw ServiceJobRuleViolation("Completion time is required.")
        }
        if (completedBy.isBlank()) {
            throw ServiceJobRuleViolation("Completion user is required.")
        }
    }

    fun requireMarkIncomplete(currentStatus: String) {
        requireStatusTransition(currentStatus, Incomplete)
    }

    fun requireMarkDisputed(currentStatus: String, startedAt: String?, completedAt: String?, completedBy: String?) {
        requireStatusTransition(currentStatus, Disputed)
        if (startedAt.isNullOrBlank() || completedAt.isNullOrBlank() || completedBy.isNullOrBlank()) {
            throw ServiceJobRuleViolation("A disputed job must have been completed first.")
        }
    }

    fun requireAssigneeForCleanerAction(userId: String, job: ServiceJob) {
        val assigneeId = job.assignee?.userId
        if (assigneeId == null || assigneeId != userId) {
            throw ServiceJobRuleViolation("This job is not assigned to you.")
        }
    }

    fun requireJobAction(access: Access, membershipUserId: String, job: ServiceJob) {
        if (access.canOpenAssignedJobs && !access.canWriteContracts) {
            requireAssigneeForCleanerAction(membershipUserId, job)
        } else if (!access.canOpenAssignedJobs && !access.canWriteContracts) {
            throw ServiceJobRuleViolation("You cannot update this job.")
        }
    }
}
