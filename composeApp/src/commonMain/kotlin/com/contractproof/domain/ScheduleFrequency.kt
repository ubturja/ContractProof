package com.contractproof.domain

enum class ScheduleFrequency {
    Daily,
    Weekdays,
    Weekly,
    ;

    fun toStorageValue(): String {
        return when (this) {
            Daily -> STORAGE_DAILY
            Weekdays -> STORAGE_WEEKDAYS
            Weekly -> STORAGE_WEEKLY
        }
    }

    companion object {
        const val STORAGE_DAILY = "daily"
        const val STORAGE_WEEKDAYS = "weekdays"
        const val STORAGE_WEEKLY = "weekly"

        fun fromStorageValue(value: String): ScheduleFrequency {
            return when (value.trim()) {
                STORAGE_DAILY -> Daily
                STORAGE_WEEKDAYS -> Weekdays
                STORAGE_WEEKLY -> Weekly
                else -> throw ScheduleRuleViolation("Schedule frequency is invalid.")
            }
        }
    }
}

class ScheduleRuleViolation(message: String) : IllegalArgumentException(message)

object ScheduleRules {
    fun requireFrequency(value: String): ScheduleFrequency {
        return ScheduleFrequency.fromStorageValue(value)
    }

    fun requireWeekdayForFrequency(frequency: ScheduleFrequency, weekday: Int?): Int {
        return when (frequency) {
            ScheduleFrequency.Weekly -> {
                val day = weekday ?: throw ScheduleRuleViolation("Visit day is required for a weekly schedule.")
                if (day !in 1..7) {
                    throw ScheduleRuleViolation("Visit weekday must be 1 through 7.")
                }
                day
            }
            ScheduleFrequency.Daily,
            ScheduleFrequency.Weekdays,
            -> weekday?.takeIf { it in 1..7 } ?: 1
        }
    }

    fun matchesDay(frequency: ScheduleFrequency, weekday: Int, isoDayOfWeek: Int): Boolean {
        return when (frequency) {
            ScheduleFrequency.Daily -> true
            ScheduleFrequency.Weekdays -> isoDayOfWeek in 1..5
            ScheduleFrequency.Weekly -> isoDayOfWeek == weekday
        }
    }

    fun frequencyLabel(frequency: ScheduleFrequency): String {
        return when (frequency) {
            ScheduleFrequency.Daily -> "Daily"
            ScheduleFrequency.Weekdays -> "Weekdays"
            ScheduleFrequency.Weekly -> "Weekly"
        }
    }

    fun generatesJobs(scheduleStatus: String): Boolean {
        return scheduleStatus == RequirementRules.ScheduleActive
    }
}
