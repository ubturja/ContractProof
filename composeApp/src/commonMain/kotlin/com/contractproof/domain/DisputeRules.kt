package com.contractproof.domain

import kotlinx.datetime.LocalDate

object DisputeRules {
    fun requireCanFile(access: Access) {
        if (!access.canWriteDisputes && !access.canFileClientDispute) {
            throw DisputeRuleViolation("You cannot file a dispute.")
        }
    }

    fun requireDraft(job: ServiceJob, draft: DisputeDraft) {
        val complaint = draft.complaint.trim()
        if (complaint.isEmpty()) {
            throw DisputeRuleViolation("Issue description is required.")
        }
        if (draft.serviceJobId != job.id) {
            throw DisputeRuleViolation("Service job does not match.")
        }
        job.requirements.firstOrNull { it.id == draft.disputedServiceJobRequirementId }
            ?: throw DisputeRuleViolation("Select a requirement on this job.")
        requireDisputableJobStatus(job.status)
        if (draft.serviceDate != job.serviceDate) {
            // Allow same calendar date only for MVP consistency with job
            if (draft.serviceDate.toString() != job.serviceDate.toString()) {
                throw DisputeRuleViolation("Service date must match the job.")
            }
        }
        if (draft.attachmentLocalPath != null || draft.attachmentBytes != null) {
            if (draft.attachmentMimeType.isNullOrBlank()) {
                throw DisputeRuleViolation("Attachment type is required.")
            }
        }
    }

    fun requireDisputableJobStatus(status: String) {
        if (status != ServiceJobRules.Completed && status != ServiceJobRules.Disputed) {
            throw DisputeRuleViolation("Only a completed job can be disputed.")
        }
    }
}
