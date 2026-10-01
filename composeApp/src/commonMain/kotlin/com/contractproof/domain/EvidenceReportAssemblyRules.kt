package com.contractproof.domain

object EvidenceReportAssemblyRules {
    fun build(bundle: DisputeReconstructionBundle): ServiceEvidenceReport {
        val dispute = bundle.dispute
        val job = bundle.job
        val requirementTextById = job.requirements.associate { it.id to it.requirementText }
        val disputedId = dispute.disputedServiceJobRequirementId

        val uploadedEvidence = bundle.evidence.filter { it.syncStatus == EvidenceSyncStatus.Uploaded }
        val uploadedExceptions = bundle.exceptions.filter { it.syncStatus == EvidenceSyncStatus.Uploaded }

        val refs = ReportEntityRefs(
            disputeId = dispute.id,
            organizationId = dispute.organizationId,
            contractId = job.contractId,
            contractVersionId = job.contractVersionId,
            serviceJobId = job.id,
            disputedServiceJobRequirementId = disputedId,
            disputeItemIds = bundle.items.map { it.id },
            serviceJobRequirementIds = job.requirements.map { it.id },
            evidenceRecordIds = uploadedEvidence.map { it.id },
            exceptionIds = uploadedExceptions.map { it.id },
            complaintAttachmentObjectPath = dispute.complaintAttachmentObjectPath,
        )

        val header = ServiceEvidenceReportHeader(
            clientName = dispute.clientName,
            locationName = dispute.locationName,
            serviceDate = dispute.serviceDate,
            complaint = dispute.complaint,
            disputedRequirementText = bundle.disputedRequirementText,
            contractTitle = bundle.contract.contractTitle,
            contractVersionLabel = bundle.contract.versionLabel,
        )

        val contractRequirements = job.requirements.sortedBy { it.sortOrder }.map { req ->
            ReportContractRequirementLine(
                serviceJobRequirementId = req.id,
                requirementText = req.requirementText,
                isDisputed = req.id == disputedId,
            )
        }

        val scheduledService = ReportScheduledService(
            scheduledStart = job.scheduledStart,
            scheduledEnd = job.scheduledEnd,
            serviceDate = job.serviceDate,
        )

        val assignedPersonnel = ReportAssignedPersonnel(
            displayName = job.assignee?.displayName ?: "Not recorded",
        )

        val evidenceLines = uploadedEvidence.map { record ->
            ReportEvidenceLine(
                evidenceRecordId = record.id,
                serviceJobRequirementId = record.serviceJobRequirementId,
                requirementText = requirementTextById[record.serviceJobRequirementId]
                    ?: record.serviceJobRequirementId,
                evidenceType = record.type,
                capturedAt = record.capturedAt,
                isPhoto = record.type == EvidenceType.Photo,
                objectPath = record.file?.objectPath,
                mimeType = record.file?.mimeType,
            )
        }

        val exceptionLines = uploadedExceptions.map { ex ->
            ReportExceptionLine(
                exceptionId = ex.id,
                serviceJobRequirementId = ex.serviceJobRequirementId,
                requirementText = requirementTextById[ex.serviceJobRequirementId]
                    ?: ex.serviceJobRequirementId,
                reason = ex.reason,
                recordedAt = ex.recordedAt,
            )
        }

        val acknowledgements = buildList {
            uploadedEvidence.filter {
                it.type == EvidenceType.ChecklistCompletion || it.type == EvidenceType.Timestamp
            }.forEach { record ->
                val label = when (record.type) {
                    EvidenceType.ChecklistCompletion -> "Checklist recorded"
                    EvidenceType.Timestamp -> "Timestamp recorded"
                    EvidenceType.Photo -> return@forEach
                }
                add(
                    ReportAcknowledgementLine(
                        label = label,
                        occurredAt = record.capturedAt,
                        detail = requirementTextById[record.serviceJobRequirementId] ?: "",
                    ),
                )
            }
            add(
                ReportAcknowledgementLine(
                    label = "Dispute filed",
                    occurredAt = dispute.createdAt,
                    detail = dispute.complaint,
                ),
            )
        }

        val coverage = bundle.items.map { item ->
            ReportCoverageLine(
                serviceJobRequirementId = item.serviceJobRequirementId,
                requirementText = requirementTextById[item.serviceJobRequirementId]
                    ?: item.serviceJobRequirementId,
                outcome = item.outcome,
            )
        }

        val attachments = buildList {
            val complaintPath = dispute.complaintAttachmentObjectPath
            val complaintMime = dispute.complaintAttachmentMimeType
            if (!complaintPath.isNullOrBlank()) {
                add(
                    ReportAttachmentLine(
                        label = "Complaint attachment",
                        objectPath = complaintPath,
                        mimeType = complaintMime,
                    ),
                )
            }
            uploadedEvidence.mapNotNull { it.file }.forEach { file ->
                add(
                    ReportAttachmentLine(
                        label = "Evidence file",
                        objectPath = file.objectPath,
                        mimeType = file.mimeType,
                    ),
                )
            }
        }

        return ServiceEvidenceReport(
            refs = refs,
            header = header,
            contractRequirements = contractRequirements,
            scheduledService = scheduledService,
            assignedPersonnel = assignedPersonnel,
            timeline = bundle.timeline,
            evidence = evidenceLines,
            exceptions = exceptionLines,
            acknowledgements = acknowledgements,
            coverage = coverage,
            attachments = attachments,
        )
    }
}
