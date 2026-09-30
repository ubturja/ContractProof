package com.contractproof.feature.service

import androidx.compose.runtime.Composable
import com.contractproof.app.PlaceholderPage

@Composable
fun ServiceScreen(onBack: () -> Unit) {
    PlaceholderPage(
        title = "Service",
        message = "This screen is not wired to a service job.",
        onBack = onBack,
    )
}
