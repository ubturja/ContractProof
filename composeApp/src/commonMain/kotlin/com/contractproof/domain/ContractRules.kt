package com.contractproof.domain

data class ContractRecord(
    val id: String,
    val organizationId: String,
    val clientId: String,
    val locationId: String,
    val title: String,
    val status: String,
    val startsOn: String,
    val endsOn: String?,
    val currentVersionId: String?,
    val documentPath: String?,
    val documentFileName: String? = null,
    val documentByteSize: Long? = null,
    val documentMimeType: String? = null,
)

class ContractRuleViolation(message: String) : IllegalArgumentException(message)

object ContractRules {
    const val Draft = "draft"
    const val Active = "active"
    const val Ended = "ended"
    const val PdfMimeType = "application/pdf"
    const val MaxPdfBytes = 20 * 1024 * 1024

    private val isoDate = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val uuidPath = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE,
    )

    fun requireTitle(title: String): String {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            throw ContractRuleViolation("Contract title is required.")
        }
        return trimmed
    }

    fun requireIsoDate(value: String, label: String): String {
        val trimmed = value.trim()
        if (!isoDate.matches(trimmed)) {
            throw ContractRuleViolation("$label must be a date as YYYY-MM-DD.")
        }
        return trimmed
    }

    fun optionalEndDate(value: String, startsOn: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val end = requireIsoDate(trimmed, "End date")
        requireTermOrder(startsOn, end)
        return end
    }

    fun requireTermOrder(startsOn: String, endsOn: String?) {
        if (endsOn != null && endsOn < startsOn) {
            throw ContractRuleViolation("End date cannot be before the start date.")
        }
    }

    fun requireOrganization(organizationId: String, membershipOrganizationId: String) {
        if (organizationId != membershipOrganizationId) {
            throw ContractRuleViolation("A contract belongs to exactly one organization.")
        }
    }

    fun requireLocationForClient(location: LocationRecord, clientId: String, organizationId: String) {
        if (location.organizationId != organizationId) {
            throw ContractRuleViolation("A contract location must be in the same organization.")
        }
        if (location.clientId != clientId) {
            throw ContractRuleViolation("A contract location must belong to the assigned client.")
        }
    }

    fun requireWritableStatus(status: String) {
        if (status != Draft && status != Ended) {
            throw ContractRuleViolation("A contract cannot be set active until requirements are approved.")
        }
    }

    fun requireStatus(status: String) {
        if (status != Draft && status != Active && status != Ended) {
            throw ContractRuleViolation("Contract status is invalid.")
        }
    }

    fun requireWrite(access: Access) {
        if (!access.canWriteContracts) {
            throw ContractRuleViolation("Only an owner can change contracts.")
        }
    }

    fun requirePdfFileName(fileName: String): String {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) {
            throw ContractRuleViolation("A PDF file name is required.")
        }
        return trimmed
    }

    fun requirePdfBytes(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) {
            throw ContractRuleViolation("The selected file is empty.")
        }
        if (bytes.size > MaxPdfBytes) {
            throw ContractRuleViolation("A contract PDF must be 20 MB or smaller.")
        }
        if (bytes.size < 5 ||
            bytes[0] != 0x25.toByte() ||
            bytes[1] != 0x50.toByte() ||
            bytes[2] != 0x44.toByte() ||
            bytes[3] != 0x46.toByte()
        ) {
            throw ContractRuleViolation("Only a PDF contract can be uploaded.")
        }
        return bytes
    }

    fun objectPath(organizationId: String, contractId: String, versionId: String): String {
        requirePathId(organizationId, "Organization")
        requirePathId(contractId, "Contract")
        requirePathId(versionId, "Version")
        return "${organizationId.lowercase()}/${contractId.lowercase()}/${versionId.lowercase()}.pdf"
    }

    fun requirePathInOrganization(path: String, organizationId: String) {
        val normalized = path.lowercase()
        val prefix = "${organizationId.lowercase()}/"
        if (!normalized.startsWith(prefix) || normalized == prefix) {
            throw ContractRuleViolation("A contract document belongs to exactly one organization.")
        }
        val parts = normalized.split('/')
        if (parts.size != 3 || !parts[2].endsWith(".pdf")) {
            throw ContractRuleViolation("A contract document path is invalid.")
        }
        requirePathId(parts[1], "Contract")
        val version = parts[2].removeSuffix(".pdf")
        requirePathId(version, "Version")
    }

    private fun requirePathId(value: String, label: String) {
        if (!uuidPath.matches(value)) {
            throw ContractRuleViolation("$label id is invalid.")
        }
    }

    fun hasDocument(contract: ContractRecord): Boolean {
        return !contract.documentPath.isNullOrEmpty()
    }

    fun byteSizeLabel(bytes: Long): String {
        if (bytes < 1024) {
            return "$bytes B"
        }
        if (bytes < 1024 * 1024) {
            return "${bytes / 1024} KB"
        }
        return "${bytes / (1024 * 1024)} MB"
    }

    fun statusLabel(status: String): String {
        return when (status) {
            Draft -> "Draft"
            Active -> "Active"
            Ended -> "Ended"
            else -> status
        }
    }
}
