package com.contractproof.app

import android.util.Log
import com.contractproof.core.push.InboundNotificationMapper
import com.contractproof.core.push.PushTokenBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ContractProofFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = InboundNotificationMapper.fromDataPayload(message.data)
        if (notification == null) {
            Log.i(TAG, "Ignored FCM message without supported type")
            return
        }
        val title = message.data["title"] ?: defaultTitle(notification)
        val body = message.data["body"] ?: defaultBody(notification)
        ContractProofNotificationDisplay.show(
            context = this,
            notification = notification,
            title = title,
            body = body,
        )
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")
        CoroutineScope(Dispatchers.IO).launch {
            PushTokenBridge.onNewToken(token)
        }
    }

    private fun defaultTitle(notification: com.contractproof.core.push.InboundNotification): String {
        return when (notification) {
            is com.contractproof.core.push.InboundNotification.AssignedService -> "Service assigned"
            is com.contractproof.core.push.InboundNotification.MissingEvidence -> "Evidence missing"
            is com.contractproof.core.push.InboundNotification.DisputeReceived -> "Dispute received"
            is com.contractproof.core.push.InboundNotification.ReportGenerated -> "Report ready"
        }
    }

    private fun defaultBody(notification: com.contractproof.core.push.InboundNotification): String {
        return when (notification) {
            is com.contractproof.core.push.InboundNotification.AssignedService ->
                "Open the assigned service job."
            is com.contractproof.core.push.InboundNotification.MissingEvidence ->
                "A required task still needs evidence."
            is com.contractproof.core.push.InboundNotification.DisputeReceived ->
                "A new dispute needs your attention."
            is com.contractproof.core.push.InboundNotification.ReportGenerated ->
                "Your evidence report is ready to review."
        }
    }

    companion object {
        private const val TAG = "FCM"
    }
}
