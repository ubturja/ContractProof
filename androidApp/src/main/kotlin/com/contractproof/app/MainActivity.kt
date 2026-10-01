package com.contractproof.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.contractproof.core.initPlatformContext
import com.contractproof.core.push.InboundNotification
import com.contractproof.core.push.PendingNotificationTap
import com.contractproof.subscription.SubscriptionPurchaseActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlatformContext(this)
        SubscriptionPurchaseActivity.bind(this)
        storeNotificationTap(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        storeNotificationTap(intent)
    }

    private fun storeNotificationTap(intent: Intent?) {
        val notification = parseNotification(intent) ?: return
        PendingNotificationTap.pending = notification
    }

    companion object {
        private const val EXTRA_TYPE = "notification_type"
        private const val EXTRA_JOB_ID = "job_id"
        private const val EXTRA_DISPUTE_ID = "dispute_id"

        fun notificationIntent(context: Context, notification: InboundNotification): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TYPE, notification.type)
                when (notification) {
                    is InboundNotification.AssignedService ->
                        putExtra(EXTRA_JOB_ID, notification.jobId)
                    is InboundNotification.MissingEvidence ->
                        putExtra(EXTRA_JOB_ID, notification.jobId)
                    is InboundNotification.DisputeReceived ->
                        putExtra(EXTRA_DISPUTE_ID, notification.disputeId)
                    is InboundNotification.ReportGenerated ->
                        putExtra(EXTRA_DISPUTE_ID, notification.disputeId)
                }
            }
        }

        private fun parseNotification(intent: Intent?): InboundNotification? {
            val type = intent?.getStringExtra(EXTRA_TYPE)?.trim()?.lowercase() ?: return null
            return when (type) {
                InboundNotification.AssignedService.TYPE -> {
                    val jobId = intent.getStringExtra(EXTRA_JOB_ID)?.trim().orEmpty()
                    if (jobId.isEmpty()) null else InboundNotification.AssignedService(jobId)
                }
                InboundNotification.MissingEvidence.TYPE -> {
                    val jobId = intent.getStringExtra(EXTRA_JOB_ID)?.trim().orEmpty()
                    if (jobId.isEmpty()) null else InboundNotification.MissingEvidence(jobId)
                }
                InboundNotification.DisputeReceived.TYPE -> {
                    val disputeId = intent.getStringExtra(EXTRA_DISPUTE_ID)?.trim().orEmpty()
                    if (disputeId.isEmpty()) null else InboundNotification.DisputeReceived(disputeId)
                }
                InboundNotification.ReportGenerated.TYPE -> {
                    val disputeId = intent.getStringExtra(EXTRA_DISPUTE_ID)?.trim().orEmpty()
                    if (disputeId.isEmpty()) null else InboundNotification.ReportGenerated(disputeId)
                }
                else -> null
            }
        }
    }
}
