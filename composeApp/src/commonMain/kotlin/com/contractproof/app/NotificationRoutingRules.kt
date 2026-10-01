package com.contractproof.app

import androidx.navigation3.runtime.NavKey
import com.contractproof.core.push.InboundNotification
import com.contractproof.domain.Access

object NotificationRoutingRules {
    fun navTarget(notification: InboundNotification, access: Access): NavKey? {
        return when (notification) {
            is InboundNotification.AssignedService,
            is InboundNotification.MissingEvidence,
            -> {
                if (!access.canOpenAssignedJobs) {
                    return null
                }
                val jobId = when (notification) {
                    is InboundNotification.AssignedService -> notification.jobId
                    is InboundNotification.MissingEvidence -> notification.jobId
                    else -> return null
                }
                JobRoute(jobId)
            }
            is InboundNotification.DisputeReceived -> {
                if (!access.canOpenDisputes) {
                    return null
                }
                DisputeDetailRoute(notification.disputeId)
            }
            is InboundNotification.ReportGenerated -> {
                if (!access.canOpenDisputes) {
                    return null
                }
                ReportPreviewRoute(notification.disputeId)
            }
        }
    }
}
