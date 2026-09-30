package com.contractproof.feature.auth

import androidx.compose.runtime.Composable
import com.contractproof.app.PlaceholderPage

@Composable
fun ClientHoldScreen(onSignOut: () -> Unit) {
    PlaceholderPage(
        title = "Client",
        message = "Service review and disputes are not in the field app yet.",
        actions = listOf("Sign out" to onSignOut),
    )
}
