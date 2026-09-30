package com.contractproof.data

data class AuthUser(
    val id: String,
    val email: String,
)

interface AuthGateway {
    suspend fun hasStoredSession(): Boolean

    suspend fun currentUser(): AuthUser?

    suspend fun signIn(email: String, password: String)

    suspend fun signUp(email: String, password: String): Boolean

    suspend fun signOut()

    suspend fun requestPasswordReset(email: String)
}

sealed class AuthFailure : Exception() {
    data object Network : AuthFailure()

    data object InvalidCredentials : AuthFailure()

    data object EmailInUse : AuthFailure()

    data object Rejected : AuthFailure()
}
