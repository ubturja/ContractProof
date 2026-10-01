package com.contractproof.domain

class ExceptionRuleViolation(message: String) : IllegalArgumentException(message)

object ExceptionRules {
    const val AreaBlocked = "Area blocked"
    const val ClientRefusedAccess = "Client refused access"
    const val SafetyConcern = "Safety concern"
    const val CouldNotComplete = "Could not complete"
    const val Other = "Other"

    val reasons: List<String> = listOf(
        AreaBlocked,
        ClientRefusedAccess,
        SafetyConcern,
        CouldNotComplete,
        Other,
    )

    fun requireReason(reason: String): String {
        val trimmed = reason.trim()
        if (trimmed !in reasons) {
            throw ExceptionRuleViolation("Choose an exception reason.")
        }
        return trimmed
    }

    fun formatReason(reason: String, note: String): String {
        val base = requireReason(reason)
        val trimmedNote = note.trim()
        if (trimmedNote.isEmpty()) {
            if (base == Other) {
                throw ExceptionRuleViolation("Add a short note for this exception.")
            }
            return base
        }
        return "$base — $trimmedNote"
    }
}
