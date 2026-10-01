package com.contractproof.core.observability

private val blockedKeys = setOf(
    "password",
    "token",
    "authorization",
    "email",
    "note",
    "body",
    "document",
    "path",
)

object ObservabilitySanitizer {
    fun sanitizeTags(tags: Map<String, String>): Map<String, String> {
        return tags.filterKeys { key ->
            val normalized = key.lowercase()
            blockedKeys.none { blocked -> normalized.contains(blocked) }
        }
    }

    fun sanitizeMessage(message: String): String {
        if (message.length > 200) {
            return message.take(200)
        }
        return message
    }
}
