package com.contractproof.core.analytics

private val blockedPropertyKeys = setOf(
    "password",
    "token",
    "authorization",
    "email",
    "note",
    "body",
    "document",
    "path",
    "name",
    "address",
)

object AnalyticsSanitizer {
    fun sanitizeProperties(properties: Map<String, String>): Map<String, String> {
        return properties.filterKeys { key ->
            val normalized = key.lowercase()
            blockedPropertyKeys.none { blocked -> normalized.contains(blocked) }
        }
    }
}
