package com.contractproof.feature.service

import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRules
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ServiceJobDisplay {
    fun statusLabel(status: String): String {
        return when (status) {
            ServiceJobRules.Scheduled -> "Scheduled"
            ServiceJobRules.InProgress -> "In progress"
            ServiceJobRules.Completed -> "Completed"
            ServiceJobRules.Incomplete -> "Incomplete"
            ServiceJobRules.Disputed -> "Disputed"
            ServiceJobRules.Cancelled -> "Cancelled"
            else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    fun serviceTimeWindow(job: ServiceJob): String {
        val zone = TimeZone.of(job.location.timezone)
        val start = formatClock(Instant.parse(job.scheduledStart).toLocalDateTime(zone))
        val end = formatClock(Instant.parse(job.scheduledEnd).toLocalDateTime(zone))
        return "$start – $end"
    }

    fun toTodayJobCard(job: ServiceJob): TodayJobCardUi {
        return TodayJobCardUi(
            jobId = job.id,
            locationName = job.location.name,
            clientName = job.client.name,
            serviceTimeLabel = serviceTimeWindow(job),
            statusLabel = statusLabel(job.status),
            coveragePercent = job.evidenceStatus.coveragePercent,
        )
    }

    private fun formatClock(dateTime: kotlinx.datetime.LocalDateTime): String {
        val hour24 = dateTime.hour
        val minute = dateTime.minute
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val minutePart = minute.toString().padStart(2, '0')
        return "$hour12:$minutePart $period"
    }
}

data class TodayJobCardUi(
    val jobId: String,
    val locationName: String,
    val clientName: String,
    val serviceTimeLabel: String,
    val statusLabel: String,
    val coveragePercent: Int,
)
