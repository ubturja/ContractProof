package com.contractproof.core.observability

import kotlin.test.Test

class NoOpErrorReporterTest {
    @Test
    fun noOpDoesNotThrow() {
        val reporter = NoOpErrorReporter()
        reporter.captureThrowable(IllegalStateException("test"))
        reporter.captureMessage("hello", ErrorLevel.Warning)
        reporter.addBreadcrumb("test", "breadcrumb")
    }
}
