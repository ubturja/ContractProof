package com.contractproof.feature.service

import com.contractproof.data.ExceptionGateway
import com.contractproof.data.Membership
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.data.TaskExceptionResult
import com.contractproof.domain.Evidence
import com.contractproof.domain.EvidenceLocation
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.EvidenceSubmitResult
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobAssignee
import com.contractproof.domain.ServiceJobClientRef
import com.contractproof.domain.ServiceJobEvidenceState
import com.contractproof.domain.ServiceJobEvidenceStatus
import com.contractproof.domain.ServiceJobLocationRef
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.ServiceJobRules
import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.data.local.ContractProofTestDatabase
import com.contractproof.data.local.SqlDelightEvidenceStore
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.local.SqlDelightSyncMetadataStore
import com.contractproof.data.local.SqlDelightSyncQueueStore
import com.contractproof.data.sync.SyncCoordinator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.contractproof.core.analytics.NoOpProductAnalytics
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class JobControllerTest {
    @Test
    fun startServiceMovesJobToInProgress() = runBlocking {
        val jobs = MemoryJobRepository()
        val job = sampleJob(status = ServiceJobRules.Scheduled)
        jobs.seed(job)
        val controller = testJobController(jobs)
        controller.load(job.id)
        controller.refresh()
        controller.startService()
        assertEquals(ServiceJobRules.InProgress, jobs.lastStatus)
        assertFalse(controller.jobState.value.canStart)
    }

    @Test
    fun finishBlockedUntilMandatoryComplete() = runBlocking {
        val jobs = MemoryJobRepository()
        val job = sampleJob(status = ServiceJobRules.InProgress)
        jobs.seed(job)
        val controller = testJobController(jobs)
        controller.load(job.id)
        controller.refresh()
        assertFalse(controller.finishService())
    }

    @Test
    fun preparePhotoRejectsWrongRequirementId() = runBlocking {
        val jobs = MemoryJobRepository()
        val job = sampleJob(status = ServiceJobRules.InProgress)
        jobs.seed(job)
        val controller = testJobController(jobs)
        controller.load(job.id)
        controller.refresh()
        controller.loadTask("req-1")
        controller.preparePhoto(
            "req-other",
            CapturedPhoto(localPath = "/tmp/x.jpg", bytes = byteArrayOf(1)),
        )
        assertEquals(
            "Evidence does not match the open task requirement.",
            controller.taskState.value.banner,
        )
    }

    @Test
    fun finishSucceedsWhenCoverageComplete() = runBlocking {
        val jobs = MemoryJobRepository()
        val job = sampleJob(status = ServiceJobRules.InProgress).let { current ->
            current.copy(
                requirements = current.requirements.map {
                    it.copy(status = com.contractproof.domain.JobRequirementStatus.Satisfied)
                },
                evidenceStatus = ServiceJobEvidenceStatus(1, 1, 100, ServiceJobEvidenceState.ReadyToComplete),
            )
        }
        jobs.seed(job)
        val controller = testJobController(jobs)
        controller.load(job.id)
        controller.refresh()
        assertTrue(controller.finishService())
        assertEquals(ServiceJobRules.Completed, jobs.lastStatus)
    }

    private fun sampleJob(status: String): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Acme"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("user-1", "Cleaner"),
            status = status,
            requirements = listOf(
                com.contractproof.domain.ServiceJobRequirement(
                    id = "req-1",
                    contractRequirementId = "cr-1",
                    requirementText = "Vacuum",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = com.contractproof.domain.JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.NotStarted),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = "schedule-1",
        )
    }
}

private class FixedOrg : OrganizationGateway {
    override suspend fun currentMembership(): Membership {
        return Membership(
            organizationId = "org-1",
            organizationName = "Northside",
            role = "cleaner",
            userId = "user-1",
        )
    }

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        throw OrganizationFailure.Rejected
    }
}

private fun testJobController(jobs: ServiceJobRepository): JobController {
    val database = ContractProofTestDatabase.create()
    val drafts = ServiceExecutionDraftStore()
    val syncCoordinator = SyncCoordinator(
        organizations = FixedOrg(),
        queue = SqlDelightSyncQueueStore(database),
        metadata = SqlDelightSyncMetadataStore(database),
        evidence = MemoryEvidence(),
        exceptions = MemoryExceptions(),
        serviceJobs = jobs,
        exceptionStore = SqlDelightExceptionStore(database),
    )
    return JobController(
        organizations = FixedOrg(),
        serviceJobs = jobs,
        evidence = MemoryEvidence(),
        exceptions = MemoryExceptions(),
        drafts = drafts,
        draftHydrator = ServiceExecutionDraftHydrator(
            SqlDelightEvidenceStore(database),
            SqlDelightExceptionStore(database),
        ),
        syncCoordinator = syncCoordinator,
        evidenceStore = SqlDelightEvidenceStore(database),
        analytics = NoOpProductAnalytics(),
    )
}

private class MemoryJobRepository : ServiceJobRepository {
    private var job: ServiceJob? = null
    var lastStatus: String = ""

    fun seed(value: ServiceJob) {
        job = value
        lastStatus = value.status
    }

    override suspend fun generateForApprovedVersion(contractId: String, contractVersionId: String, horizonDays: Int) = Unit

    override suspend fun get(jobId: String): ServiceJob? = job

    override suspend fun listAssignedOn(serviceDate: LocalDate, assigneeUserId: String): List<ServiceJob> = emptyList()

    override suspend fun listForOrganizationOn(serviceDate: LocalDate): List<ServiceJob> = emptyList()

    override suspend fun listForContract(contractId: String, fromDate: LocalDate, limit: Int): List<ServiceJob> =
        emptyList()

    override suspend fun start(jobId: String, startedAt: String): ServiceJob {
        job = job?.copy(status = ServiceJobRules.InProgress, startedAt = startedAt)
        lastStatus = ServiceJobRules.InProgress
        return job!!
    }

    override suspend fun complete(jobId: String, completedAt: String, completedBy: String): ServiceJob {
        job = job?.copy(status = ServiceJobRules.Completed, completedAt = completedAt, completedBy = completedBy)
        lastStatus = ServiceJobRules.Completed
        return job!!
    }

    override suspend fun markIncomplete(jobId: String): ServiceJob = job!!

    override suspend fun markDisputed(jobId: String): ServiceJob = job!!
}

private class MemoryEvidence : EvidenceRepository {
    var uploadCalls = 0
    var lastRetryRequirementId: String? = null

    override suspend fun getByRequirement(organizationId: String, serviceJobRequirementId: String): Evidence? = null

    override suspend fun listForJob(organizationId: String, serviceJobId: String): List<Evidence> = emptyList()

    override suspend fun upsert(evidence: Evidence) = Unit

    override suspend fun preparePhotoEvidence(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation?,
    ): Evidence {
        return Evidence(
            id = "ev-1",
            organizationId = "org-1",
            serviceJobId = jobId,
            serviceJobRequirementId = requirementId,
            capturedByUserId = "user-1",
            capturedAt = capturedAt,
            type = com.contractproof.domain.EvidenceType.Photo,
            location = null,
            file = null,
            syncStatus = com.contractproof.domain.EvidenceSyncStatus.Pending,
        )
    }

    override suspend fun uploadPhotoEvidence(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        uploadCalls++
        onProgress(50)
        onProgress(100)
        return EvidenceSubmitResult(uploaded = true, pending = false)
    }

    override suspend fun retryPhotoUpload(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult {
        lastRetryRequirementId = requirementId
        return uploadPhotoEvidence(jobId, requirementId, onProgress)
    }

    override suspend fun submitPhoto(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation?,
        onProgress: (Int) -> Unit,
    ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)

    override suspend fun submitChecklistCompletion(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)

    override suspend fun submitTimestamp(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation?,
    ): EvidenceSubmitResult = EvidenceSubmitResult(uploaded = true, pending = false)
}

private class MemoryExceptions : ExceptionGateway {
    override suspend fun submit(
        jobId: String,
        requirementId: String,
        reason: String,
        recordedAt: String,
    ): TaskExceptionResult = TaskExceptionResult(uploaded = true, pending = false)
}
