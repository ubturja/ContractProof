package com.contractproof.feature.subscription

import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionSnapshot
import com.contractproof.domain.SubscriptionStatus
import com.contractproof.core.analytics.FakeProductAnalytics
import com.contractproof.subscription.FakeSubscriptionService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PaywallControllerTest {
    @Test
    fun restoreSuccessUpdatesState() = runBlocking {
        val service = FakeSubscriptionService(
            initial = SubscriptionSnapshot(
                plan = SubscriptionPlan.Pro,
                status = SubscriptionStatus.Active,
            ),
        )
        val controller = PaywallController(service, FakeProductAnalytics())
        controller.restore()
        assertEquals(1, service.restoreCalls)
        assertEquals(SubscriptionPlan.Pro, controller.state.value.snapshot?.effectivePlan())
        assertTrue(controller.state.value.banner?.contains("restored") == true)
    }
}
