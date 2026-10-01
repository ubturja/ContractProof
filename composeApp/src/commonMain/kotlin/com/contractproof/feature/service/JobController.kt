package com.contractproof.feature.service

import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.domain.EvidenceFailure
import com.contractproof.domain.EvidenceRepository
import com.contractproof.domain.EvidenceSyncStatus
import com.contractproof.data.ExceptionFailure
import com.contractproof.data.ExceptionGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ServiceJobFailure
import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.domain.ExceptionRuleViolation
import com.contractproof.domain.ExceptionRules
import com.contractproof.domain.RequirementDraftOverlay
import com.contractproof.domain.ServiceCompletionRules
import com.contractproof.domain.ServiceExecutionRules
import com.contractproof.domain.JobCoverageSnapshot
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.ServiceJobRules
import com.contractproof.domain.TaskEvidenceRuleViolation
import com.contractproof.domain.TaskEvidenceRules
import com.contractproof.domain.TaskExecutionState
import com.contractproof.domain.TaskNextAction
import com.contractproof.domain.SyncItemKind
import com.contractproof.domain.SyncQueueRules
import com.contractproof.data.local.SqlDelightEvidenceStore
import com.contractproof.data.sync.SyncCoordinator
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class TaskRowUi(
    val id: String,
    val text: String,
    val isMandatory: Boolean,
    val workStatus: com.contractproof.core.design.CpWorkStatus,
    val evidenceHint: String,
    val nextAction: TaskNextAction,
)

data class JobUiState(
    val jobId: String = "",
    val locationName: String = "",
    val clientName: String = "",
    val statusLabel: String = "",
    val startedAtLabel: String? = null,
    val completedAtLabel: String? = null,
    val coveragePercent: Int = 0,
    val missingMandatory: List<String> = emptyList(),
    val tasks: List<TaskRowUi> = emptyList(),
    val canStart: Boolean = false,
    val canFinish: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val banner: String? = null,
    val finishMessage: String? = null,
    val coverageUi: CoverageUiState = CoverageUiState(),
    val completionBlockerLabels: List<String> = emptyList(),
)

data class TaskUiState(
    val requirementId: String = "",
    val requirementText: String = "",
    val evidenceHint: String = "",
    val requiresPhoto: Boolean = false,
    val isMandatory: Boolean = false,
    val mandatoryLabel: String? = null,
    val capturedLocalPath: String? = null,
    val hasOnDeviceEvidence: Boolean = false,
    val canRetakePhoto: Boolean = false,
    val workStatus: com.contractproof.core.design.CpWorkStatus = com.contractproof.core.design.CpWorkStatus.Missing,
    val primaryAction: TaskNextAction = TaskNextAction.CapturePhoto,
    val canReportException: Boolean = true,
    val saving: Boolean = false,
    val banner: String? = null,
    val uploadPercent: Int? = null,
    val canRetryUpload: Boolean = false,
)

data class ExceptionUiState(
    val selectedReason: String = "",
    val note: String = "",
    val saving: Boolean = false,
    val banner: String? = null,
)

class JobController(
    private val organizations: OrganizationGateway,
    private val serviceJobs: ServiceJobRepository,
    private val evidence: EvidenceRepository,
    private val exceptions: ExceptionGateway,
    private val drafts: ServiceExecutionDraftStore,
    private val draftHydrator: ServiceExecutionDraftHydrator,
    private val syncCoordinator: SyncCoordinator,
    private val evidenceStore: SqlDelightEvidenceStore,
    private val analytics: ProductAnalytics,
) {
    private var currentJobId: String = ""
    private var cachedJob: ServiceJob? = null
    private var currentTaskRequirementId: String = ""

    private val jobUi = MutableStateFlow(JobUiState())
    val jobState: StateFlow<JobUiState> = jobUi.asStateFlow()

    private val taskUi = MutableStateFlow(TaskUiState())
    val taskState: StateFlow<TaskUiState> = taskUi.asStateFlow()

    private val exceptionUi = MutableStateFlow(ExceptionUiState())
    val exceptionState: StateFlow<ExceptionUiState> = exceptionUi.asStateFlow()

    fun load(jobId: String) {
        if (currentJobId != jobId) {
            drafts.clearJob()
            currentJobId = jobId
        }
    }

    suspend fun refresh() {
        if (currentJobId.isBlank()) {
            return
        }
        jobUi.update { it.copy(loading = true, banner = null) }
        try {
            val job = serviceJobs.get(currentJobId)
            if (job == null) {
                jobUi.update {
                    it.copy(loading = false, banner = "This job is unavailable.")
                }
                return
            }
            cachedJob = job
            draftHydrator.hydrate(drafts, job)
            applyJob(job)
        } catch (failure: ServiceJobFailure) {
            val cached = cachedJob
            if (failure == ServiceJobFailure.Network && cached != null) {
                draftHydrator.hydrate(drafts, cached)
                applyJob(cached)
                jobUi.update {
                    it.copy(
                        loading = false,
                        banner = "You are offline. Showing cached job details.",
                    )
                }
            } else {
                jobUi.update {
                    it.copy(
                        loading = false,
                        banner = when (failure) {
                            ServiceJobFailure.Network -> "You are offline. Showing cached job details."
                            ServiceJobFailure.Rejected -> "This job could not be loaded."
                        },
                    )
                }
            }
        }
    }

    suspend fun startService() {
        val job = cachedJob ?: return
        if (!jobUi.value.canStart) {
            return
        }
        jobUi.update { it.copy(saving = true, banner = null) }
        try {
            val startedAt = nowIso()
            val updated = serviceJobs.start(job.id, startedAt)
            cachedJob = updated
            if (updated.syncStatus == EvidenceSyncStatus.Pending) {
                syncCoordinator.enqueue(SyncItemKind.JobStart, job.id)
                jobUi.update {
                    it.copy(
                        saving = false,
                        banner = "Start is saved on this device and will sync later.",
                    )
                }
            }
            syncCoordinator.drain()
            draftHydrator.hydrate(drafts, updated)
            applyJob(updated)
            analytics.track(ProductEvent.ServiceStarted(job.id))
        } catch (failure: ServiceJobFailure) {
            jobUi.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ServiceJobFailure.Network -> "Start could not be saved. Try again when online."
                        ServiceJobFailure.Rejected -> "This service could not be started."
                    },
                )
            }
        }
    }

    suspend fun finishService(): Boolean {
        val job = cachedJob ?: return false
        val overlays = draftOverlays()
        if (!ServiceExecutionRules.canFinish(job.requirements, overlays)) {
            jobUi.update { it.copy(finishMessage = "Mandatory tasks are still missing.") }
            return false
        }
        val membership = organizations.currentMembership() ?: return false
        jobUi.update { it.copy(saving = true, finishMessage = null, banner = null) }
        val completedAt = nowIso()
        try {
            val updated = serviceJobs.complete(job.id, completedAt, membership.userId)
            cachedJob = updated
            if (updated.syncStatus == EvidenceSyncStatus.Pending) {
                syncCoordinator.enqueue(SyncItemKind.JobComplete, job.id)
                draftHydrator.hydrate(drafts, updated)
                jobUi.update {
                    it.copy(
                        saving = false,
                        banner = "Finish is saved on this device and will sync later.",
                    )
                }
                syncCoordinator.drain()
                applyJob(updated)
                analytics.track(ProductEvent.ServiceCompleted(job.id))
                return true
            }
            drafts.clearJob()
            syncCoordinator.drain()
            analytics.track(ProductEvent.ServiceCompleted(job.id))
            return true
        } catch (failure: ServiceJobFailure) {
            when (failure) {
                ServiceJobFailure.Network -> {
                    jobUi.update {
                        it.copy(
                            saving = false,
                            banner = "Finish is saved on this device and will sync later.",
                        )
                    }
                    return true
                }
                ServiceJobFailure.Rejected -> {
                    jobUi.update {
                        it.copy(
                            saving = false,
                            banner = "This service could not be finished.",
                        )
                    }
                    return false
                }
            }
        }
    }

    fun loadTask(requirementId: String) {
        val job = cachedJob ?: return
        val requirement = job.requirements.find { it.id == requirementId } ?: return
        currentTaskRequirementId = requirementId
        val overlay = overlayFor(requirement.id)
        val state = ServiceExecutionRules.effectiveState(requirement, overlay)
        val action = ServiceExecutionRules.nextAction(requirement, overlay)
        val evidenceDraft = drafts.evidenceDraft(requirementId)
        val hasDraft = evidenceDraft != null && requirement.requiresPhoto
        val onDevice = hasDraft && evidenceDraft?.syncStatus != EvidenceSyncStatus.Uploaded
        taskUi.value = TaskUiState(
            requirementId = requirementId,
            requirementText = requirement.requirementText,
            evidenceHint = ServiceExecutionDisplay.evidenceHint(requirement),
            requiresPhoto = requirement.requiresPhoto,
            isMandatory = requirement.isMandatory,
            mandatoryLabel = if (requirement.requiresPhoto && requirement.isMandatory) {
                "${ServiceExecutionDisplay.evidenceHint(requirement)} · ${RequirementRules.requiredLabel(true)}"
            } else {
                null
            },
            capturedLocalPath = if (hasDraft) evidenceDraft?.localPath else null,
            hasOnDeviceEvidence = onDevice,
            canRetakePhoto = requirement.requiresPhoto &&
                evidenceDraft != null &&
                evidenceDraft.syncStatus != EvidenceSyncStatus.Uploaded,
            workStatus = ServiceExecutionDisplay.taskWorkStatus(state, evidenceDraft),
            primaryAction = action,
            canReportException = state == TaskExecutionState.Missing,
            uploadPercent = evidenceDraft?.uploadPercent,
            canRetryUpload = evidenceDraft?.syncStatus == EvidenceSyncStatus.Failed,
        )
    }

    fun updateExceptionReason(value: String) {
        exceptionUi.update { it.copy(selectedReason = value, banner = null) }
    }

    fun updateExceptionNote(value: String) {
        exceptionUi.update { it.copy(note = value, banner = null) }
    }

    suspend fun preparePhoto(requirementId: String, photo: CapturedPhoto) {
        taskUi.update { it.copy(saving = true, banner = null) }
        try {
            requireActiveRequirement(requirementId)
            val prepared = evidence.preparePhotoEvidence(currentJobId, requirementId, photo, nowIso())
            val stagingPath = prepared.file?.localStagingPath ?: photo.localPath
            drafts.putEvidence(
                EvidenceDraft(
                    requirementId = requirementId,
                    localPath = stagingPath,
                    recordId = prepared.id,
                    syncStatus = EvidenceSyncStatus.Pending,
                ),
            )
            taskUi.update { it.copy(saving = false) }
            syncCoordinator.enqueue(SyncItemKind.EvidenceUpload, currentJobId, requirementId)
            loadTask(requirementId)
        } catch (violation: TaskEvidenceRuleViolation) {
            taskUi.update { it.copy(saving = false, banner = violation.message) }
        } catch (failure: EvidenceFailure) {
            taskUi.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        EvidenceFailure.Network -> "Photo saved on this device. Upload will retry later."
                        EvidenceFailure.Rejected -> "Photo could not be saved."
                    },
                )
            }
            syncCoordinator.enqueue(SyncItemKind.EvidenceUpload, currentJobId, requirementId)
        }
    }

    suspend fun uploadPhoto(requirementId: String) {
        try {
            requireActiveRequirement(requirementId)
        } catch (violation: TaskEvidenceRuleViolation) {
            taskUi.update { it.copy(banner = violation.message) }
            return
        }
        updateEvidenceDraftStatus(requirementId, EvidenceSyncStatus.Uploading, 0)
        syncCoordinator.enqueue(SyncItemKind.EvidenceUpload, currentJobId, requirementId)
        try {
            val result = evidence.uploadPhotoEvidence(
                jobId = currentJobId,
                requirementId = requirementId,
                onProgress = { percent ->
                    updateEvidenceDraftStatusInMemory(requirementId, EvidenceSyncStatus.Uploading, percent)
                },
            )
            if (result.uploaded) {
                analytics.track(ProductEvent.EvidenceAdded(currentJobId, requirementId))
                drafts.clearRequirement(requirementId)
            } else {
                updateEvidenceDraftStatus(requirementId, EvidenceSyncStatus.Failed, null)
            }
            refresh()
            loadTask(requirementId)
            syncCoordinator.drain()
        } catch (failure: EvidenceFailure) {
            updateEvidenceDraftStatus(requirementId, EvidenceSyncStatus.Failed, null)
            taskUi.update {
                it.copy(
                    banner = when (failure) {
                        EvidenceFailure.Network -> "Photo saved on this device. Upload will retry later."
                        EvidenceFailure.Rejected -> "Photo upload failed."
                    },
                )
            }
            refresh()
            loadTask(requirementId)
            syncCoordinator.drain()
        }
    }

    suspend fun retryPhotoUpload(requirementId: String) {
        try {
            requireActiveRequirement(requirementId)
        } catch (violation: TaskEvidenceRuleViolation) {
            taskUi.update { it.copy(banner = violation.message) }
            return
        }
        updateEvidenceDraftStatus(requirementId, EvidenceSyncStatus.Retrying, 0)
        loadTask(requirementId)
        syncCoordinator.requestImmediateRetry(
            SyncQueueRules.dedupeKeyFor(SyncItemKind.EvidenceUpload, currentJobId, requirementId),
        )
        refresh()
        loadTask(requirementId)
    }

    private fun requireActiveRequirement(requirementId: String) {
        val job = cachedJob ?: throw TaskEvidenceRuleViolation("This task is unavailable.")
        TaskEvidenceRules.requireMatchingRequirement(requirementId, currentTaskRequirementId)
        TaskEvidenceRules.requireRequirementOnJob(requirementId, job)
    }

    private suspend fun updateEvidenceDraftStatus(
        requirementId: String,
        status: EvidenceSyncStatus,
        uploadPercent: Int?,
    ) {
        updateEvidenceDraftStatusInMemory(requirementId, status, uploadPercent)
        val current = drafts.evidenceDraft(requirementId) ?: return
        evidenceStore.updateUploadPercent(current.recordId, uploadPercent)
    }

    private fun updateEvidenceDraftStatusInMemory(
        requirementId: String,
        status: EvidenceSyncStatus,
        uploadPercent: Int?,
    ) {
        val current = drafts.evidenceDraft(requirementId) ?: return
        drafts.putEvidence(
            current.copy(
                syncStatus = status,
                uploadPercent = uploadPercent,
            ),
        )
        if (currentTaskRequirementId == requirementId) {
            taskUi.update {
                it.copy(
                    workStatus = ServiceExecutionDisplay.workStatusForEvidenceDraft(
                        current.copy(syncStatus = status, uploadPercent = uploadPercent),
                    ),
                    uploadPercent = uploadPercent,
                    canRetryUpload = status == EvidenceSyncStatus.Failed,
                )
            }
        }
    }

    suspend fun markDone(requirementId: String) {
        taskUi.update { it.copy(saving = true, banner = null) }
        try {
            val result = evidence.submitChecklistCompletion(currentJobId, requirementId, nowIso())
            if (!result.uploaded) {
                drafts.putEvidence(
                    EvidenceDraft(
                        requirementId = requirementId,
                        localPath = "ack",
                        recordId = "ack",
                        syncStatus = EvidenceSyncStatus.Pending,
                    ),
                )
            } else {
                drafts.clearRequirement(requirementId)
            }
            refresh()
            taskUi.update { it.copy(saving = false) }
            loadTask(requirementId)
        } catch (failure: EvidenceFailure) {
            drafts.putEvidence(
                EvidenceDraft(
                    requirementId = requirementId,
                    localPath = "ack",
                    recordId = "ack",
                    syncStatus = EvidenceSyncStatus.Failed,
                ),
            )
            taskUi.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        EvidenceFailure.Network -> "Marked on this device. Upload will retry later."
                        EvidenceFailure.Rejected -> "Task could not be marked done."
                    },
                )
            }
            refresh()
        }
    }

    suspend fun saveException(requirementId: String): Boolean {
        val current = exceptionUi.value
        exceptionUi.update { it.copy(saving = true, banner = null) }
        try {
            val formatted = ExceptionRules.formatReason(current.selectedReason, current.note)
            val result = exceptions.submit(currentJobId, requirementId, formatted, nowIso())
            if (!result.uploaded) {
                drafts.putException(
                    ExceptionDraft(
                        requirementId = requirementId,
                        reason = current.selectedReason,
                        note = current.note,
                        pending = true,
                    ),
                )
            } else {
                drafts.clearRequirement(requirementId)
            }
            if (result.uploaded) {
                analytics.track(ProductEvent.ExceptionCreated(currentJobId, requirementId))
            }
            exceptionUi.value = ExceptionUiState()
            refresh()
            return true
        } catch (error: ExceptionRuleViolation) {
            exceptionUi.update { it.copy(saving = false, banner = error.message) }
            return false
        } catch (failure: ExceptionFailure) {
            drafts.putException(
                ExceptionDraft(
                    requirementId = requirementId,
                    reason = current.selectedReason,
                    note = current.note,
                    pending = true,
                    failed = failure == ExceptionFailure.Network,
                ),
            )
            exceptionUi.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ExceptionFailure.Network -> "Exception saved on this device."
                        ExceptionFailure.Rejected -> "Exception could not be saved."
                    },
                )
            }
            refresh()
            return failure == ExceptionFailure.Network
        }
    }

    private fun applyJob(job: ServiceJob) {
        val overlays = draftOverlays()
        val snapshot = ServiceExecutionRules.coverageSnapshot(job.requirements, overlays)
        val coverage = ServiceExecutionRules.countsForCoverage(job.requirements, overlays)
        val blockers = ServiceCompletionRules.completionBlockers(snapshot)
        val missing = blockers.map { it.label }
        val coverageUi = CoverageDisplayMapper.toUiState(snapshot, job.status)
        val tasks = job.requirements.sortedBy { it.sortOrder }.map { requirement ->
            val overlay = overlays[requirement.id]
            val state = ServiceExecutionRules.effectiveState(requirement, overlay)
            val action = ServiceExecutionRules.nextAction(requirement, overlay)
            val evidenceDraft = drafts.evidenceDraft(requirement.id)
            TaskRowUi(
                id = requirement.id,
                text = requirement.requirementText,
                isMandatory = requirement.isMandatory,
                workStatus = ServiceExecutionDisplay.taskWorkStatus(state, evidenceDraft),
                evidenceHint = ServiceExecutionDisplay.evidenceHint(requirement),
                nextAction = action,
            )
        }
        jobUi.update {
            it.copy(
                jobId = job.id,
                locationName = job.location.name,
                clientName = job.client.name,
                statusLabel = ServiceJobDisplay.statusLabel(job.status),
                startedAtLabel = job.startedAt?.let(::formatTimestamp),
                completedAtLabel = job.completedAt?.let(::formatTimestamp),
                coveragePercent = coverage.coveragePercent,
                missingMandatory = missing,
                tasks = tasks,
                canStart = job.status == ServiceJobRules.Scheduled,
                canFinish = ServiceCompletionRules.canCompleteJob(snapshot, job.status),
                loading = false,
                saving = false,
                finishMessage = null,
                coverageUi = coverageUi,
                completionBlockerLabels = missing,
            )
        }
    }

    fun currentCoverageSnapshot(): JobCoverageSnapshot? {
        val job = cachedJob ?: return null
        return ServiceExecutionRules.coverageSnapshot(job.requirements, draftOverlays())
    }

    private fun draftOverlays(): Map<String, RequirementDraftOverlay> {
        val result = mutableMapOf<String, RequirementDraftOverlay>()
        drafts.allEvidenceDrafts().forEach { (_, draft) ->
            result[draft.requirementId] = ServiceExecutionDisplay.overlayFromDrafts(
                draft.requirementId,
                draft,
                drafts.exceptionDraft(draft.requirementId),
            ) ?: RequirementDraftOverlay(
                requirementId = draft.requirementId,
                evidencePending = draft.syncStatus != EvidenceSyncStatus.Uploaded,
                evidenceSyncStatus = draft.syncStatus,
            )
        }
        drafts.allExceptionDrafts().forEach { (_, draft) ->
            result[draft.requirementId] = ServiceExecutionDisplay.overlayFromDrafts(
                draft.requirementId,
                drafts.evidenceDraft(draft.requirementId),
                draft,
            ) ?: RequirementDraftOverlay(
                requirementId = draft.requirementId,
                exceptionPending = draft.pending,
                exceptionFailed = draft.failed,
            )
        }
        return result
    }

    private fun overlayFor(requirementId: String): RequirementDraftOverlay? {
        return draftOverlays()[requirementId]
    }

    private fun nowIso(): String {
        return kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toString()
    }

    private fun formatTimestamp(value: String): String {
        return try {
            val instant = Instant.parse(value)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.date} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
        } catch (_: Throwable) {
            value
        }
    }
}
