package com.contractproof.feature.dispute

import androidx.compose.runtime.Composable
import com.contractproof.app.PlaceholderPage

@Composable
fun DisputesScreen(onBack: () -> Unit) {
    PlaceholderPage(
        title = "Disputes",
        message = "This screen is not wired to a dispute.",
        onBack = onBack,
    )
}
