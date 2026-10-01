package com.contractproof.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

data class ServiceJobRequirementDraft(
    val contractRequirementId: String,
    val requirementText: String,
    val requiresPhoto: Boolean,
    val isMandatory: Boolean,
    val sortOrder: Int,
)

data class ServiceJobDraft(
    val scheduleId: String,
    val contractVersionId: String,
    val scheduledStart: String,
    val scheduledEnd: String,
    val requirements: List<ServiceJobRequirementDraft>,
)

data class ServiceJobGenerationContext(
    val contractId: String,
    val contractVersionId: String,
    val clientId: String,
    val locationId: String,
    val organizationId: String,
)

object ServiceJobGenerationRules {
    const val DefaultHorizonDays = 14

    fun buildDrafts(
        context: ServiceJobGenerationContext,
        schedules: List<ScheduleRecord>,
        requirements: List<RequirementRecord>,
        today: LocalDate,
        horizonDays: Int = DefaultHorizonDays,
    ): List<ServiceJobDraft> {
        if (requirements.isEmpty()) {
            return emptyList()
        }
        val rangeEnd = today.plus(DatePeriod(days = horizonDays - 1))
        val requirementDrafts = requirements
            .sortedBy { it.sortOrder }
            .map { requirement ->
                ServiceJobRequirementDraft(
                    contractRequirementId = requirement.id,
                    requirementText = requirement.task,
                    requiresPhoto = requirement.requiresPhoto,
                    isMandatory = requirement.isMandatory,
                    sortOrder = requirement.sortOrder,
                )
            }
        return schedules.flatMap { schedule ->
            val dates = ScheduleRecurrenceRules.occurrenceDates(schedule, today, rangeEnd)
            dates.map { date ->
                val start = localInstant(schedule, date, schedule.visit.startTime)
                val end = localInstant(schedule, date, schedule.visit.endTime)
                ServiceJobDraft(
                    scheduleId = schedule.id,
                    contractVersionId = context.contractVersionId,
                    scheduledStart = start,
                    scheduledEnd = end,
                    requirements = requirementDrafts,
                )
            }
        }
    }

    private fun localInstant(schedule: ScheduleRecord, date: LocalDate, clockTime: String): String {
        val parts = clockTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val second = parts.getOrNull(2)?.toInt() ?: 0
        val zone = TimeZone.of(schedule.visit.timezone)
        val local = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute, second)
        return local.toInstant(zone).toString()
    }
}
