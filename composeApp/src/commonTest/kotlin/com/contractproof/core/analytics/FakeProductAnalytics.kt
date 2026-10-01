package com.contractproof.core.analytics

class FakeProductAnalytics : ProductAnalytics {
    val events = mutableListOf<ProductEvent>()
    var organizationId: String? = null
    var role: String? = null

    override fun setUserContext(organizationId: String?, role: String?) {
        this.organizationId = organizationId
        this.role = role
    }

    override fun track(event: ProductEvent) {
        events.add(event)
    }
}
