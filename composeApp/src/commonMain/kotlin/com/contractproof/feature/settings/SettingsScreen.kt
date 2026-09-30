package com.contractproof.feature.settings

import androidx.compose.runtime.Composable
import com.contractproof.app.PlaceholderPage

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    PlaceholderPage(
        title = "Settings",
        message = "You are signed in. Signing out ends this session on this device.",
        onBack = onBack,
        actions = listOf("Sign out" to onSignOut),
    )
}
