package com.contractproof.core.push

sealed class InboundNotification {
    abstract val type: String

    data class AssignedService(val jobId: String) : InboundNotification() {
        override val type: String = TYPE
        companion object {
            const val TYPE: String = "assigned_service"
        }
    }

    data class MissingEvidence(val jobId: String) : InboundNotification() {
        override val type: String = TYPE
        companion object {
            const val TYPE: String = "missing_evidence"
        }
    }

    data class DisputeReceived(val disputeId: String) : InboundNotification() {
        override val type: String = TYPE
        companion object {
            const val TYPE: String = "dispute_received"
        }
    }

    data class ReportGenerated(val disputeId: String) : InboundNotification() {
        override val type: String = TYPE
        companion object {
            const val TYPE: String = "report_generated"
        }
    }
}
