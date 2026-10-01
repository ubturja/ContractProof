package com.contractproof.domain

import kotlinx.datetime.LocalDate

interface ServiceJobRepository {
    suspend fun generateForApprovedVersion(
        contractId: String,
        contractVersionId: String,
        horizonDays: Int = ServiceJobGenerationRules.DefaultHorizonDays,
    )

    suspend fun get(jobId: String): ServiceJob?

    suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob>

    suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob>

    suspend fun listForContract(contractId: String, fromDate: LocalDate, limit: Int = 20): List<ServiceJob>

    suspend fun start(jobId: String, startedAt: String): ServiceJob

    suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob

    suspend fun markIncomplete(jobId: String): ServiceJob

    suspend fun markDisputed(jobId: String): ServiceJob
}
