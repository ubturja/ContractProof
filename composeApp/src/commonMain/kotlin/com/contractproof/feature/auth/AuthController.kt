package com.contractproof.feature.auth

import com.contractproof.data.AuthFailure
import com.contractproof.data.AuthGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AuthPhase {
    Restoring,
    SignedOut,
    SigningIn,
    CreatingAccount,
    SendingReset,
    SignedIn,
    ResetSent,
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val phase: AuthPhase = AuthPhase.Restoring,
    val banner: String? = null,
    val suggestSignIn: Boolean = false,
) {
    val isBusy: Boolean
        get() = phase == AuthPhase.Restoring ||
            phase == AuthPhase.SigningIn ||
            phase == AuthPhase.CreatingAccount ||
            phase == AuthPhase.SendingReset

    val canSubmitCredentials: Boolean
        get() = email.trim().isNotEmpty() && password.isNotEmpty() && !isBusy

    val canRequestReset: Boolean
        get() = email.trim().isNotEmpty() && !isBusy
}

class AuthController(
    private val gateway: AuthGateway,
) {
    private val signedIn = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = signedIn.asStateFlow()

    fun updateEmail(value: String) {
        signedIn.update { it.copy(email = value) }
    }

    fun updatePassword(value: String) {
        signedIn.update { it.copy(password = value) }
    }

    fun clearNotice() {
        signedIn.update { current ->
            current.copy(
                banner = null,
                suggestSignIn = false,
                phase = if (current.phase == AuthPhase.ResetSent) AuthPhase.SignedOut else current.phase,
            )
        }
    }

    suspend fun restore() {
        if (signedIn.value.phase != AuthPhase.Restoring && signedIn.value.phase != AuthPhase.SignedOut) {
            return
        }
        signedIn.update { it.copy(phase = AuthPhase.Restoring, banner = null, suggestSignIn = false) }
        val signedInNow = try {
            gateway.hasStoredSession()
        } catch (_: Throwable) {
            false
        }
        signedIn.update { current ->
            current.copy(phase = if (signedInNow) AuthPhase.SignedIn else AuthPhase.SignedOut)
        }
    }

    suspend fun signIn() {
        val current = signedIn.value
        if (!current.canSubmitCredentials) {
            return
        }
        val email = current.email.trim()
        val password = current.password
        signedIn.update {
            it.copy(email = email, phase = AuthPhase.SigningIn, banner = null, suggestSignIn = false)
        }
        try {
            gateway.signIn(email, password)
            signedIn.update { it.copy(phase = AuthPhase.SignedIn, password = "", banner = null) }
        } catch (failure: AuthFailure) {
            signedIn.update { latest ->
                when (failure) {
                    AuthFailure.Network -> latest.copy(
                        phase = AuthPhase.SignedOut,
                        banner = "You need a connection to sign in.",
                    )
                    else -> latest.copy(
                        phase = AuthPhase.SignedOut,
                        password = "",
                        banner = "Sign-in failed.",
                    )
                }
            }
        }
    }

    suspend fun signUp() {
        val current = signedIn.value
        if (!current.canSubmitCredentials) {
            return
        }
        val email = current.email.trim()
        val password = current.password
        signedIn.update {
            it.copy(email = email, phase = AuthPhase.CreatingAccount, banner = null, suggestSignIn = false)
        }
        try {
            val hasSession = gateway.signUp(email, password)
            signedIn.update { latest ->
                if (hasSession) {
                    latest.copy(phase = AuthPhase.SignedIn, password = "", banner = null)
                } else {
                    latest.copy(
                        phase = AuthPhase.SignedOut,
                        password = "",
                        banner = "Confirm your email, then sign in.",
                        suggestSignIn = true,
                    )
                }
            }
        } catch (failure: AuthFailure) {
            signedIn.update { latest ->
                when (failure) {
                    AuthFailure.Network -> latest.copy(
                        phase = AuthPhase.SignedOut,
                        banner = "You need a connection to create an account.",
                    )
                    AuthFailure.EmailInUse -> latest.copy(
                        phase = AuthPhase.SignedOut,
                        banner = "That email is already in use.",
                        suggestSignIn = true,
                    )
                    else -> latest.copy(
                        phase = AuthPhase.SignedOut,
                        banner = "The account was not created.",
                    )
                }
            }
        }
    }

    suspend fun requestPasswordReset() {
        val current = signedIn.value
        if (!current.canRequestReset) {
            return
        }
        val email = current.email.trim()
        signedIn.update {
            it.copy(email = email, phase = AuthPhase.SendingReset, banner = null, suggestSignIn = false)
        }
        try {
            gateway.requestPasswordReset(email)
            signedIn.update {
                it.copy(
                    phase = AuthPhase.ResetSent,
                    password = "",
                    banner = "Check your email for a reset link.",
                )
            }
        } catch (failure: AuthFailure) {
            signedIn.update { latest ->
                latest.copy(
                    phase = AuthPhase.SignedOut,
                    banner = if (failure is AuthFailure.Network) {
                        "You need a connection to reset your password."
                    } else {
                        "The reset email was not sent."
                    },
                )
            }
        }
    }

    suspend fun signOut() {
        gateway.signOut()
        signedIn.update {
            AuthUiState(phase = AuthPhase.SignedOut)
        }
    }
}
