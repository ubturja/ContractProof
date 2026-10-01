package com.contractproof.subscription

import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionSource
import com.contractproof.domain.SubscriptionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking

class DisabledSubscriptionServiceTest {
    @Test
    fun defaultsToFreeDisabled() {
        val service = DisabledSubscriptionService()
        assertFalse(service.isConfigured())
        assertEquals(SubscriptionPlan.Free, service.state.value.plan)
        assertEquals(SubscriptionSource.Disabled, service.state.value.source)
        assertEquals(SubscriptionStatus.Active, service.state.value.status)
    }

    @Test
    fun refreshIsNoOp() = runBlocking {
        val service = DisabledSubscriptionService()
        service.refresh()
        assertEquals(SubscriptionPlan.Free, service.state.value.plan)
    }
}
