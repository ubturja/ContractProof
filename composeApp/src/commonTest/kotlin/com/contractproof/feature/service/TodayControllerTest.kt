package com.contractproof.feature.service

import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class TodayControllerTest {
    @Test
    fun refreshMapsAssignedJobs() = runBlocking {
        val organizations = CleanerOrganizationGateway(userId = "cleaner-1")
        val jobs = MemoryServiceJobRepository()
        val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val job = sampleJob(id = "job-1", locationName = "Lobby")
        jobs.seed(today, "cleaner-1", listOf(job))
        val controller = TodayController(organizations, jobs)
        controller.refresh()
        assertEquals(1, controller.state.value.jobs.size)
        assertEquals("Lobby", controller.state.value.jobs.single().locationName)
        assertEquals(50, controller.state.value.jobs.single().coveragePercent)
        assertEquals("Northside", controller.state.value.organizationName)
    }

    @Test
    fun emptyJobsShowsNoBanner() = runBlocking {
        val organizations = CleanerOrganizationGateway(userId = "cleaner-1")
        val jobs = MemoryServiceJobRepository()
        val controller = TodayController(organizations, jobs)
        controller.refresh()
        assertTrue(controller.state.value.jobs.isEmpty())
        assertEquals(null, controller.state.value.banner)
    }

    @Test
    fun networkFailureWithCachedJobsKeepsBannerOffline() = runBlocking {
        val organizations = CleanerOrganizationGateway(userId = "cleaner-1")
        val jobs = MemoryServiceJobRepository(failWith = ServiceJobFailure.Network)
        val controller = TodayController(organizations, jobs)
        controller.seedForPreview(listOf(sampleJob(id = "job-1", locationName = "Lobby")))
        controller.refresh()
        assertEquals(1, controller.state.value.jobs.size)
        assertEquals("You are offline. Showing the last loaded jobs.", controller.state.value.banner)
    }

    @Test
    fun networkFailureWithoutCacheShowsConnectionMessage() = runBlocking {
        val organizations = CleanerOrganizationGateway(userId = "cleaner-1")
        val jobs = MemoryServiceJobRepository(failWith = ServiceJobFailure.Network)
        val controller = TodayController(organizations, jobs)
        controller.refresh()
        assertTrue(controller.state.value.jobs.isEmpty())
        assertEquals("You need a connection to load today's jobs.", controller.state.value.banner)
    }

    @Test
    fun missingMembershipShowsUnavailable() = runBlocking {
        val organizations = object : OrganizationGateway {
            override suspend fun currentMembership(): Membership? = null
            override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
                throw OrganizationFailure.Rejected
            }
        }
        val jobs = MemoryServiceJobRepository()
        val controller = TodayController(organizations, jobs)
        controller.refresh()
        assertTrue(controller.state.value.jobs.isEmpty())
        assertEquals("Today's jobs are not available.", controller.state.value.banner)
    }

    private fun sampleJob(id: String, locationName: String): ServiceJob {
        return ServiceJob(
            id = id,
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", locationName, "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T14:00:00Z",
            scheduledEnd = "2026-10-01T15:00:00Z",
            assignee = ServiceJobAssignee("cleaner-1", "Cleaner"),
            status = ServiceJobRules.Scheduled,
            requirements = emptyList(),
            evidenceStatus = ServiceJobEvidenceStatus(
                mandatoryTotal = 2,
                mandatorySatisfied = 1,
                coveragePercent = 50,
                state = ServiceJobEvidenceState.Partial,
            ),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = "schedule-1",
        )
    }
}

private class CleanerOrganizationGateway(
    private val userId: String,
    private val role: String = "cleaner",
) : OrganizationGateway {
    override suspend fun currentMembership(): Membership? {
        return Membership(
            organizationId = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0",
            organizationName = "Northside",
            role = role,
            userId = userId,
        )
    }

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private class MemoryServiceJobRepository(
    private val failWith: ServiceJobFailure? = null,
) : ServiceJobRepository {
    private val byDay = mutableMapOf<Pair<LocalDate, String>, List<ServiceJob>>()

    fun seed(date: LocalDate, assigneeUserId: String, jobs: List<ServiceJob>) {
        byDay[date to assigneeUserId] = jobs
    }

    override suspend fun generateForApprovedVersion(contractId: String, contractVersionId: String, horizonDays: Int) = Unit

    override suspend fun get(jobId: String): ServiceJob? = null

    override suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob> {
        failWith?.let { throw it }
        return byDay[serviceDate to assigneeUserId] ?: emptyList()
    }

    override suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob> = emptyList()

    override suspend fun listForContract(contractId: String, fromDate: LocalDate, limit: Int): List<ServiceJob> =
        emptyList()

    override suspend fun start(jobId: String, startedAt: String): ServiceJob {
        throw ServiceJobFailure.Rejected
    }

    override suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob {
        throw ServiceJobFailure.Rejected
    }

    override suspend fun markIncomplete(jobId: String): ServiceJob {
        throw ServiceJobFailure.Rejected
    }

    override suspend fun markDisputed(jobId: String): ServiceJob {
        throw ServiceJobFailure.Rejected
    }
}
