package com.contractproof.core.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class NotificationPermissionController {
    actual fun currentState(): NotificationPermissionState {
        return NotificationPermissionState(isGranted = false, canRequest = false)
    }

    actual suspend fun requestPermission(): NotificationPermissionState {
        return currentState()
    }
}

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    return remember { NotificationPermissionController() }
}
