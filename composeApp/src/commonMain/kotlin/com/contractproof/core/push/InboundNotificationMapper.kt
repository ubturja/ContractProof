package com.contractproof.core.push

object InboundNotificationMapper {
    fun fromDataPayload(data: Map<String, String>): InboundNotification? {
        val type = data["type"]?.trim()?.lowercase() ?: return null
        return when (type) {
            InboundNotification.AssignedService.TYPE -> {
                val jobId = data["job_id"]?.trim().orEmpty()
                if (jobId.isEmpty()) null else InboundNotification.AssignedService(jobId)
            }
            InboundNotification.MissingEvidence.TYPE -> {
                val jobId = data["job_id"]?.trim().orEmpty()
                if (jobId.isEmpty()) null else InboundNotification.MissingEvidence(jobId)
            }
            InboundNotification.DisputeReceived.TYPE -> {
                val disputeId = data["dispute_id"]?.trim().orEmpty()
                if (disputeId.isEmpty()) null else InboundNotification.DisputeReceived(disputeId)
            }
            InboundNotification.ReportGenerated.TYPE -> {
                val disputeId = data["dispute_id"]?.trim().orEmpty()
                if (disputeId.isEmpty()) null else InboundNotification.ReportGenerated(disputeId)
            }
            else -> null
        }
    }
}
