package com.contractproof.core.observability

class NoOpErrorReporter : ErrorReporter {
    override fun captureThrowable(
        throwable: Throwable,
        tags: Map<String, String>,
        fingerprint: String?,
    ) {
        // Disabled error reporting.
    }

    override fun captureMessage(message: String, level: ErrorLevel, tags: Map<String, String>) {
        // Disabled error reporting.
    }

    override fun addBreadcrumb(category: String, message: String) {
        // Disabled error reporting.
    }
}
