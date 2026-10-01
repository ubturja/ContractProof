package com.contractproof.feature.extraction

import com.contractproof.data.ContractFailure
import com.contractproof.data.ContractGateway
import com.contractproof.data.ExtractionFailure
import com.contractproof.data.ExtractionGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.RequirementDraft
import com.contractproof.data.RequirementFailure
import com.contractproof.data.RequirementGateway
import com.contractproof.data.ScheduleFailure
import com.contractproof.data.ScheduleGateway
import com.contractproof.domain.Access
import com.contractproof.domain.ContractExtractionV1
import com.contractproof.domain.ContractVersionRules
import com.contractproof.domain.Entitlements
import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.analytics.ProductEvent
import com.contractproof.core.observability.ErrorLevel
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.domain.SubscriptionService
import com.contractproof.domain.ExtractionReviewRuleViolation
import com.contractproof.domain.ExtractionReviewRules
import com.contractproof.domain.ExtractionRules
import com.contractproof.domain.ExtractionVisitCandidate
import com.contractproof.domain.RequirementFrequency
import com.contractproof.domain.RequirementRules
import com.contractproof.domain.RequirementVisit
import com.contractproof.domain.ReviewRequirementDraft
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout

enum class ExtractionReviewPhase {
    Loading,
    Processing,
    Review,
    Error,
    Empty,
}

data class ExtractionReviewUiState(
    val contractId: String = "",
    val versionId: String = "",
    val documentFileName: String? = null,
    val versionStatus: String = "",
    val phase: ExtractionReviewPhase = ExtractionReviewPhase.Loading,
    val drafts: List<ReviewRequirementDraft> = emptyList(),
    val warnings: List<String> = emptyList(),
    val usedFallback: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
    val canExtract: Boolean = false,
    val saving: Boolean = false,
    val editingId: String? = null,
    val draftTask: String = "",
    val draftRequiresPhoto: Boolean = true,
    val draftIsMandatory: Boolean = true,
    val visitDraft: ExtractionVisitCandidate? = null,
    val scheduleId: String? = null,
    val defaultTimezone: String = "",
    val contractStartsOn: String = "",
    val needsUpgrade: Boolean = false,
) {
    val unapprovedBanner: String
        get() = ExtractionReviewRules.UnapprovedBanner

    val canApprove: Boolean
        get() = canWrite && !saving && phase != ExtractionReviewPhase.Processing &&
            drafts.any { it.task.trim().isNotEmpty() }

    val canSaveDraft: Boolean
        get() = canWrite && draftTask.trim().isNotEmpty() && !saving
}

class ExtractionReviewController(
    private val organizations: OrganizationGateway,
    private val contracts: ContractGateway,
    private val extractions: ExtractionGateway,
    private val requirements: RequirementGateway,
    private val schedules: ScheduleGateway,
    private val subscription: SubscriptionService,
    private val analytics: ProductAnalytics,
    private val errorReporter: ErrorReporter,
) {
    private val ui = MutableStateFlow(ExtractionReviewUiState())
    val state: StateFlow<ExtractionReviewUiState> = ui.asStateFlow()

    fun updateDraftTask(value: String) {
        ui.update { it.copy(draftTask = value) }
    }

    fun updateDraftRequiresPhoto(value: Boolean) {
        ui.update { it.copy(draftRequiresPhoto = value) }
    }

    fun updateDraftIsMandatory(value: Boolean) {
        ui.update { it.copy(draftIsMandatory = value) }
    }

    fun startNewDraft() {
        ui.update {
            it.copy(
                editingId = null,
                draftTask = "",
                draftRequiresPhoto = true,
                draftIsMandatory = true,
            )
        }
    }

    fun startEditDraft(localId: String) {
        val draft = ui.value.drafts.firstOrNull { it.localId == localId } ?: return
        ui.update {
            it.copy(
                editingId = localId,
                draftTask = draft.task,
                draftRequiresPhoto = draft.requiresPhoto,
                draftIsMandatory = draft.isMandatory,
            )
        }
    }

    fun cancelEdit() {
        ui.update {
            it.copy(
                editingId = null,
                draftTask = "",
                draftRequiresPhoto = true,
                draftIsMandatory = true,
            )
        }
    }

    fun saveDraft() {
        val current = ui.value
        if (!current.canSaveDraft) return
        val task = current.draftTask.trim()
        val updated = if (current.editingId != null) {
            current.drafts.map { draft ->
                if (draft.localId == current.editingId) {
                    draft.copy(
                        task = task,
                        requiresPhoto = current.draftRequiresPhoto,
                        isMandatory = current.draftIsMandatory,
                        fromExtraction = draft.fromExtraction && draft.extractionKey != null,
                    )
                } else {
                    draft
                }
            }
        } else {
            current.drafts + ExtractionReviewRules.newManualDraft().copy(
                task = task,
                requiresPhoto = current.draftRequiresPhoto,
                isMandatory = current.draftIsMandatory,
            )
        }
        ui.update {
            it.copy(
                drafts = updated,
                phase = if (updated.isEmpty()) ExtractionReviewPhase.Empty else ExtractionReviewPhase.Review,
                editingId = null,
                draftTask = "",
                draftRequiresPhoto = true,
                draftIsMandatory = true,
                banner = null,
            )
        }
    }

    fun deleteDraft(localId: String) {
        if (!ui.value.canWrite) return
        val updated = ui.value.drafts.filterNot { it.localId == localId }
        ui.update {
            it.copy(
                drafts = updated,
                phase = when {
                    updated.isEmpty() -> ExtractionReviewPhase.Empty
                    else -> ExtractionReviewPhase.Review
                },
                editingId = if (it.editingId == localId) null else it.editingId,
            )
        }
    }

    fun moveDraft(localId: String, delta: Int) {
        if (!ui.value.canWrite) return
        val moved = ExtractionReviewRules.moveInOrder(ui.value.drafts, localId, delta)
        ui.update { it.copy(drafts = moved) }
    }

    suspend fun load(contractId: String, versionId: String) {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenContracts) {
            ui.update {
                it.copy(
                    phase = ExtractionReviewPhase.Error,
                    banner = "Contract review is not available.",
                )
            }
            return
        }
        ui.update {
            it.copy(
                contractId = contractId,
                versionId = versionId,
                phase = ExtractionReviewPhase.Loading,
                banner = null,
                canWrite = access.canWriteContracts,
                canExtract = Entitlements.canExtractContracts(access, subscription.state.value),
                needsUpgrade = false,
            )
        }
        try {
            val version = contracts.getVersion(contractId, versionId)
            val header = requirements.headerForVersion(versionId)
            val scheduleItems = schedules.listForContract(contractId)
            val visit = version.extraction?.visits?.firstOrNull()
            ui.update {
                it.copy(
                    documentFileName = version.documentFileName,
                    versionStatus = version.status,
                    defaultTimezone = scheduleItems.firstOrNull()?.visit?.timezone
                        ?: visit?.timezone.orEmpty(),
                    contractStartsOn = header.contractStartsOn,
                    scheduleId = scheduleItems.firstOrNull()?.id,
                    visitDraft = visit,
                    warnings = version.extraction?.document?.warnings.orEmpty(),
                    usedFallback = version.extraction?.pipeline?.usedFallback == true,
                )
            }
            if (version.status == ContractVersionRules.Extracted && version.extraction != null) {
                applyExtraction(version.extraction)
            } else if (version.documentPath.isNullOrEmpty()) {
                ui.update {
                    it.copy(
                        phase = ExtractionReviewPhase.Error,
                        banner = "This version has no contract PDF to read.",
                    )
                }
            } else {
                ui.update { it.copy(phase = ExtractionReviewPhase.Review) }
            }
        } catch (failure: ContractFailure) {
            ui.update {
                it.copy(
                    phase = ExtractionReviewPhase.Error,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to open this contract."
                        else -> "This contract could not be opened."
                    },
                )
            }
        } catch (failure: RequirementFailure) {
            ui.update {
                it.copy(
                    phase = ExtractionReviewPhase.Error,
                    banner = when (failure) {
                        RequirementFailure.Network -> "You need a connection to open this contract."
                        else -> "This contract could not be opened."
                    },
                )
            }
        }
    }

    fun clearUpgradeSignal() {
        ui.update { it.copy(needsUpgrade = false) }
    }

    suspend fun startExtraction(force: Boolean = false) {
        val current = ui.value
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        val canExtract = Entitlements.canExtractContracts(access, subscription.state.value)
        if (!canExtract || current.saving) {
            if (!canExtract) {
                ui.update {
                    it.copy(
                        banner = "AI contract extraction requires Pro. Open plans to upgrade.",
                        needsUpgrade = true,
                        canExtract = false,
                    )
                }
            }
            return
        }
        ui.update {
            it.copy(
                phase = ExtractionReviewPhase.Processing,
                banner = null,
                saving = true,
            )
        }
        try {
            withTimeout(120_000) {
                extractions.runExtraction(current.versionId, force)
            }
            val extraction = extractions.loadExtraction(current.versionId)
            if (extraction == null) {
                ui.update {
                    it.copy(
                        phase = ExtractionReviewPhase.Error,
                        saving = false,
                        banner = "The requirements could not be read from the contract.",
                    )
                }
                return
            }
            applyExtraction(extraction)
            analytics.track(
                ProductEvent.ContractExtractionCompleted(
                    contractId = current.contractId,
                    versionId = current.versionId,
                ),
            )
            ui.update { it.copy(saving = false, versionStatus = ContractVersionRules.Extracted) }
        } catch (_: TimeoutCancellationException) {
            ui.update {
                it.copy(
                    phase = ExtractionReviewPhase.Error,
                    saving = false,
                    banner = "Reading the contract took too long. Retry or enter requirements manually.",
                )
            }
        } catch (failure: ExtractionFailure) {
            if (failure is ExtractionFailure.Rejected) {
                errorReporter.captureMessage(
                    message = "contract_extraction_rejected",
                    level = ErrorLevel.Error,
                    tags = mapOf(
                        "feature" to "extraction",
                        "contract_id" to current.contractId,
                        "version_id" to current.versionId,
                    ),
                )
            }
            ui.update {
                it.copy(
                    phase = ExtractionReviewPhase.Error,
                    saving = false,
                    banner = when (failure) {
                        ExtractionFailure.Network ->
                            "You need a connection to read the contract."
                        is ExtractionFailure.Rejected -> failure.userMessage
                    },
                )
            }
        }
    }

    suspend fun retryExtraction() {
        startExtraction(force = true)
    }

    suspend fun approve(): Boolean {
        val current = ui.value
        if (!current.canApprove) return false
        ui.update { it.copy(saving = true, banner = null) }
        return try {
            ExtractionReviewRules.requireCanApprove(current.drafts)
            val items = current.drafts
                .filter { it.task.trim().isNotEmpty() }
                .map { draft ->
                    RequirementDraft(
                        task = draft.task,
                        requiresPhoto = draft.requiresPhoto,
                        isMandatory = draft.isMandatory,
                        extractionKey = draft.extractionKey,
                    )
                }
            requirements.replaceAllForVersion(current.versionId, items)
            current.visitDraft?.let { visit ->
                val normalized = RequirementRules.requireVisit(
                    RequirementVisit(
                        weekday = visit.weekday,
                        startTime = visit.startTime,
                        endTime = visit.endTime,
                        timezone = visit.timezone.ifEmpty { current.defaultTimezone },
                        startsOn = visit.startsOn.ifEmpty { current.contractStartsOn },
                        endsOn = visit.endsOn,
                        status = RequirementRules.ScheduleActive,
                    ),
                )
                RequirementRules.requireWeekdayWhenWeekly(RequirementFrequency.Weekly, normalized.weekday)
                schedules.upsertWeeklyVisit(
                    contractVersionId = current.versionId,
                    scheduleId = current.scheduleId,
                    visit = normalized,
                )
            }
            contracts.approveVersion(current.contractId, current.versionId)
            ui.update { it.copy(saving = false) }
            true
        } catch (_: ExtractionReviewRuleViolation) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = "Add at least one requirement before approving.",
                )
            }
            false
        } catch (failure: RequirementFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        RequirementFailure.Network -> "You need a connection to approve this contract."
                        RequirementFailure.Rejected -> "Requirements could not be saved."
                    },
                )
            }
            false
        } catch (failure: ScheduleFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ScheduleFailure.Network -> "You need a connection to approve this contract."
                        ScheduleFailure.Rejected -> "The visit schedule could not be saved."
                    },
                )
            }
            false
        } catch (failure: ContractFailure) {
            ui.update {
                it.copy(
                    saving = false,
                    banner = when (failure) {
                        ContractFailure.Network -> "You need a connection to approve this contract."
                        else -> "The contract was not approved."
                    },
                )
            }
            false
        }
    }

    private fun applyExtraction(extraction: ContractExtractionV1) {
        val drafts = try {
            ExtractionReviewRules.draftsFromExtraction(extraction)
        } catch (_: Exception) {
            emptyList()
        }
        val visit = extraction.visits.firstOrNull()
        ui.update {
            it.copy(
                drafts = drafts,
                visitDraft = visit,
                warnings = extraction.document.warnings,
                usedFallback = extraction.pipeline.usedFallback,
                phase = when {
                    drafts.isEmpty() -> ExtractionReviewPhase.Empty
                    else -> ExtractionReviewPhase.Review
                },
                banner = if (drafts.isEmpty()) {
                    "No requirements were found. Add them manually or retry."
                } else {
                    null
                },
            )
        }
    }
}
