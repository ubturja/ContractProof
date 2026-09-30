package com.contractproof.feature.contract

import androidx.compose.runtime.Composable
import com.contractproof.app.PlaceholderPage

@Composable
fun ContractsScreen(onBack: () -> Unit) {
    PlaceholderPage(
        title = "Contracts",
        message = "This screen is not wired to a contract.",
        onBack = onBack,
    )
}
