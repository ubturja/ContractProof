package com.contractproof.core.observability

enum class ErrorLevel {
    Info,
    Warning,
    Error,
}

interface ErrorReporter {
    fun captureThrowable(
        throwable: Throwable,
        tags: Map<String, String> = emptyMap(),
        fingerprint: String? = null,
    )

    fun captureMessage(
        message: String,
        level: ErrorLevel,
        tags: Map<String, String> = emptyMap(),
    )

    fun addBreadcrumb(category: String, message: String)
}
