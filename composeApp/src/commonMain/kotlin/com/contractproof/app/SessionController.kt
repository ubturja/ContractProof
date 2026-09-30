package com.contractproof.app

import com.contractproof.data.AuthGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.Home
import com.contractproof.feature.auth.AuthController
import com.contractproof.feature.auth.AuthPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SessionDestination {
    Restoring,
    SignedOut,
    CompanySetup,
    Dashboard,
    Today,
    ClientHold,
}

data class CompanySetupState(
    val companyName: String = "",
    val displayName: String = "",
    val saving: Boolean = false,
    val banner: String? = null,
) {
    val canSubmit: Boolean
        get() = companyName.trim().isNotEmpty() && displayName.trim().isNotEmpty() && !saving
}

class SessionController(
    val auth: AuthController,
    private val organizations: OrganizationGateway,
    private val users: AuthGateway,
) {
    private val destinationState = MutableStateFlow(SessionDestination.Restoring)
    val destination: StateFlow<SessionDestination> = destinationState.asStateFlow()

    private val membershipState = MutableStateFlow<Membership?>(null)
    val membership: StateFlow<Membership?> = membershipState.asStateFlow()

    private val setupState = MutableStateFlow(CompanySetupState())
    val setup: StateFlow<CompanySetupState> = setupState.asStateFlow()

    val access: Access
        get() = membershipState.value?.let { Access.forMembership(it.role) } ?: Access.unsigned

    fun updateCompanyName(value: String) {
        setupState.update { it.copy(companyName = value) }
    }

    fun updateDisplayName(value: String) {
        setupState.update { it.copy(displayName = value) }
    }

    suspend fun restore() {
        destinationState.value = SessionDestination.Restoring
        auth.restore()
        resolveDestination()
    }

    suspend fun signIn() {
        auth.signIn()
        resolveDestination()
    }

    suspend fun signUp() {
        auth.signUp()
        resolveDestination()
    }

    suspend fun requestPasswordReset() {
        auth.requestPasswordReset()
    }

    suspend fun signOut() {
        auth.signOut()
        membershipState.value = null
        setupState.value = CompanySetupState()
        destinationState.value = SessionDestination.SignedOut
    }

    suspend fun createCompany() {
        if (membershipState.value != null || !Access.unsigned.canCreateCompany) {
            return
        }
        val current = setupState.value
        if (!current.canSubmit) {
            return
        }
        setupState.update {
            it.copy(
                companyName = current.companyName.trim(),
                displayName = current.displayName.trim(),
                saving = true,
                banner = null,
            )
        }
        try {
            val created = organizations.createOwnerOrganization(
                companyName = setupState.value.companyName,
                displayName = setupState.value.displayName,
            )
            membershipState.value = created
            setupState.update { it.copy(saving = false, banner = null) }
            destinationState.value = destinationFor(created)
        } catch (failure: OrganizationFailure) {
            setupState.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        OrganizationFailure.Network ->
                            "You need a connection to create the company."
                        OrganizationFailure.Rejected ->
                            "The company was not saved."
                    },
                )
            }
        }
    }

    private suspend fun resolveDestination() {
        if (auth.state.value.phase != AuthPhase.SignedIn) {
            membershipState.value = null
            destinationState.value = SessionDestination.SignedOut
            return
        }
        if (users.currentUser() == null) {
            membershipState.value = null
            destinationState.value = SessionDestination.SignedOut
            return
        }
        val found = try {
            organizations.currentMembership()
        } catch (_: OrganizationFailure) {
            null
        }
        membershipState.value = found
        destinationState.value = destinationFor(found)
    }
}

internal fun destinationFor(membership: Membership?): SessionDestination {
    if (membership == null) {
        return SessionDestination.CompanySetup
    }
    return when (Access.forMembership(membership.role).home) {
        Home.Dashboard -> SessionDestination.Dashboard
        Home.Today -> SessionDestination.Today
        Home.ClientHold -> SessionDestination.ClientHold
    }
}
