package com.contractproof.core.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProductEventTest {
    @Test
    fun eventNamesMatchProductSpec() {
        assertEquals("account_created", ProductEvent.AccountCreated.name)
        assertEquals("organization_created", ProductEvent.OrganizationCreated("o").name)
        assertEquals("paywall_viewed", ProductEvent.PaywallViewed.name)
        assertEquals("subscription_started", ProductEvent.SubscriptionStarted("pro").name)
    }

    @Test
    fun propertiesAvoidBlockedKeys() {
        val properties = ProductEvent.LocationCreated("loc", "client").properties()
        properties.keys.forEach { key ->
            val normalized = key.lowercase()
            assertFalse(normalized.contains("email"))
            assertFalse(normalized.contains("password"))
            assertFalse(normalized.contains("document"))
        }
        val sanitized = AnalyticsSanitizer.sanitizeProperties(
            mapOf("job_id" to "j1", "email" to "hidden@example.com"),
        )
        assertEquals(mapOf("job_id" to "j1"), sanitized)
    }
}
