package com.contractproof.feature.service

import androidx.compose.runtime.Composable
import com.contractproof.app.PlaceholderPage

@Composable
fun TodayScreen(
    organizationName: String,
    onOpenSettings: () -> Unit,
) {
    PlaceholderPage(
        title = "Today",
        message = if (organizationName.isEmpty()) {
            "Assigned jobs are not wired yet."
        } else {
            "$organizationName. Assigned jobs are not wired yet."
        },
        actions = listOf("Open settings" to onOpenSettings),
    )
}
