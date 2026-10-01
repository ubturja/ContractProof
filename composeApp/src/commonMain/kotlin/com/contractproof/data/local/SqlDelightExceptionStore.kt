package com.contractproof.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightExceptionStore(
    private val database: ContractProofDatabase,
) {
    private val queries = database.taskExceptionQueries

    suspend fun getByRequirement(organizationId: String, requirementId: String): LocalTaskException? {
        return withContext(Dispatchers.Default) {
            queries.selectByRequirement(organizationId, requirementId)
                .executeAsOneOrNull()
                ?.let { ExceptionLocalMapper.toDomain(it) }
        }
    }

    suspend fun listForJob(organizationId: String, jobId: String): List<LocalTaskException> {
        return withContext(Dispatchers.Default) {
            queries.selectForJob(organizationId, jobId).executeAsList().map {
                ExceptionLocalMapper.toDomain(it)
            }
        }
    }

    suspend fun upsert(exception: LocalTaskException) {
        withContext(Dispatchers.Default) {
            val values = ExceptionLocalMapper.values(exception)
            queries.upsert(
                id = values.id,
                organization_id = values.organizationId,
                service_job_id = values.serviceJobId,
                service_job_requirement_id = values.serviceJobRequirementId,
                recorded_by = values.recordedBy,
                recorded_at = values.recordedAt,
                reason = values.reason,
                sync_status = values.syncStatus,
            )
        }
    }

    suspend fun deleteForRequirement(organizationId: String, requirementId: String) {
        withContext(Dispatchers.Default) {
            queries.deleteForRequirement(organizationId, requirementId)
        }
    }
}
