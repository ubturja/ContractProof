package com.contractproof.domain

enum class Role {
    Owner,
    Manager,
    Cleaner,
    Client,
    ;

    fun allowsLocation(locationId: String, locationIds: List<String>): Boolean {
        return when (this) {
            Owner -> true
            Manager -> locationIds.contains(locationId)
            Cleaner, Client -> false
        }
    }

    companion object {
        fun from(value: String?): Role {
            return when (value) {
                "owner" -> Owner
                "manager" -> Manager
                "cleaner" -> Cleaner
                else -> Client
            }
        }
    }
}

enum class Home {
    Dashboard,
    Today,
    ClientHold,
}

data class Access(
    val home: Home,
    val canCreateCompany: Boolean,
    val canOpenDashboard: Boolean,
    val canAddLocation: Boolean,
    val canOpenLocations: Boolean,
    val canOpenService: Boolean,
    val canOpenContracts: Boolean,
    val canWriteContracts: Boolean,
    val canOpenDisputes: Boolean,
    val canWriteDisputes: Boolean,
    val canOpenClients: Boolean,
    val canWriteClients: Boolean,
    val canManageOrganization: Boolean,
    val canManageSubscription: Boolean,
    val canInviteMembers: Boolean,
    val canOpenAssignedJobs: Boolean,
    val canOpenClientServiceRecords: Boolean,
    val canFileClientDispute: Boolean,
    val canOpenSettings: Boolean,
) {
    companion object {
        val unsigned = Access(
            home = Home.Dashboard,
            canCreateCompany = true,
            canOpenDashboard = false,
            canAddLocation = false,
            canOpenLocations = false,
            canOpenService = false,
            canOpenContracts = false,
            canWriteContracts = false,
            canOpenDisputes = false,
            canWriteDisputes = false,
            canOpenClients = false,
            canWriteClients = false,
            canManageOrganization = false,
            canManageSubscription = false,
            canInviteMembers = false,
            canOpenAssignedJobs = false,
            canOpenClientServiceRecords = false,
            canFileClientDispute = false,
            canOpenSettings = false,
        )

        fun forRole(role: Role): Access {
            return when (role) {
                Role.Owner -> Access(
                    home = Home.Dashboard,
                    canCreateCompany = false,
                    canOpenDashboard = true,
                    canAddLocation = true,
                    canOpenLocations = true,
                    canOpenService = true,
                    canOpenContracts = true,
                    canWriteContracts = true,
                    canOpenDisputes = true,
                    canWriteDisputes = true,
                    canOpenClients = true,
                    canWriteClients = true,
                    canManageOrganization = true,
                    canManageSubscription = true,
                    canInviteMembers = true,
                    canOpenAssignedJobs = true,
                    canOpenClientServiceRecords = false,
                    canFileClientDispute = false,
                    canOpenSettings = true,
                )
                Role.Manager -> Access(
                    home = Home.Dashboard,
                    canCreateCompany = false,
                    canOpenDashboard = true,
                    canAddLocation = false,
                    canOpenLocations = true,
                    canOpenService = true,
                    canOpenContracts = true,
                    canWriteContracts = false,
                    canOpenDisputes = true,
                    canWriteDisputes = false,
                    canOpenClients = true,
                    canWriteClients = false,
                    canManageOrganization = false,
                    canManageSubscription = false,
                    canInviteMembers = false,
                    canOpenAssignedJobs = false,
                    canOpenClientServiceRecords = false,
                    canFileClientDispute = false,
                    canOpenSettings = true,
                )
                Role.Cleaner -> Access(
                    home = Home.Today,
                    canCreateCompany = false,
                    canOpenDashboard = false,
                    canAddLocation = false,
                    canOpenLocations = false,
                    canOpenService = false,
                    canOpenContracts = false,
                    canWriteContracts = false,
                    canOpenDisputes = false,
                    canWriteDisputes = false,
                    canOpenClients = false,
                    canWriteClients = false,
                    canManageOrganization = false,
                    canManageSubscription = false,
                    canInviteMembers = false,
                    canOpenAssignedJobs = true,
                    canOpenClientServiceRecords = false,
                    canFileClientDispute = false,
                    canOpenSettings = true,
                )
                Role.Client -> Access(
                    home = Home.ClientHold,
                    canCreateCompany = false,
                    canOpenDashboard = false,
                    canAddLocation = false,
                    canOpenLocations = false,
                    canOpenService = false,
                    canOpenContracts = false,
                    canWriteContracts = false,
                    canOpenDisputes = false,
                    canWriteDisputes = false,
                    canOpenClients = false,
                    canWriteClients = false,
                    canManageOrganization = false,
                    canManageSubscription = false,
                    canInviteMembers = false,
                    canOpenAssignedJobs = false,
                    canOpenClientServiceRecords = true,
                    canFileClientDispute = true,
                    canOpenSettings = true,
                )
            }
        }

        fun forMembership(membershipRole: String?): Access {
            return forRole(Role.from(membershipRole))
        }
    }
}
