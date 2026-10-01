package com.contractproof.core.analytics

class NoOpProductAnalytics : ProductAnalytics {
    override fun setUserContext(organizationId: String?, role: String?) {
        // Disabled analytics.
    }

    override fun track(event: ProductEvent) {
        // Disabled analytics.
    }
}
