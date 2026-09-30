package com.contractproof.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlin.coroutines.cancellation.CancellationException

class SupabaseAuthGateway(
    private val client: SupabaseClient,
) : AuthGateway {
    override suspend fun hasStoredSession(): Boolean {
        return currentUser() != null
    }

    override suspend fun currentUser(): AuthUser? {
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: return null
        val email = user.email?.takeIf { it.isNotEmpty() } ?: return null
        return AuthUser(id = user.id, email = email)
    }

    override suspend fun signIn(email: String, password: String) {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapAuthFailure(error, signIn = true)
        }
    }

    override suspend fun signUp(email: String, password: String): Boolean {
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapAuthFailure(error, signIn = false)
        }
        return currentUser() != null
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    override suspend fun requestPasswordReset(email: String) {
        try {
            client.auth.resetPasswordForEmail(email)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            throw mapAuthFailure(error, signIn = false)
        }
    }
}

private fun mapAuthFailure(error: Throwable, signIn: Boolean): AuthFailure {
    if (error is AuthFailure) {
        return error
    }
    if (error.isOfflineFailure()) {
        return AuthFailure.Network
    }
    val detail = error.describeChain().lowercase()
    if (
        detail.contains("user_already_exists") ||
        detail.contains("email_exists") ||
        detail.contains("already registered") ||
        detail.contains("already been registered") ||
        detail.contains("already in use")
    ) {
        return AuthFailure.EmailInUse
    }
    return if (signIn) AuthFailure.InvalidCredentials else AuthFailure.Rejected
}
