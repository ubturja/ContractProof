package com.contractproof.domain

object DisputeReconstructionRules {
    fun build(
        dispute: Dispute,
        items: List<DisputeItem>,
        job: ServiceJob,
        disputedRequirementText: String,
        contract: DisputeContractRef,
        evidence: List<Evidence>,
        exceptions: List<JobExceptionRecord>,
    ): DisputeReconstructionBundle {
        val requirementTextById = job.requirements.associate { it.id to it.requirementText }
        val events = mutableListOf<DisputeTimelineEvent>()

        events += DisputeTimelineEvent(
            occurredAt = job.scheduledStart,
            category = DisputeTimelineCategory.Job,
            title = "Job scheduled",
            body = "Scheduled start ${job.scheduledStart}; end ${job.scheduledEnd}.",
        )
        job.startedAt?.let { started ->
            events += DisputeTimelineEvent(
                occurredAt = started,
                category = DisputeTimelineCategory.Job,
                title = "Job started",
                body = job.assignee?.let { "Assigned worker: ${it.displayName}." }
                    ?: "Start recorded.",
            )
        }
        job.completedAt?.let { completed ->
            events += DisputeTimelineEvent(
                occurredAt = completed,
                category = DisputeTimelineCategory.Job,
                title = "Job completed",
                body = "Completion recorded.",
            )
        }

        evidence.filter { it.syncStatus == EvidenceSyncStatus.Uploaded }.forEach { record ->
            val label = requirementTextById[record.serviceJobRequirementId] ?: record.serviceJobRequirementId
            when (record.type) {
                EvidenceType.ChecklistCompletion -> {
                    events += DisputeTimelineEvent(
                        occurredAt = record.capturedAt,
                        category = DisputeTimelineCategory.Acknowledgement,
                        title = "Checklist recorded",
                        body = "Requirement: $label.",
                    )
                }
                EvidenceType.Timestamp -> {
                    events += DisputeTimelineEvent(
                        occurredAt = record.capturedAt,
                        category = DisputeTimelineCategory.Acknowledgement,
                        title = "Timestamp recorded",
                        body = "Requirement: $label.",
                    )
                }
                EvidenceType.Photo -> {
                    events += DisputeTimelineEvent(
                        occurredAt = record.capturedAt,
                        category = DisputeTimelineCategory.Evidence,
                        title = "Photo evidence recorded",
                        body = "Requirement: $label.",
                    )
                }
            }
        }

        exceptions.filter { it.syncStatus == EvidenceSyncStatus.Uploaded }.forEach { ex ->
            val label = requirementTextById[ex.serviceJobRequirementId] ?: ex.serviceJobRequirementId
            events += DisputeTimelineEvent(
                occurredAt = ex.recordedAt,
                category = DisputeTimelineCategory.Exception,
                title = "Exception recorded",
                body = "Requirement: $label. Reason: ${ex.reason}",
            )
        }

        items.filter { it.outcome == DisputeOutcome.Missing }.forEach { item ->
            val label = requirementTextById[item.serviceJobRequirementId] ?: item.serviceJobRequirementId
            events += DisputeTimelineEvent(
                occurredAt = dispute.createdAt,
                category = DisputeTimelineCategory.MissingEvidence,
                title = "No uploaded evidence at filing",
                body = "Requirement: $label.",
            )
        }

        events += DisputeTimelineEvent(
            occurredAt = dispute.createdAt,
            category = DisputeTimelineCategory.DisputeFiled,
            title = "Dispute filed",
            body = dispute.complaint,
        )

        val timeline = events.sortedWith(
            compareBy<DisputeTimelineEvent> { it.occurredAt }
                .thenBy { it.sortKey },
        )

        val missingLabels = items
            .filter { it.outcome == DisputeOutcome.Missing }
            .map { requirementTextById[it.serviceJobRequirementId] ?: it.serviceJobRequirementId }

        return DisputeReconstructionBundle(
            dispute = dispute,
            items = items,
            job = job,
            disputedRequirementText = disputedRequirementText,
            contract = contract,
            evidence = evidence,
            exceptions = exceptions,
            timeline = timeline,
            missingRequirementLabels = missingLabels,
        )
    }
}
