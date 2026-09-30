package com.contractproof.domain

data class ClientRecord(
    val id: String,
    val organizationId: String,
    val name: String,
    val status: String,
)

class ClientRuleViolation(message: String) : IllegalArgumentException(message)

object ClientRules {
    const val Active = "active"
    const val Archived = "archived"

    fun requireName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw ClientRuleViolation("Client name is required.")
        }
        return trimmed
    }

    fun requireOrganization(organizationId: String, membershipOrganizationId: String) {
        if (organizationId != membershipOrganizationId) {
            throw ClientRuleViolation("A client belongs to exactly one organization.")
        }
    }

    fun requireStatus(status: String) {
        if (status != Active && status != Archived) {
            throw ClientRuleViolation("Client status is invalid.")
        }
    }

    fun requireWrite(access: Access) {
        if (!access.canWriteClients) {
            throw ClientRuleViolation("Only an owner can change clients.")
        }
    }

    fun search(clients: List<ClientRecord>, query: String): List<ClientRecord> {
        val needle = query.trim()
        if (needle.isEmpty()) {
            return clients
        }
        return clients.filter { client ->
            client.name.contains(needle, ignoreCase = true)
        }
    }

    fun archived(client: ClientRecord): ClientRecord {
        return client.copy(status = Archived)
    }
}
