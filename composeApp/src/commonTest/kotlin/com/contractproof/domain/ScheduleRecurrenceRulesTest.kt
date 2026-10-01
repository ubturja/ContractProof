package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class ScheduleRecurrenceRulesTest {
    @Test
    fun dailyIncludesEveryDayInRange() {
        val schedule = sampleSchedule(frequency = ScheduleFrequency.STORAGE_DAILY)
        val dates = ScheduleRecurrenceRules.occurrenceDates(
            schedule = schedule,
            rangeStart = LocalDate.parse("2026-10-01"),
            rangeEnd = LocalDate.parse("2026-10-05"),
        )
        assertEquals(5, dates.size)
    }

    @Test
    fun weekdaysSkipsWeekends() {
        val schedule = sampleSchedule(frequency = ScheduleFrequency.STORAGE_WEEKDAYS)
        val dates = ScheduleRecurrenceRules.occurrenceDates(
            schedule = schedule,
            rangeStart = LocalDate.parse("2026-10-01"),
            rangeEnd = LocalDate.parse("2026-10-11"),
        )
        assertEquals(7, dates.size)
        assertTrue(dates.none { it.dayOfWeek.name == "SATURDAY" || it.dayOfWeek.name == "SUNDAY" })
    }

    @Test
    fun weeklyOnlyOnSpecificWeekday() {
        val schedule = sampleSchedule(
            frequency = ScheduleFrequency.STORAGE_WEEKLY,
            weekday = 3,
        )
        val dates = ScheduleRecurrenceRules.occurrenceDates(
            schedule = schedule,
            rangeStart = LocalDate.parse("2026-10-01"),
            rangeEnd = LocalDate.parse("2026-10-31"),
        )
        assertEquals(4, dates.size)
        assertTrue(dates.all { it.dayOfWeek.name == "WEDNESDAY" })
    }

    @Test
    fun pausedScheduleProducesNoDates() {
        val schedule = sampleSchedule(
            frequency = ScheduleFrequency.STORAGE_DAILY,
            status = RequirementRules.SchedulePaused,
        )
        val dates = ScheduleRecurrenceRules.occurrenceDates(
            schedule = schedule,
            rangeStart = LocalDate.parse("2026-10-01"),
            rangeEnd = LocalDate.parse("2026-10-07"),
        )
        assertEquals(0, dates.size)
    }

    private fun sampleSchedule(
        frequency: String,
        weekday: Int = 1,
        status: String = RequirementRules.ScheduleActive,
    ): ScheduleRecord {
        return ScheduleRecord(
            id = "s1",
            contractId = "c1",
            locationId = "l1",
            frequency = frequency,
            visit = RequirementVisit(
                weekday = weekday,
                startTime = "08:00:00",
                endTime = "10:00:00",
                timezone = "America/New_York",
                startsOn = "2026-10-01",
                endsOn = null,
                status = status,
            ),
        )
    }
}
