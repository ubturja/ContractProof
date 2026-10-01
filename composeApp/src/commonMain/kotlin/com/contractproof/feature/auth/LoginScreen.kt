package com.contractproof.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpButtonStyle
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar

@Composable
fun LoginScreen(
    state: AuthUiState,
    showDemoCredentialsHint: Boolean = false,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    AuthForm(
        title = "Sign in",
        state = state,
        showDemoCredentialsHint = showDemoCredentialsHint,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        submitLabel = if (state.phase == AuthPhase.SigningIn) "Signing in" else "Sign in",
        submitEnabled = state.canSubmitCredentials,
        onSubmit = onSubmit,
        secondary = {
            CpButton(
                label = "Create account",
                onClick = onCreateAccount,
                style = CpButtonStyle.Secondary,
                enabled = !state.isBusy,
            )
            CpButton(
                label = "Forgot password",
                onClick = onForgotPassword,
                style = CpButtonStyle.Secondary,
                enabled = !state.isBusy,
            )
        },
    )
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    AuthForm(
        title = "Create account",
        state = state,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        submitLabel = if (state.phase == AuthPhase.CreatingAccount) "Creating account" else "Create account",
        submitEnabled = state.canSubmitCredentials,
        onSubmit = onSubmit,
        onBack = onBack,
        secondary = {
            if (state.suggestSignIn) {
                CpButton(
                    label = "Sign in",
                    onClick = onSignIn,
                    style = CpButtonStyle.Secondary,
                    enabled = !state.isBusy,
                )
            }
        },
    )
}

@Composable
fun ResetPasswordScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Reset password", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                CpTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Email",
                )
                CpButton(
                    label = if (state.phase == AuthPhase.SendingReset) "Sending reset" else "Send reset",
                    onClick = onSubmit,
                    enabled = state.canRequestReset && state.phase != AuthPhase.ResetSent,
                )
                CpButton(
                    label = "Sign in",
                    onClick = onSignIn,
                    style = CpButtonStyle.Secondary,
                    enabled = !state.isBusy,
                )
            }
        }
    }
}

@Composable
private fun AuthForm(
    title: String,
    state: AuthUiState,
    showDemoCredentialsHint: Boolean = false,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    submitLabel: String,
    submitEnabled: Boolean,
    onSubmit: () -> Unit,
    onBack: (() -> Unit)? = null,
    secondary: @Composable () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = title, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                if (showDemoCredentialsHint) {
                    DemoCredentialsHint()
                }
                CpTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                CpTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    concealed = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )
                CpButton(
                    label = submitLabel,
                    onClick = onSubmit,
                    enabled = submitEnabled,
                )
                secondary()
            }
        }
    }
}
