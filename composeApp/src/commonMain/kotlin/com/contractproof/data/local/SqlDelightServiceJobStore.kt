package com.contractproof.data.local

import com.contractproof.domain.ServiceJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class SqlDelightServiceJobStore(
    private val database: ContractProofDatabase,
) {
    private val jobQueries = database.serviceJobQueries
    private val requirementQueries = database.serviceJobRequirementQueries

    suspend fun get(organizationId: String, jobId: String): ServiceJob? {
        return withContext(Dispatchers.Default) {
            val row = jobQueries.selectById(organizationId, jobId).executeAsOneOrNull()
                ?: return@withContext null
            val requirements = requirementQueries.selectForJob(organizationId, jobId).executeAsList()
            JobLocalMapper.toDomain(row, requirements)
        }
    }

    suspend fun listAssignedOn(
        organizationId: String,
        serviceDate: LocalDate,
        assigneeUserId: String,
    ): List<ServiceJob> {
        return withContext(Dispatchers.Default) {
            jobQueries.listAssignedOnDate(organizationId, assigneeUserId, serviceDate.toString())
                .executeAsList()
                .map { row ->
                    val requirements = requirementQueries.selectForJob(organizationId, row.id).executeAsList()
                    JobLocalMapper.toDomain(row, requirements)
                }
        }
    }

    suspend fun replaceJobsForOrganization(organizationId: String, jobs: List<ServiceJob>) {
        withContext(Dispatchers.Default) {
            database.transaction {
                jobQueries.deleteAllForOrganization(organizationId)
                jobs.forEach { job ->
                    upsertJobInTransaction(job)
                }
            }
        }
    }

    suspend fun upsert(job: ServiceJob) {
        withContext(Dispatchers.Default) {
            database.transaction {
                upsertJobInTransaction(job)
            }
        }
    }

    private fun upsertJobInTransaction(job: ServiceJob) {
        val values = JobLocalMapper.jobValues(job)
        jobQueries.upsert(
            id = values.id,
            organization_id = values.organizationId,
            client_id = values.clientId,
            client_name = values.clientName,
            location_id = values.locationId,
            location_name = values.locationName,
            location_timezone = values.locationTimezone,
            service_date = values.serviceDate,
            scheduled_start = values.scheduledStart,
            scheduled_end = values.scheduledEnd,
            assignee_user_id = values.assigneeUserId,
            assignee_display_name = values.assigneeDisplayName,
            status = values.status,
            started_at = values.startedAt,
            completed_at = values.completedAt,
            completed_by = values.completedBy,
            contract_id = values.contractId,
            contract_version_id = values.contractVersionId,
            schedule_id = values.scheduleId,
            mandatory_total = values.mandatoryTotal,
            mandatory_satisfied = values.mandatorySatisfied,
            coverage_percent = values.coveragePercent,
            evidence_state = values.evidenceState,
            sync_status = values.syncStatus,
            is_demo = values.isDemo,
        )
        requirementQueries.deleteForJob(job.organizationId, job.id)
        job.requirements.forEach { requirement ->
            val req = JobLocalMapper.requirementValues(job.organizationId, job.id, requirement)
            requirementQueries.upsert(
                id = req.id,
                organization_id = req.organizationId,
                service_job_id = req.serviceJobId,
                contract_requirement_id = req.contractRequirementId,
                requirement_text = req.requirementText,
                requires_photo = req.requiresPhoto,
                is_mandatory = req.isMandatory,
                sort_order = req.sortOrder,
                status = req.status,
            )
        }
    }
}
