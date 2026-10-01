package com.contractproof.data.local

import com.contractproof.domain.JobRequirementStatus
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRequirement
import com.contractproof.domain.ServiceJobRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class SqlDelightServiceJobStoreTest {
    @Test
    fun roundTripsAssignedJob() = runBlocking {
        val store = SqlDelightServiceJobStore(ContractProofTestDatabase.create())
        val job = sampleJob()
        store.upsert(job)
        val loaded = store.listAssignedOn("org-1", LocalDate(2026, 10, 1), "user-1")
        assertEquals(1, loaded.size)
        assertEquals("Lobby", loaded.first().location.name)
        assertEquals("Vacuum", loaded.first().requirements.first().requirementText)
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("user-1", "Cleaner"),
            status = ServiceJobRules.Scheduled,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "req-1",
                    contractRequirementId = "cr-1",
                    requirementText = "Vacuum",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.NotStarted),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = "schedule-1",
        )
    }
}
