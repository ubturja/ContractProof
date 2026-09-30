package com.contractproof.feature.auth

import com.contractproof.data.AuthFailure
import com.contractproof.data.AuthGateway
import com.contractproof.data.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class AuthControllerTest {
    @Test
    fun restoreWithEmptyStoreSignsOut() = runBlocking {
        val gateway = FakeAuthGateway()
        val controller = AuthController(gateway)

        controller.restore()

        assertEquals(AuthPhase.SignedOut, controller.state.value.phase)
        assertEquals(1, gateway.sessionReads)
    }

    @Test
    fun restoreWithStoredSessionSignsIn() = runBlocking {
        val gateway = FakeAuthGateway(storedSession = true)
        val controller = AuthController(gateway)

        controller.restore()

        assertEquals(AuthPhase.SignedIn, controller.state.value.phase)
    }

    @Test
    fun restoreWithUnreadableStoreSignsOutOnce() = runBlocking {
        val gateway = FakeAuthGateway(storedSession = null)
        val controller = AuthController(gateway)

        controller.restore()

        assertEquals(AuthPhase.SignedOut, controller.state.value.phase)
        assertEquals(1, gateway.sessionReads)
        assertFalse(controller.state.value.phase == AuthPhase.SignedIn)
    }

    @Test
    fun signInSuccessSignsIn() = runBlocking {
        val gateway = FakeAuthGateway()
        val controller = signedOut(gateway)
        controller.updateEmail("owner@example.com")
        controller.updatePassword("secret")

        controller.signIn()

        assertEquals(AuthPhase.SignedIn, controller.state.value.phase)
        assertEquals(1, gateway.signInCalls)
        assertEquals("", controller.state.value.password)
    }

    @Test
    fun signInFailureKeepsEmailAndClearsPassword() = runBlocking {
        val gateway = FakeAuthGateway(signInFailure = AuthFailure.InvalidCredentials)
        val controller = signedOut(gateway)
        controller.updateEmail("owner@example.com")
        controller.updatePassword("wrong")

        controller.signIn()

        assertEquals(AuthPhase.SignedOut, controller.state.value.phase)
        assertEquals("owner@example.com", controller.state.value.email)
        assertEquals("", controller.state.value.password)
        assertEquals("Sign-in failed.", controller.state.value.banner)
    }

    @Test
    fun signUpSuccessSignsIn() = runBlocking {
        val gateway = FakeAuthGateway(signUpSession = true)
        val controller = signedOut(gateway)
        controller.updateEmail("owner@example.com")
        controller.updatePassword("secret")

        controller.signUp()

        assertEquals(AuthPhase.SignedIn, controller.state.value.phase)
        assertEquals(1, gateway.signUpCalls)
    }

    @Test
    fun signUpConfirmationStaysSignedOut() = runBlocking {
        val gateway = FakeAuthGateway(signUpSession = false)
        val controller = signedOut(gateway)
        controller.updateEmail("owner@example.com")
        controller.updatePassword("secret")

        controller.signUp()

        assertEquals(AuthPhase.SignedOut, controller.state.value.phase)
        assertEquals("Confirm your email, then sign in.", controller.state.value.banner)
        assertTrue(controller.state.value.suggestSignIn)
    }

    @Test
    fun duplicateEmailPointsToSignIn() = runBlocking {
        val gateway = FakeAuthGateway(signUpFailure = AuthFailure.EmailInUse)
        val controller = signedOut(gateway)
        controller.updateEmail("owner@example.com")
        controller.updatePassword("secret")

        controller.signUp()

        assertEquals(AuthPhase.SignedOut, controller.state.value.phase)
        assertEquals("That email is already in use.", controller.state.value.banner)
        assertTrue(controller.state.value.suggestSignIn)
        assertEquals("owner@example.com", controller.state.value.email)
    }

    @Test
    fun signOutReturnsToSignedOut() = runBlocking {
        val gateway = FakeAuthGateway(storedSession = true)
        val controller = AuthController(gateway)
        controller.restore()

        controller.signOut()

        assertEquals(AuthPhase.SignedOut, controller.state.value.phase)
        assertEquals(1, gateway.signOutCalls)
        assertEquals("", controller.state.value.email)
    }

    @Test
    fun passwordResetRequestsEmail() = runBlocking {
        val gateway = FakeAuthGateway()
        val controller = signedOut(gateway)
        controller.updateEmail("owner@example.com")

        controller.requestPasswordReset()

        assertEquals(AuthPhase.ResetSent, controller.state.value.phase)
        assertEquals("Check your email for a reset link.", controller.state.value.banner)
        assertEquals(1, gateway.resetCalls)
        assertEquals("owner@example.com", gateway.resetEmail)
    }
}

private suspend fun signedOut(gateway: FakeAuthGateway): AuthController {
    val controller = AuthController(gateway)
    controller.restore()
    return controller
}

private class FakeAuthGateway(
    private val storedSession: Boolean? = false,
    private val signInFailure: AuthFailure? = null,
    private val signUpSession: Boolean = true,
    private val signUpFailure: AuthFailure? = null,
) : AuthGateway {
    var sessionReads: Int = 0
    var signInCalls: Int = 0
    var signUpCalls: Int = 0
    var signOutCalls: Int = 0
    var resetCalls: Int = 0
    var resetEmail: String? = null
    var user: AuthUser? = if (storedSession == true) {
        AuthUser(id = "11111111-1111-1111-1111-111111111111", email = "owner@example.com")
    } else {
        null
    }

    override suspend fun hasStoredSession(): Boolean {
        sessionReads += 1
        return storedSession ?: throw IllegalStateException("unreadable session")
    }

    override suspend fun currentUser(): AuthUser? = user

    override suspend fun signIn(email: String, password: String) {
        signInCalls += 1
        signInFailure?.let { throw it }
        user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = email)
    }

    override suspend fun signUp(email: String, password: String): Boolean {
        signUpCalls += 1
        signUpFailure?.let { throw it }
        if (signUpSession) {
            user = AuthUser(id = "11111111-1111-1111-1111-111111111111", email = email)
        }
        return signUpSession
    }

    override suspend fun signOut() {
        signOutCalls += 1
        user = null
    }

    override suspend fun requestPasswordReset(email: String) {
        resetCalls += 1
        resetEmail = email
    }
}
