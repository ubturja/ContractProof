package com.contractproof.data

import com.contractproof.data.local.SqlDelightServiceJobStore
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import kotlinx.datetime.LocalDate

class OfflineFirstServiceJobRepository(
    private val remote: ServiceJobRepository,
    private val local: SqlDelightServiceJobStore,
    private val organizations: OrganizationGateway,
) : ServiceJobRepository {
    override suspend fun generateForApprovedVersion(
        contractId: String,
        contractVersionId: String,
        horizonDays: Int,
    ) {
        remote.generateForApprovedVersion(contractId, contractVersionId, horizonDays)
    }

    override suspend fun get(jobId: String): ServiceJob? {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        val cached = local.get(membership.organizationId, jobId)
        return try {
            val remoteJob = remote.get(jobId)
            if (remoteJob != null) {
                local.upsert(remoteJob.copy(syncStatus = EvidenceSyncStatus.Uploaded))
            }
            remoteJob ?: cached
        } catch (error: ServiceJobFailure) {
            if (error == ServiceJobFailure.Network) {
                return cached
            }
            throw error
        }
    }

    override suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob> {
        return try {
            remote.listForOrganizationOn(serviceDate)
        } catch (error: ServiceJobFailure) {
            if (error == ServiceJobFailure.Network) {
                return emptyList()
            }
            throw error
        }
    }

    override suspend fun listForContract(
        contractId: String,
        fromDate: LocalDate,
        limit: Int,
    ): List<ServiceJob> {
        return try {
            remote.listForContract(contractId, fromDate, limit)
        } catch (error: ServiceJobFailure) {
            if (error == ServiceJobFailure.Network) {
                return emptyList()
            }
            throw error
        }
    }

    override suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob> {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        val cached = local.listAssignedOn(membership.organizationId, serviceDate, assigneeUserId)
        return try {
            val remoteJobs = remote.listAssignedOn(serviceDate, assigneeUserId)
            local.replaceJobsForOrganization(
                membership.organizationId,
                remoteJobs.map { it.copy(syncStatus = EvidenceSyncStatus.Uploaded) },
            )
            remoteJobs
        } catch (error: ServiceJobFailure) {
            if (error == ServiceJobFailure.Network) {
                if (cached.isNotEmpty()) {
                    return cached
                }
            }
            throw error
        }
    }

    override suspend fun start(jobId: String, startedAt: String): ServiceJob {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        val existing = local.get(membership.organizationId, jobId)
        return try {
            val updated = remote.start(jobId, startedAt)
            local.upsert(updated.copy(syncStatus = EvidenceSyncStatus.Uploaded))
            updated
        } catch (error: ServiceJobFailure) {
            if (error == ServiceJobFailure.Network && existing != null) {
                val optimistic = existing.copy(
                    status = ServiceJobRules.InProgress,
                    startedAt = startedAt,
                    syncStatus = EvidenceSyncStatus.Pending,
                )
                local.upsert(optimistic)
                return optimistic
            }
            throw error
        }
    }

    override suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob {
        val membership = organizations.currentMembership() ?: throw ServiceJobFailure.Rejected
        val existing = local.get(membership.organizationId, jobId)
        return try {
            val updated = remote.complete(jobId, completedAt, completedBy)
            local.upsert(updated.copy(syncStatus = EvidenceSyncStatus.Uploaded))
            updated
        } catch (error: ServiceJobFailure) {
            if (error == ServiceJobFailure.Network && existing != null) {
                val optimistic = existing.copy(
                    status = ServiceJobRules.Completed,
                    completedAt = completedAt,
                    completedBy = completedBy,
                    syncStatus = EvidenceSyncStatus.Pending,
                )
                local.upsert(optimistic)
                return optimistic
            }
            throw error
        }
    }

    override suspend fun markIncomplete(jobId: String): ServiceJob {
        return remote.markIncomplete(jobId)
    }

    override suspend fun markDisputed(jobId: String): ServiceJob {
        return remote.markDisputed(jobId)
    }
}
