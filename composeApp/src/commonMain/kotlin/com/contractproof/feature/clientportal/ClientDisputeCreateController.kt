package com.contractproof.feature.clientportal

import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.core.observability.ErrorLevel
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.data.DisputeFailure
import com.contractproof.data.DisputeGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.DisputeDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import com.contractproof.feature.dispute.DisputeCreateUiState

class ClientDisputeCreateController(
    private val organizations: OrganizationGateway,
    private val disputes: DisputeGateway,
    private val analytics: ProductAnalytics,
    private val errorReporter: ErrorReporter,
) {
    private val ui = MutableStateFlow(DisputeCreateUiState())
    val state: StateFlow<DisputeCreateUiState> = ui.asStateFlow()

    private var attachmentBytes: ByteArray? = null
    private var attachmentMimeType: String? = null
    private var presetJobId: String? = null
    private var presetRequirementId: String? = null

    fun prepare(jobId: String?, requirementId: String?) {
        presetJobId = jobId
        presetRequirementId = requirementId
    }

    suspend fun load() {
        val membership = organizations.currentMembership()
        if (membership == null || !Access.forMembership(membership.role).canFileClientDispute) {
            ui.update { it.copy(banner = "You cannot file a dispute.", loading = false) }
            return
        }
        ui.update { it.copy(loading = true, banner = null) }
        try {
            val jobs = disputes.listDisputableJobs()
            val selectedJobId = presetJobId ?: jobs.firstOrNull()?.id
            val job = jobs.firstOrNull { it.id == selectedJobId }
            ui.update {
                it.copy(
                    jobs = jobs,
                    loading = false,
                    selectedJobId = selectedJobId,
                    selectedRequirementId = presetRequirementId,
                    serviceDate = job?.serviceDate?.toString() ?: "",
                )
            }
        } catch (error: DisputeFailure) {
            ui.update {
                it.copy(
                    loading = false,
                    banner = when (error) {
                        DisputeFailure.Network -> "Could not load services. Check your connection."
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
        val date = job.serviceDate
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
                    message = "client_dispute_create_rejected",
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
