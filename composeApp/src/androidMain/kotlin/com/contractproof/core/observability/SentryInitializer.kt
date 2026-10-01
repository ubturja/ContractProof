package com.contractproof.core.observability

import android.content.Context
import android.util.Log
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

object SentryInitializer {
    private const val TAG = "Sentry"

    fun configure(applicationContext: Context): Boolean {
        val dsn = SentryLocalConfig.dsn.trim()
        if (dsn.isEmpty()) {
            Log.i(TAG, "DSN missing; Sentry disabled")
            return false
        }
        return try {
            SentryAndroid.init(applicationContext) { options ->
                options.dsn = dsn
                options.environment = SentryLocalConfig.environment
                options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    scrubEvent(event)
                }
            }
            Log.i(TAG, "Sentry configured")
            true
        } catch (error: Throwable) {
            Log.e(TAG, "Sentry configure failed", error)
            false
        }
    }

    private fun scrubEvent(event: SentryEvent): SentryEvent? {
        event.breadcrumbs?.forEach { breadcrumb ->
            breadcrumb.message = ObservabilitySanitizer.sanitizeMessage(breadcrumb.message ?: "")
        }
        event.tags = ObservabilitySanitizer.sanitizeTags(event.tags ?: emptyMap())
        return event
    }
}

class SentryErrorReporter : ErrorReporter {
    override fun captureThrowable(
        throwable: Throwable,
        tags: Map<String, String>,
        fingerprint: String?,
    ) {
        Sentry.withScope { scope ->
            ObservabilitySanitizer.sanitizeTags(tags).forEach { (key, value) ->
                scope.setTag(key, value)
            }
            fingerprint?.let { scope.fingerprint = listOf(it) }
            Sentry.captureException(throwable)
        }
    }

    override fun captureMessage(message: String, level: ErrorLevel, tags: Map<String, String>) {
        val sentryLevel = when (level) {
            ErrorLevel.Info -> SentryLevel.INFO
            ErrorLevel.Warning -> SentryLevel.WARNING
            ErrorLevel.Error -> SentryLevel.ERROR
        }
        Sentry.withScope { scope ->
            ObservabilitySanitizer.sanitizeTags(tags).forEach { (key, value) ->
                scope.setTag(key, value)
            }
            Sentry.captureMessage(ObservabilitySanitizer.sanitizeMessage(message), sentryLevel)
        }
    }

    override fun addBreadcrumb(category: String, message: String) {
        Sentry.addBreadcrumb(
            io.sentry.Breadcrumb().apply {
                this.category = category
                this.message = ObservabilitySanitizer.sanitizeMessage(message)
            },
        )
    }
}
