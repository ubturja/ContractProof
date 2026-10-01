package com.contractproof.data

import com.contractproof.domain.ContractExtractionV1

data class ExtractionRunResult(
    val contractVersionId: String,
    val requirementCount: Int,
    val visitCount: Int,
    val warnings: List<String>,
)

sealed class ExtractionFailure : Exception() {
    data object Network : ExtractionFailure()

    data class Rejected(
        val userMessage: String,
        val retryable: Boolean,
        val code: String? = null,
    ) : ExtractionFailure() {
        override val message: String get() = userMessage
    }
}

interface ExtractionGateway {
    suspend fun runExtraction(contractVersionId: String, force: Boolean = false): ExtractionRunResult

    suspend fun loadExtraction(contractVersionId: String): ContractExtractionV1?
}
