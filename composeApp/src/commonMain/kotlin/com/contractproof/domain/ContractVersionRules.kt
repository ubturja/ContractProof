package com.contractproof.domain

data class ContractVersionRecord(
    val id: String,
    val organizationId: String,
    val contractId: String,
    val versionNumber: Int,
    val status: String,
    val effectiveOn: String,
    val createdBy: String,
    val documentPath: String?,
    val documentFileName: String?,
    val documentByteSize: Long?,
    val documentMimeType: String?,
)

data class ServiceJobPin(
    val id: String,
    val contractId: String,
    val contractVersionId: String,
)

object ContractVersionRules {
    const val Uploaded = "uploaded"
    const val Extracted = "extracted"
    const val Approved = "approved"
    const val Superseded = "superseded"

    fun nextNumber(versions: List<ContractVersionRecord>): Int {
        return (versions.maxOfOrNull { it.versionNumber } ?: 0) + 1
    }

    fun hasInReview(versions: List<ContractVersionRecord>): Boolean {
        return versions.any { it.status == Uploaded || it.status == Extracted }
    }

    fun requireCanStartVersion(versions: List<ContractVersionRecord>) {
        if (hasInReview(versions)) {
            throw ContractRuleViolation("Activate the current upload before adding another version.")
        }
    }

    fun requireEffectiveOn(value: String, previous: String?): String {
        val date = ContractRules.requireIsoDate(value, "Effective date")
        if (previous != null && date < previous) {
            throw ContractRuleViolation("Effective date cannot be before the previous version.")
        }
        return date
    }

    fun previousEffectiveOn(versions: List<ContractVersionRecord>): String? {
        return versions.maxByOrNull { it.versionNumber }?.effectiveOn
    }

    fun requireActivate(access: Access, version: ContractVersionRecord, organizationId: String) {
        ContractRules.requireWrite(access)
        ContractRules.requireOrganization(version.organizationId, organizationId)
        if (version.status != Uploaded) {
            throw ContractRuleViolation("Only an uploaded version can be activated.")
        }
    }

    fun requireApprove(
        access: Access,
        version: ContractVersionRecord,
        organizationId: String,
        requirementCount: Int,
    ) {
        ContractRules.requireWrite(access)
        ContractRules.requireOrganization(version.organizationId, organizationId)
        if (!isInReviewStatus(version.status)) {
            throw ContractRuleViolation("Only a version in review can be approved.")
        }
        if (requirementCount < 1) {
            throw ContractRuleViolation("Add at least one requirement before approving.")
        }
    }

    fun isActive(version: ContractVersionRecord, currentVersionId: String?): Boolean {
        return currentVersionId != null && version.id == currentVersionId && version.status == Approved
    }

    fun isInReview(version: ContractVersionRecord): Boolean {
        return isInReviewStatus(version.status)
    }

    fun isInReviewStatus(status: String): Boolean {
        return status == Uploaded || status == Extracted
    }

    fun statusLabel(version: ContractVersionRecord, currentVersionId: String?): String {
        return when {
            isActive(version, currentVersionId) -> "Active"
            isInReview(version) -> "In review"
            else -> "Inactive"
        }
    }

    fun keepPinnedJobs(jobs: List<ServiceJobPin>): List<ServiceJobPin> {
        return jobs
    }

    fun createdByLabel(createdBy: String, currentUserId: String): String {
        return if (createdBy == currentUserId) "You" else "Team member"
    }
}
