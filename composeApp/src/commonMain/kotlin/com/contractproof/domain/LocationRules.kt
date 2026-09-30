package com.contractproof.domain

data class LocationRecord(
    val id: String,
    val organizationId: String,
    val clientId: String,
    val name: String,
    val timezone: String,
    val address: String?,
    val zoneCode: String?,
    val status: String,
)

class LocationRuleViolation(message: String) : IllegalArgumentException(message)

object LocationRules {
    const val Active = "active"
    const val Archived = "archived"

    fun requireName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw LocationRuleViolation("Location name is required.")
        }
        return trimmed
    }

    fun requireTimezone(timezone: String): String {
        val trimmed = timezone.trim()
        if (trimmed.isEmpty()) {
            throw LocationRuleViolation("Timezone is required.")
        }
        return trimmed
    }

    fun optionalText(value: String): String? {
        return value.trim().ifEmpty { null }
    }

    fun requireOrganization(organizationId: String, membershipOrganizationId: String) {
        if (organizationId != membershipOrganizationId) {
            throw LocationRuleViolation("A location belongs to exactly one organization.")
        }
    }

    fun requireClient(clientOrganizationId: String, membershipOrganizationId: String) {
        if (clientOrganizationId != membershipOrganizationId) {
            throw LocationRuleViolation("A location's client must be in the same organization.")
        }
    }

    fun requireStatus(status: String) {
        if (status != Active && status != Archived) {
            throw LocationRuleViolation("Location status is invalid.")
        }
    }

    fun requireWrite(access: Access) {
        if (!access.canAddLocation) {
            throw LocationRuleViolation("Only an owner can change locations.")
        }
    }

    fun visibleTo(locations: List<LocationRecord>, role: Role, locationIds: List<String>): List<LocationRecord> {
        return when (role) {
            Role.Owner -> locations
            Role.Manager -> locations.filter { role.allowsLocation(it.id, locationIds) }
            Role.Cleaner, Role.Client -> emptyList()
        }
    }

    fun forClient(locations: List<LocationRecord>, clientId: String): List<LocationRecord> {
        return locations.filter { it.clientId == clientId }
    }

    fun archived(location: LocationRecord): LocationRecord {
        return location.copy(status = Archived)
    }
}
