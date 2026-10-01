package com.contractproof.feature.dispute

import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.core.observability.ErrorLevel
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.DisputeDraft
import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceJobRequirement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

data class DisputeCreateUiState(
    val jobs: List<ServiceJob> = emptyList(),
    val selectedJobId: String? = null,
    val selectedRequirementId: String? = null,
    val complaint: String = "",
    val serviceDate: String = "",
    val attachmentFileName: String? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val banner: String? = null,
) {
    val selectedJob: ServiceJob?
        get() = jobs.firstOrNull { it.id == selectedJobId }

    val requirements: List<ServiceJobRequirement>
        get() = selectedJob?.requirements?.sortedBy { it.sortOrder } ?: emptyList()

    val canSubmit: Boolean
        get() = !saving &&
            !loading &&
            selectedJobId != null &&
            selectedRequirementId != null &&
            complaint.trim().isNotEmpty() &&
            serviceDate.isNotBlank()
}

class DisputeCreateController(
    private val organizations: OrganizationGateway,
    private val disputes: DisputeGateway,
    private val analytics: ProductAnalytics,
    private val errorReporter: ErrorReporter,
) {
    private val ui = MutableStateFlow(DisputeCreateUiState())
    val state: StateFlow<DisputeCreateUiState> = ui.asStateFlow()

    private var attachmentBytes: ByteArray? = null
    private var attachmentMimeType: String? = null

    suspend fun load() {
        val membership = organizations.currentMembership()
        if (membership == null || !Access.forMembership(membership.role).canWriteDisputes) {
            ui.update { it.copy(banner = "You cannot file a dispute.", loading = false) }
            return
        }
        ui.update { it.copy(loading = true, banner = null) }
        try {
            val jobs = disputes.listDisputableJobs()
            ui.update {
                it.copy(
                    jobs = jobs,
                    loading = false,
                    selectedJobId = it.selectedJobId ?: jobs.firstOrNull()?.id,
                    serviceDate = it.serviceDate.ifBlank {
                        jobs.firstOrNull()?.serviceDate?.toString() ?: ""
                    },
                )
            }
        } catch (error: DisputeFailure) {
            ui.update {
                it.copy(
                    loading = false,
                    banner = when (error) {
                        DisputeFailure.Network -> "Could not load jobs. Check your connection."
                        is DisputeFailure.Rejected -> error.userMessage
                    },
                )
            }
        }
    }

    fun selectJob(jobId: String) {
        val job = ui.value.jobs.firstOrNull { it.id == jobId }
        ui.update {
            it.copy(
                selectedJobId = jobId,
                selectedRequirementId = null,
                serviceDate = job?.serviceDate?.toString() ?: it.serviceDate,
            )
        }
    }

    fun selectRequirement(requirementId: String) {
        ui.update { it.copy(selectedRequirementId = requirementId) }
    }

    fun updateComplaint(value: String) {
        ui.update { it.copy(complaint = value) }
    }

    fun updateServiceDate(value: String) {
        ui.update { it.copy(serviceDate = value) }
    }

    fun setAttachment(fileName: String, bytes: ByteArray, mimeType: String) {
        attachmentBytes = bytes
        attachmentMimeType = mimeType
        ui.update { it.copy(attachmentFileName = fileName) }
    }

    fun clearAttachment() {
        attachmentBytes = null
        attachmentMimeType = null
        ui.update { it.copy(attachmentFileName = null) }
    }

    suspend fun submit(onCreated: (String) -> Unit) {
        val current = ui.value
        val job = current.selectedJob ?: return
        val requirementId = current.selectedRequirementId ?: return
        val date = runCatching { LocalDate.parse(current.serviceDate.trim()) }.getOrNull()
        if (date == null) {
            ui.update { it.copy(banner = "Enter a valid service date (YYYY-MM-DD).") }
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val draft = DisputeDraft(
                serviceJobId = job.id,
                disputedServiceJobRequirementId = requirementId,
                complaint = current.complaint,
                serviceDate = date,
                attachmentBytes = attachmentBytes,
                attachmentMimeType = attachmentMimeType,
            )
            val created = disputes.create(draft)
            analytics.track(ProductEvent.DisputeCreated(created.id))
            ui.update { it.copy(saving = false) }
            onCreated(created.id)
        } catch (error: DisputeFailure) {
            if (error is DisputeFailure.Rejected) {
                errorReporter.captureMessage(
                    message = "dispute_create_rejected",
                    level = ErrorLevel.Error,
                    tags = mapOf("feature" to "disputes"),
                )
            }
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (error) {
                        DisputeFailure.Network -> "Could not save dispute. Check your connection."
                        is DisputeFailure.Rejected -> error.userMessage
                    },
                )
            }
        }
    }
}
