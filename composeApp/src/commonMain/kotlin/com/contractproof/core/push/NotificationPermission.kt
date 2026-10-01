package com.contractproof.core.push

import androidx.compose.runtime.Composable

data class NotificationPermissionState(
    val isGranted: Boolean = false,
    val canRequest: Boolean = false,
)

expect class NotificationPermissionController {
    fun currentState(): NotificationPermissionState

    suspend fun requestPermission(): NotificationPermissionState
}

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
