package com.contractproof.core.push

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InboundNotificationMapperTest {
    @Test
    fun mapsAssignedService() {
        val notification = InboundNotificationMapper.fromDataPayload(
            mapOf("type" to "assigned_service", "job_id" to "job-1"),
        )
        assertIs<InboundNotification.AssignedService>(notification)
        assertEquals("job-1", notification.jobId)
    }

    @Test
    fun mapsDisputeReceived() {
        val notification = InboundNotificationMapper.fromDataPayload(
            mapOf("type" to "dispute_received", "dispute_id" to "d-1"),
        )
        assertIs<InboundNotification.DisputeReceived>(notification)
    }

    @Test
    fun rejectsUnknownType() {
        assertNull(InboundNotificationMapper.fromDataPayload(mapOf("type" to "unknown")))
    }
}
