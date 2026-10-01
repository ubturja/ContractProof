package com.contractproof.data

import com.contractproof.domain.RequirementVisit
import com.contractproof.domain.ScheduleFrequency
import com.contractproof.domain.ScheduleRecord

sealed class ScheduleFailure : Exception() {
    data object Network : ScheduleFailure()

    data object Rejected : ScheduleFailure()
}

interface ScheduleGateway {
    suspend fun listForContract(contractId: String): List<ScheduleRecord>

    suspend fun upsertWeeklyVisit(
        contractVersionId: String,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord

    suspend fun upsertSchedule(
        contractVersionId: String,
        frequency: ScheduleFrequency,
        scheduleId: String?,
        visit: RequirementVisit,
    ): ScheduleRecord

    suspend fun setStatus(
        contractVersionId: String,
        scheduleId: String,
        status: String,
    ): ScheduleRecord
}
