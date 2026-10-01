package com.contractproof.core.push

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.contractproof.core.requireApplicationContext

actual class NotificationPermissionController internal constructor(
    private val activity: ComponentActivity?,
    private val permissionGranted: () -> Boolean,
    private val launchRequest: () -> Unit,
) {
    actual fun currentState(): NotificationPermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationPermissionState(isGranted = true, canRequest = false)
        }
        val granted = permissionGranted()
        return NotificationPermissionState(isGranted = granted, canRequest = !granted)
    }

    actual suspend fun requestPermission(): NotificationPermissionState {
        val state = currentState()
        if (state.isGranted || !state.canRequest) {
            return state
        }
        launchRequest()
        return currentState()
    }
}

private fun isNotificationPermissionGranted(activity: ComponentActivity?): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    val context = activity ?: requireApplicationContext()
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    val activity = LocalContext.current as? ComponentActivity
    var granted by remember(activity) { mutableStateOf(isNotificationPermissionGranted(activity)) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
    }
    return remember(activity, granted, launcher) {
        NotificationPermissionController(
            activity = activity,
            permissionGranted = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    true
                } else {
                    granted || isNotificationPermissionGranted(activity)
                }
            },
            launchRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}
