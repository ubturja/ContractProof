package com.contractproof.app

import com.contractproof.core.push.InboundNotification
import com.contractproof.domain.Access
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationRoutingRulesTest {
    @Test
    fun cleanerReceivesJobNotification() {
        val access = Access.forMembership("cleaner")
        val route = NotificationRoutingRules.navTarget(
            InboundNotification.AssignedService("job-1"),
            access,
        )
        assertEquals(JobRoute("job-1"), route)
    }

    @Test
    fun ownerReceivesDisputeNotification() {
        val access = Access.forMembership("owner")
        val route = NotificationRoutingRules.navTarget(
            InboundNotification.DisputeReceived("dispute-1"),
            access,
        )
        assertEquals(DisputeDetailRoute("dispute-1"), route)
    }

    @Test
    fun clientIgnoresOwnerDisputeNotification() {
        val access = Access.forMembership("client")
        val route = NotificationRoutingRules.navTarget(
            InboundNotification.DisputeReceived("dispute-1"),
            access,
        )
        assertNull(route)
    }

    @Test
    fun managerIgnoresAssignedJobNotification() {
        val access = Access.forMembership("manager")
        val route = NotificationRoutingRules.navTarget(
            InboundNotification.MissingEvidence("job-1"),
            access,
        )
        assertNull(route)
    }
}
