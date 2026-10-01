package com.contractproof.integration

import com.contractproof.app.SessionController
import com.contractproof.app.SessionDestination
import com.contractproof.core.analytics.FakeProductAnalytics
import com.contractproof.data.AuthGateway
import com.contractproof.data.AuthUser
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ContractRuleViolation
import com.contractproof.domain.ContractRules
import com.contractproof.feature.auth.AuthController
import com.contractproof.subscription.FakeSubscriptionService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class OwnerOnboardingWorkflowTest {
    @Test
    fun registerCreateCompanyThenDraftContractFieldsValidate() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = sessionController(users, organizations)
        session.restore()
        session.auth.updateEmail("owner@example.com")
        session.auth.updatePassword("secret")
        session.signUp()
        assertEquals(SessionDestination.CompanySetup, session.destination.value)

        session.updateCompanyName("ClearLine Demo Co")
        session.updateDisplayName("Owner")
        session.createCompany()

        val orgId = session.membership.value!!.organizationId
        assertEquals(SessionDestination.Dashboard, session.destination.value)

        val title = ContractRules.requireTitle("Meridian nightly clean")
        val start = ContractRules.requireIsoDate("2026-10-01", "Start date")
        ContractRules.requireOrganization(orgId, orgId)
        assertEquals("Meridian nightly clean", title)
        assertEquals("2026-10-01", start)
    }

    @Test
    fun tenantIsolationRejectsForeignOrganizationOnContractWrite() {
        val orgA = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"
        val orgB = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0"
        assertFailsWith<ContractRuleViolation> {
            ContractRules.requireOrganization(orgB, orgA)
        }
    }

    @Test
    fun signedOutSessionHasNoWrites() = runBlocking {
        val users = RecordingAuthGateway()
        val organizations = RecordingOrganizationGateway(users)
        val session = sessionController(users, organizations)
        session.restore()
        assertEquals(SessionDestination.SignedOut, session.destination.value)
        assertNull(session.membership.value)
        val access = Access.unsigned
        assertFalse(access.canWriteContracts)
        assertFalse(access.canAddLocation)
    }

    @Test
    fun twoOrgMembershipsInGatewayStayScoped() = runBlocking {
        val users = RecordingAuthGateway()
        val orgA = RecordingOrganizationGateway(users, "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0")
        val orgB = RecordingOrganizationGateway(users, "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0")
        users.user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = "owner@example.com")
        orgA.membership = Membership(
            organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
            organizationName = "A",
            role = "owner",
        )
        orgB.membership = Membership(
            organizationId = "b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0",
            organizationName = "B",
            role = "owner",
        )
        assertEquals("a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0", orgA.currentMembership()?.organizationId)
        assertEquals("b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0", orgB.currentMembership()?.organizationId)
    }
}

private fun sessionController(
    users: RecordingAuthGateway,
    organizations: OrganizationGateway,
): SessionController {
    val analytics = FakeProductAnalytics()
    return SessionController(
        AuthController(users, analytics),
        organizations,
        users,
        FakeSubscriptionService(),
        analytics,
    )
}

private class RecordingAuthGateway : AuthGateway {
    var user: AuthUser? = null

    override suspend fun hasStoredSession(): Boolean = user != null

    override suspend fun currentUser(): AuthUser? = user

    override suspend fun signIn(email: String, password: String) {
        user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = email)
    }

    override suspend fun signUp(email: String, password: String): Boolean {
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
    private val organizationId: String = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
) : OrganizationGateway {
    var membership: Membership? = null

    override suspend fun currentMembership(): Membership? = membership

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        val user = users.currentUser() ?: throw OrganizationFailure.Rejected
        val created = Membership(
            organizationId = organizationId,
            organizationName = companyName.trim(),
            role = "owner",
            userId = user.id,
        )
        membership = created
        return created
    }
}
