package com.contractproof.data

import com.contractproof.domain.Access
import com.contractproof.domain.ContractExtractionV1
import com.contractproof.domain.ExtractionRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabaseExtractionGateway(
    private val client: SupabaseClient,
    private val config: SupabaseConfig,
    private val organizations: OrganizationGateway,
) : ExtractionGateway {
    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun runExtraction(contractVersionId: String, force: Boolean): ExtractionRunResult {
        val membership = organizations.currentMembership() ?: throw ExtractionFailure.Rejected(
            userMessage = "Extraction is not available.",
            retryable = false,
        )
        if (!Access.forMembership(membership.role).canWriteContracts) {
            throw ExtractionFailure.Rejected(
                userMessage = "Only an owner can extract contract requirements.",
                retryable = false,
            )
        }
        try {
            client.auth.awaitInitialization()
            val token = client.auth.currentSessionOrNull()?.accessToken
                ?: throw ExtractionFailure.Rejected(
                    userMessage = "A signed-in owner is required.",
                    retryable = false,
                )
            val response = http.post("${config.url.trimEnd('/')}/functions/v1/extract-contract") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ExtractionRequest(contractVersionId = contractVersionId, force = force))
            }
            val body = response.body<ExtractionFunctionBody>()
            if (body.ok) {
                val versionId = body.contractVersionId ?: contractVersionId
                return ExtractionRunResult(
                    contractVersionId = versionId,
                    requirementCount = body.requirementCount ?: 0,
                    visitCount = body.visitCount ?: 0,
                    warnings = body.warnings ?: emptyList(),
                )
            }
            val error = body.error
                ?: throw ExtractionFailure.Rejected(
                    userMessage = "Extraction failed.",
                    retryable = true,
                )
            throw ExtractionFailure.Rejected(
                userMessage = userMessageForCode(error.code, error.message),
                retryable = error.retryable,
                code = error.code,
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is ExtractionFailure) throw error
            throw if (error.isOfflineFailure()) {
                ExtractionFailure.Network
            } else {
                ExtractionFailure.Rejected(
                    userMessage = "Extraction could not be completed.",
                    retryable = true,
                )
            }
        }
    }

    override suspend fun loadExtraction(contractVersionId: String): ContractExtractionV1? {
        val membership = organizations.currentMembership() ?: throw ExtractionFailure.Rejected(
            userMessage = "Extraction is not available.",
            retryable = false,
        )
        if (!Access.forMembership(membership.role).canOpenContracts) {
            throw ExtractionFailure.Rejected(
                userMessage = "Contracts are not available.",
                retryable = false,
            )
        }
        return try {
            client.from("contract_versions")
                .select {
                    filter {
                        eq("id", contractVersionId)
                        eq("organization_id", membership.organizationId)
                    }
                }
                .decodeList<ExtractionVersionRow>()
                .firstOrNull()
                ?.extraction
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw if (error.isOfflineFailure()) {
                ExtractionFailure.Network
            } else {
                ExtractionFailure.Rejected(
                    userMessage = "Extraction could not be loaded.",
                    retryable = true,
                )
            }
        }
    }

    private fun userMessageForCode(code: String, fallback: String): String {
        return when (code) {
            "TEXT_EXTRACTION_FAILED" -> "No text could be read from this contract PDF."
            "NO_PDF" -> "This version has no contract PDF to read."
            "VALIDATION_FAILED", "LLM_OUTPUT_INVALID" ->
                "The requirements could not be read from the contract."
            "LLM_UNAVAILABLE" -> "The contract reader is temporarily unavailable."
            "VERSION_NOT_IN_REVIEW" -> "This version is no longer in review."
            else -> fallback
        }
    }
}

@Serializable
private data class ExtractionVersionRow(
    val extraction: ContractExtractionV1? = null,
)

@Serializable
private data class ExtractionFunctionBody(
    val ok: Boolean,
    @SerialName("contract_version_id") val contractVersionId: String? = null,
    val status: String? = null,
    @SerialName("requirement_count") val requirementCount: Int? = null,
    @SerialName("visit_count") val visitCount: Int? = null,
    val warnings: List<String>? = null,
    val error: ExtractionFunctionError? = null,
)

@Serializable
private data class ExtractionFunctionError(
    val code: String,
    val message: String,
    val retryable: Boolean,
)
