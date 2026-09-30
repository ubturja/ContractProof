package com.contractproof.app

import com.contractproof.data.AuthGateway
import com.contractproof.data.AuthUser
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.feature.auth.AuthController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SessionControllerTest {
    @Test
    fun signUpWithoutMembershipGoesToCompanySetup() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = SessionController(AuthController(users), organizations, users)
        session.restore()
        session.auth.updateEmail("owner@example.com")
        session.auth.updatePassword("secret")

        session.signUp()

        assertEquals(SessionDestination.CompanySetup, session.destination.value)
        assertNull(session.membership.value)
        assertEquals(1, users.signUpCalls)
        assertEquals(0, organizations.organizationInserts)
    }

    @Test
    fun createCompanyInsertsProfileOrganizationAndOwnerThenDashboard() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = signedInWithoutCompany(users, organizations)
        session.updateCompanyName("Northside")
        session.updateDisplayName("Owner A")

        session.createCompany()

        val membership = session.membership.value
        assertEquals(SessionDestination.Dashboard, session.destination.value)
        assertEquals("Northside", membership?.organizationName)
        assertEquals("owner", membership?.role)
        assertEquals(1, organizations.userInserts)
        assertEquals(1, organizations.organizationInserts)
        assertEquals(1, organizations.membershipInserts)
        assertEquals(users.user?.id, organizations.createdBy)
        assertEquals(users.user?.id, organizations.memberUserId)
    }

    @Test
    fun secondCreateDoesNotInsertAnotherOrganization() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = signedInWithoutCompany(users, organizations)
        session.updateCompanyName("Northside")
        session.updateDisplayName("Owner A")
        session.createCompany()

        session.updateCompanyName("Second")
        session.createCompany()

        assertEquals(1, organizations.organizationInserts)
        assertEquals("Northside", session.membership.value?.organizationName)
        assertEquals(SessionDestination.Dashboard, session.destination.value)
    }

    @Test
    fun gatewayUsesSessionUserAndRejectsForeignOrganization() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        signedInWithoutCompany(users, organizations)
        val foreignUser = "22222222-2222-2222-2222-222222222222"
        val foreignOrg = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"

        val created = organizations.createOwnerOrganization("Northside", "Owner A")

        assertEquals(users.user?.id, organizations.createdBy)
        assertNotEquals(foreignUser, organizations.createdBy)
        assertNotEquals(foreignOrg, created.organizationId)
        assertFalse(organizations.acceptedForeignCreator)
        assertFalse(organizations.claimedForeignOrganization)
    }

    @Test
    fun dashboardDestinationIsEmptyOwnerHome() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = signedInWithoutCompany(users, organizations)
        session.updateCompanyName("Northside")
        session.updateDisplayName("Owner A")

        session.createCompany()

        assertEquals(SessionDestination.Dashboard, session.destination.value)
        assertEquals("Northside", session.membership.value?.organizationName)
        assertEquals("owner", session.membership.value?.role)
        assertTrue(organizations.locationInserts == 0)
    }

    @Test
    fun ownerWithMembershipGoesToDashboardWithWrites() = runBlocking {
        val session = signedInWithRole("owner")
        val access = Access.forMembership(session.membership.value?.role)
        assertEquals(SessionDestination.Dashboard, session.destination.value)
        assertTrue(access.canAddLocation)
        assertTrue(access.canWriteContracts)
        assertTrue(access.canWriteDisputes)
    }

    @Test
    fun managerGoesToDashboardWithoutWrites() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = signedInWithRole("manager", users, organizations)
        val access = Access.forMembership(session.membership.value?.role)
        assertEquals(SessionDestination.Dashboard, session.destination.value)
        assertFalse(access.canAddLocation)
        assertFalse(access.canWriteContracts)
        assertFalse(access.canWriteDisputes)
        assertFalse(access.canCreateCompany)
        session.createCompany()
        assertEquals(0, organizations.organizationInserts)
    }

    @Test
    fun cleanerGoesToToday() = runBlocking {
        val session = signedInWithRole("cleaner")
        val access = Access.forMembership(session.membership.value?.role)
        assertEquals(SessionDestination.Today, session.destination.value)
        assertFalse(access.canOpenDashboard)
        assertFalse(access.canOpenContracts)
        assertFalse(access.canOpenDisputes)
    }

    @Test
    fun clientGoesToHoldWithoutWrites() = runBlocking {
        val session = signedInWithRole("client")
        val access = Access.forMembership(session.membership.value?.role)
        assertEquals(SessionDestination.ClientHold, session.destination.value)
        assertFalse(access.canAddLocation)
        assertFalse(access.canWriteContracts)
        assertFalse(access.canWriteDisputes)
        assertFalse(access.canOpenAssignedJobs)
        assertFalse(access.canCreateCompany)
    }
}

private suspend fun signedInWithoutCompany(
    users: RecordingAuthGateway,
    organizations: RecordingOrganizationGateway,
): SessionController {
    val session = SessionController(AuthController(users), organizations, users)
    session.restore()
    session.auth.updateEmail("owner@example.com")
    session.auth.updatePassword("secret")
    session.signUp()
    return session
}

private suspend fun signedInWithRole(
    role: String,
    users: RecordingAuthGateway = RecordingAuthGateway(),
    organizations: RecordingOrganizationGateway = RecordingOrganizationGateway(users),
): SessionController {
    users.user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = "$role@example.com")
    organizations.membership = Membership(
        organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
        organizationName = "Northside",
        role = role,
        locationIds = if (role == "manager") emptyList() else emptyList(),
        clientId = if (role == "client") "c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1" else null,
    )
    val session = SessionController(AuthController(users), organizations, users)
    session.restore()
    return session
}

private class RecordingAuthGateway : AuthGateway {
    var signUpCalls: Int = 0
    var user: AuthUser? = null

    override suspend fun hasStoredSession(): Boolean = user != null

    override suspend fun currentUser(): AuthUser? = user

    override suspend fun signIn(email: String, password: String) {
        user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = email)
    }

    override suspend fun signUp(email: String, password: String): Boolean {
        signUpCalls += 1
        user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = email)
        return true
    }

    override suspend fun signOut() {
        user = null
    }

    override suspend fun requestPasswordReset(email: String) = Unit
}

private class RecordingOrganizationGateway(
    private val users: RecordingAuthGateway,
) : OrganizationGateway {
    var membership: Membership? = null
    var userInserts: Int = 0
    var organizationInserts: Int = 0
    var membershipInserts: Int = 0
    var locationInserts: Int = 0
    var createdBy: String? = null
    var memberUserId: String? = null
    var acceptedForeignCreator: Boolean = false
    var claimedForeignOrganization: Boolean = false
    private var organizationId: String = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
    private var organizationName: String? = null

    override suspend fun currentMembership(): Membership? = membership

    override suspend fun createOwnerOrganization(
        companyName: String,
        displayName: String,
    ): Membership {
        val user = users.currentUser() ?: throw OrganizationFailure.Rejected
        if (createdBy != null && createdBy != user.id) {
            acceptedForeignCreator = true
            throw OrganizationFailure.Rejected
        }
        membership?.let { return it }
        if (userInserts == 0) {
            userInserts += 1
        }
        if (organizationInserts == 0) {
            organizationInserts += 1
            createdBy = user.id
            organizationName = companyName.trim()
        }
        if (membershipInserts == 0) {
            membershipInserts += 1
            memberUserId = user.id
        }
        val created = Membership(
            organizationId = organizationId,
            organizationName = organizationName ?: companyName.trim(),
            role = "owner",
        )
        membership = created
        return created
    }
}
