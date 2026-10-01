package com.contractproof.domain

enum class RequirementFrequency {
    Weekly,
}

data class ScheduleRecord(
    val id: String,
    val contractId: String,
    val locationId: String,
    val frequency: String = ScheduleFrequency.STORAGE_WEEKLY,
    val visit: RequirementVisit,
)

data class RequirementVisit(
    val weekday: Int,
    val startTime: String,
    val endTime: String,
    val timezone: String,
    val startsOn: String,
    val endsOn: String?,
    val status: String,
)

data class RequirementRecord(
    val id: String,
    val organizationId: String,
    val contractId: String,
    val contractVersionId: String,
    val locationId: String,
    val locationName: String,
    val zoneCode: String?,
    val task: String,
    val sortOrder: Int,
    val requiresPhoto: Boolean,
    val isMandatory: Boolean,
    val source: String,
    val extractionKey: String?,
    val isActive: Boolean,
    val visits: List<RequirementVisit>,
)

class RequirementRuleViolation(message: String) : IllegalArgumentException(message)

object RequirementRules {
    const val SourceManual = "manual"
    const val SourceExtraction = "extraction"
    const val ScheduleActive = "active"
    const val SchedulePaused = "paused"
    const val ScheduleEnded = "ended"

    private val clockTime = Regex("^\\d{2}:\\d{2}(:\\d{2})?$")

    fun requireTask(task: String): String {
        val trimmed = task.trim()
        if (trimmed.isEmpty()) {
            throw RequirementRuleViolation("Requirement text is required.")
        }
        return trimmed
    }

    fun requireSortOrder(sortOrder: Int): Int {
        if (sortOrder < 0) {
            throw RequirementRuleViolation("Requirement order cannot be negative.")
        }
        return sortOrder
    }

    fun requireSource(source: String): String {
        if (source != SourceManual && source != SourceExtraction) {
            throw RequirementRuleViolation("Requirement source is invalid.")
        }
        return source
    }

    fun requireOrganization(organizationId: String, membershipOrganizationId: String) {
        if (organizationId != membershipOrganizationId) {
            throw RequirementRuleViolation("A requirement belongs to exactly one organization.")
        }
    }

    fun requireWrite(access: Access) {
        if (!access.canWriteContracts) {
            throw RequirementRuleViolation("Only an owner can change requirements.")
        }
    }

    fun requireEditable(versionStatus: String) {
        if (versionStatus == ContractVersionRules.Approved ||
            versionStatus == ContractVersionRules.Superseded
        ) {
            throw RequirementRuleViolation("Requirements on an approved contract version cannot be changed.")
        }
    }

    fun isActive(versionStatus: String): Boolean {
        return versionStatus == ContractVersionRules.Approved
    }

    fun requireVisit(visit: RequirementVisit): RequirementVisit {
        if (visit.weekday !in 1..7) {
            throw RequirementRuleViolation("Visit weekday must be 1 through 7.")
        }
        val start = requireClockTime(visit.startTime, "Start time")
        val end = requireClockTime(visit.endTime, "End time")
        if (end <= start) {
            throw RequirementRuleViolation("Service window must end after it starts.")
        }
        val timezone = visit.timezone.trim()
        if (timezone.isEmpty()) {
            throw RequirementRuleViolation("Visit timezone is required.")
        }
        val startsOn = ContractRules.requireIsoDate(visit.startsOn, "Visit start date")
        val endsOn = visit.endsOn?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
            ContractRules.requireIsoDate(value, "Visit end date")
        }
        if (endsOn != null && endsOn < startsOn) {
            throw RequirementRuleViolation("Visit end date cannot be before the start date.")
        }
        val status = visit.status.trim()
        if (status != ScheduleActive && status != SchedulePaused && status != ScheduleEnded) {
            throw RequirementRuleViolation("Visit status is invalid.")
        }
        return visit.copy(
            startTime = start,
            endTime = end,
            timezone = timezone,
            startsOn = startsOn,
            endsOn = endsOn,
            status = status,
        )
    }

    fun requireVisits(visits: List<RequirementVisit>): List<RequirementVisit> {
        return visits.map(::requireVisit)
    }

    fun nextSortOrder(requirements: List<RequirementRecord>): Int {
        return (requirements.maxOfOrNull { it.sortOrder } ?: -1) + 1
    }

    fun requireManualSource(source: String) {
        if (source != SourceManual) {
            throw RequirementRuleViolation("Only manual requirements can be changed here.")
        }
    }

    fun requireWeekdayWhenWeekly(frequency: RequirementFrequency, weekday: Int?): Int {
        if (frequency != RequirementFrequency.Weekly) {
            throw RequirementRuleViolation("Frequency is not supported.")
        }
        try {
            return ScheduleRules.requireWeekdayForFrequency(ScheduleFrequency.Weekly, weekday)
        } catch (error: ScheduleRuleViolation) {
            throw RequirementRuleViolation(error.message ?: "Visit day is invalid.")
        }
    }

    fun frequencyLabel(frequency: RequirementFrequency): String {
        return when (frequency) {
            RequirementFrequency.Weekly -> "Weekly"
        }
    }

    fun weekdayLabel(weekday: Int): String {
        return when (weekday) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $weekday"
        }
    }

    fun reorderSortOrders(orderedIds: List<String>): List<Pair<String, Int>> {
        val unique = orderedIds.toSet()
        if (unique.size != orderedIds.size) {
            throw RequirementRuleViolation("Requirement order has duplicates.")
        }
        return orderedIds.mapIndexed { index, id -> id to index }
    }

    fun moveInOrder(orderedIds: List<String>, id: String, delta: Int): List<String> {
        val index = orderedIds.indexOf(id)
        if (index < 0) {
            throw RequirementRuleViolation("Requirement is not in the list.")
        }
        val target = index + delta
        if (target < 0 || target >= orderedIds.size) {
            return orderedIds
        }
        val mutable = orderedIds.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        return mutable
    }

    fun requiredLabel(isMandatory: Boolean): String {
        return if (isMandatory) "Required" else "Optional"
    }

    fun evidenceLabel(requiresPhoto: Boolean): String {
        return if (requiresPhoto) "Photo required" else "No photo required"
    }

    fun activeLabel(isActive: Boolean): String {
        return if (isActive) "Active" else "Inactive"
    }

    private fun requireClockTime(value: String, label: String): String {
        val trimmed = value.trim()
        if (!clockTime.matches(trimmed)) {
            throw RequirementRuleViolation("$label must be a time as HH:MM or HH:MM:SS.")
        }
        return if (trimmed.length == 5) "$trimmed:00" else trimmed
    }
}
