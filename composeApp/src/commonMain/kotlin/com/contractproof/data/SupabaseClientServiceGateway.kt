package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import com.contractproof.domain.ServiceRecordAssemblyRules
import com.contractproof.domain.ServiceRecordSnapshot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable

class SupabaseClientServiceGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
    private val serviceJobs: ServiceJobRepository,
    private val evidence: EvidenceRepository,
) : ClientServiceGateway {
    override suspend fun listCompletedForClient(): List<ServiceJob> {
        val membership = organizations.currentMembership() ?: throw ClientServiceFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenClientServiceRecords) {
            throw ClientServiceFailure.Rejected
        }
        return try {
            val rows = client.from("service_jobs")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                        isIn("status", listOf(ServiceJobRules.Completed, ServiceJobRules.Disputed))
                    }
                }
                .decodeList<ServiceJobIdRow>()
            rows.mapNotNull { serviceJobs.get(it.id) }
                .sortedByDescending { it.serviceDate.toString() }
                .take(30)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw if (error.isOfflineFailure()) ClientServiceFailure.Network else ClientServiceFailure.Rejected
        }
    }

    override suspend fun loadRecord(jobId: String): ServiceRecordSnapshot? {
        val membership = organizations.currentMembership() ?: throw ClientServiceFailure.Rejected
        if (!Access.forMembership(membership.role).canOpenClientServiceRecords) {
            throw ClientServiceFailure.Rejected
        }
        val job = serviceJobs.get(jobId) ?: return null
        if (job.status != ServiceJobRules.Completed && job.status != ServiceJobRules.Disputed) {
            return null
        }
        return try {
            val evidenceItems = evidence.listForJob(membership.organizationId, jobId)
            val exceptions = client.from("exceptions")
                .select {
                    filter {
                        eq("organization_id", membership.organizationId)
                        eq("service_job_id", jobId)
                    }
                }
                .decodeList<ExceptionRecordRow>()
                .map { DisputeMapper.exception(it) }
            ServiceRecordAssemblyRules.assemble(job, evidenceItems, exceptions)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw if (error.isOfflineFailure()) ClientServiceFailure.Network else ClientServiceFailure.Rejected
        }
    }
}

@Serializable
private data class ServiceJobIdRow(val id: String)
