package com.contractproof.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object ScheduleRecurrenceRules {
    fun occurrenceDates(
        schedule: ScheduleRecord,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocalDate> {
        if (!ScheduleRules.generatesJobs(schedule.visit.status)) {
            return emptyList()
        }
        val frequency = ScheduleFrequency.fromStorageValue(schedule.frequency)
        val scheduleStart = LocalDate.parse(schedule.visit.startsOn)
        val scheduleEnd = schedule.visit.endsOn?.let { LocalDate.parse(it) }
        var cursor = maxOf(rangeStart, scheduleStart)
        val last = minOf(rangeEnd, scheduleEnd ?: rangeEnd)
        if (cursor > last) {
            return emptyList()
        }
        val dates = mutableListOf<LocalDate>()
        while (cursor <= last) {
            val isoDay = cursor.dayOfWeek.toIsoDayNumber()
            if (ScheduleRules.matchesDay(frequency, schedule.visit.weekday, isoDay)) {
                dates += cursor
            }
            cursor = cursor.plus(DatePeriod(days = 1))
        }
        return dates
    }

    private fun DayOfWeek.toIsoDayNumber(): Int {
        return when (this) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
            DayOfWeek.SUNDAY -> 7
        }
    }
}
