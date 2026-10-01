package com.contractproof.data

sealed class ExceptionFailure : Exception() {
    data object Network : ExceptionFailure()

    data object Rejected : ExceptionFailure()
}

data class TaskExceptionResult(
    val uploaded: Boolean,
    val pending: Boolean,
)

interface ExceptionGateway {
    suspend fun submit(
        jobId: String,
        requirementId: String,
        reason: String,
        recordedAt: String,
    ): TaskExceptionResult
}
