package com.contractproof.core.analytics

import com.posthog.PostHog

class PostHogProductAnalytics : ProductAnalytics {
    override fun setUserContext(organizationId: String?, role: String?) {
        if (organizationId.isNullOrBlank()) {
            PostHog.reset()
            return
        }
        val properties = buildMap<String, Any> {
            put("organization_id", organizationId)
            if (!role.isNullOrBlank()) {
                put("role", role)
            }
        }
        PostHog.identify(
            distinctId = organizationId,
            userProperties = AnalyticsSanitizer.sanitizeProperties(properties.mapValues { it.value.toString() }),
        )
    }

    override fun track(event: ProductEvent) {
        val properties = AnalyticsSanitizer.sanitizeProperties(event.properties())
            .mapValues { it.value as Any }
        PostHog.capture(event = event.name, properties = properties)
    }
}
