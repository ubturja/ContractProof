package com.contractproof.core.analytics

interface ProductAnalytics {
    fun setUserContext(organizationId: String?, role: String?)

    fun track(event: ProductEvent)
}

sealed class ProductEvent {
    abstract val name: String

    abstract fun properties(): Map<String, String>

    data object AccountCreated : ProductEvent() {
        override val name: String = "account_created"
        override fun properties(): Map<String, String> = emptyMap()
    }

    data class OrganizationCreated(val organizationId: String) : ProductEvent() {
        override val name: String = "organization_created"
        override fun properties(): Map<String, String> = mapOf("organization_id" to organizationId)
    }

    data class ClientCreated(val clientId: String) : ProductEvent() {
        override val name: String = "client_created"
        override fun properties(): Map<String, String> = mapOf("client_id" to clientId)
    }

    data class LocationCreated(val locationId: String, val clientId: String) : ProductEvent() {
        override val name: String = "location_created"
        override fun properties(): Map<String, String> = mapOf(
            "location_id" to locationId,
            "client_id" to clientId,
        )
    }

    data class ContractUploaded(val contractId: String) : ProductEvent() {
        override val name: String = "contract_uploaded"
        override fun properties(): Map<String, String> = mapOf("contract_id" to contractId)
    }

    data class ContractExtractionCompleted(val contractId: String, val versionId: String) : ProductEvent() {
        override val name: String = "contract_extraction_completed"
        override fun properties(): Map<String, String> = mapOf(
            "contract_id" to contractId,
            "version_id" to versionId,
        )
    }

    data class ServiceStarted(val jobId: String) : ProductEvent() {
        override val name: String = "service_started"
        override fun properties(): Map<String, String> = mapOf("job_id" to jobId)
    }

    data class EvidenceAdded(val jobId: String, val requirementId: String) : ProductEvent() {
        override val name: String = "evidence_added"
        override fun properties(): Map<String, String> = mapOf(
            "job_id" to jobId,
            "requirement_id" to requirementId,
        )
    }

    data class ExceptionCreated(val jobId: String, val requirementId: String) : ProductEvent() {
        override val name: String = "exception_created"
        override fun properties(): Map<String, String> = mapOf(
            "job_id" to jobId,
            "requirement_id" to requirementId,
        )
    }

    data class ServiceCompleted(val jobId: String) : ProductEvent() {
        override val name: String = "service_completed"
        override fun properties(): Map<String, String> = mapOf("job_id" to jobId)
    }

    data class DisputeCreated(val disputeId: String) : ProductEvent() {
        override val name: String = "dispute_created"
        override fun properties(): Map<String, String> = mapOf("dispute_id" to disputeId)
    }

    data class ReportGenerated(val disputeId: String) : ProductEvent() {
        override val name: String = "report_generated"
        override fun properties(): Map<String, String> = mapOf("dispute_id" to disputeId)
    }

    data object PaywallViewed : ProductEvent() {
        override val name: String = "paywall_viewed"
        override fun properties(): Map<String, String> = emptyMap()
    }

    data class SubscriptionStarted(val plan: String) : ProductEvent() {
        override val name: String = "subscription_started"
        override fun properties(): Map<String, String> = mapOf("plan" to plan)
    }
}
