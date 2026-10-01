package com.contractproof.data

import com.contractproof.domain.JobRequirementStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseExceptionGateway(
    private val client: SupabaseClient,
    private val organizations: OrganizationGateway,
    private val auth: AuthGateway,
) : ExceptionGateway {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun submit(
        jobId: String,
        requirementId: String,
        reason: String,
        recordedAt: String,
    ): TaskExceptionResult {
        val membership = organizations.currentMembership() ?: throw ExceptionFailure.Rejected
        val user = auth.currentUser() ?: throw ExceptionFailure.Rejected
        val exceptionId = Uuid.generateV4().toString().lowercase()
        try {
            client.from("exceptions").insert(
                ExceptionInsert(
                    id = exceptionId,
                    organizationId = membership.organizationId,
                    serviceJobId = jobId,
                    serviceJobRequirementId = requirementId,
                    recordedBy = user.id,
                    recordedAt = recordedAt,
                    reason = reason,
                    syncStatus = SYNC_UPLOADED,
                ),
            )
            client.from("service_job_requirements").update(
                {
                    set("status", JobRequirementStatus.STORAGE_EXCEPTION)
                },
            ) {
                filter {
                    eq("id", requirementId)
                    eq("organization_id", membership.organizationId)
                }
            }
            return TaskExceptionResult(uploaded = true, pending = false)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ExceptionFailure) throw error
            throw if (error.isOfflineFailure()) ExceptionFailure.Network else ExceptionFailure.Rejected
        }
    }

    private companion object {
        const val SYNC_UPLOADED = "uploaded"
    }
}

@Serializable
private data class ExceptionInsert(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("service_job_id") val serviceJobId: String,
    @SerialName("service_job_requirement_id") val serviceJobRequirementId: String,
    @SerialName("recorded_by") val recordedBy: String,
    @SerialName("recorded_at") val recordedAt: String,
    val reason: String,
    @SerialName("sync_status") val syncStatus: String,
)
