package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.EvidenceReportAssemblyRules
import com.contractproof.domain.EvidenceReportRecord
import com.contractproof.domain.ReportGenerationRequest
import com.contractproof.domain.ReportGenerationResult
import com.contractproof.domain.ReportRules
import com.contractproof.domain.ReportStatus
import com.contractproof.domain.ServiceEvidenceReport
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabaseReportGateway(
    private val client: SupabaseClient,
    private val config: SupabaseConfig,
    private val organizations: OrganizationGateway,
    private val disputes: DisputeGateway,
) : ReportGateway {
    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun getForDispute(disputeId: String): EvidenceReportRecord? {
        val membership = organizations.currentMembership() ?: throw ReportFailure.Rejected("Sign in required.")
        ReportRules.requireCanRequestReport(Access.forMembership(membership.role))
        return try {
            client.from("reports")
                .select {
                    filter {
                        eq("dispute_id", disputeId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ReportRow>()
                .firstOrNull()
                ?.let { ReportMapper.record(it) }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapFailure(error)
        }
    }

    override suspend fun assemblePreview(disputeId: String): ServiceEvidenceReport? {
        val bundle = disputes.getReconstruction(disputeId) ?: return null
        return EvidenceReportAssemblyRules.build(bundle)
    }

    override suspend fun requestGeneration(request: ReportGenerationRequest): ReportGenerationResult {
        val membership = organizations.currentMembership() ?: throw ReportFailure.Rejected("Sign in required.")
        ReportRules.requireCanRequestReport(Access.forMembership(membership.role))
        ReportRules.requireRequest(request)
        try {
            client.auth.awaitInitialization()
            val token = client.auth.currentSessionOrNull()?.accessToken
                ?: throw ReportFailure.Rejected("Sign in required.")
            val response = http.post("${config.url.trimEnd('/')}/functions/v1/generate-dispute-report") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateReportRequest(
                        disputeId = request.disputeId,
                        force = request.forceRegenerate,
                    ),
                )
            }
            val body = response.body<GenerateReportResponse>()
            if (!body.ok || body.report == null) {
                throw ReportFailure.Rejected(body.error?.message ?: "Report generation failed.")
            }
            val report = ReportMapper.record(body.report)
            return ReportGenerationResult(report = report, signedUrl = body.signedUrl)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ReportFailure) throw error
            throw mapFailure(error)
        }
    }

    override suspend fun signedPdfUrl(objectPath: String): String {
        val membership = organizations.currentMembership() ?: throw ReportFailure.Rejected("Sign in required.")
        ReportRules.requireCanRequestReport(Access.forMembership(membership.role))
        if (!objectPath.startsWith("${membership.organizationId}/")) {
            throw ReportFailure.Rejected("Report path is invalid.")
        }
        return try {
            client.storage.from(REPORTS_BUCKET).createSignedUrl(path = objectPath, expiresIn = 15.minutes)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapFailure(error)
        }
    }

    private fun mapFailure(error: Throwable): ReportFailure {
        return if (error.isOfflineFailure()) {
            ReportFailure.Network
        } else if (error is ReportFailure) {
            error
        } else {
            ReportFailure.Rejected(error.message ?: "Report request failed.")
        }
    }

    private companion object {
        const val REPORTS_BUCKET = "reports"
    }
}

@Serializable
private data class GenerateReportRequest(
    @SerialName("dispute_id") val disputeId: String,
    val force: Boolean = false,
)

@Serializable
private data class GenerateReportResponse(
    val ok: Boolean,
    val report: ReportRow? = null,
    @SerialName("signed_url") val signedUrl: String? = null,
    val error: GenerateReportError? = null,
)

@Serializable
private data class GenerateReportError(
    val message: String,
)
