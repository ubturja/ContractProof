package com.contractproof.domain

class EvidenceRuleViolation(message: String) : IllegalArgumentException(message)

object EvidenceRules {
    fun requireConsistentKeys(evidence: Evidence, job: ServiceJob) {
        if (evidence.organizationId != job.organizationId) {
            throw EvidenceRuleViolation("Evidence organization does not match the service job.")
        }
        if (evidence.serviceJobId != job.id) {
            throw EvidenceRuleViolation("Evidence service job does not match the service job.")
        }
        val requirement = job.requirements.firstOrNull { it.id == evidence.serviceJobRequirementId }
        if (requirement == null) {
            throw EvidenceRuleViolation("Evidence requirement is not on this service job.")
        }
    }

    fun requireCaptureProvenance(evidence: Evidence) {
        if (evidence.capturedByUserId.isBlank()) {
            throw EvidenceRuleViolation("Evidence must record who captured it.")
        }
        if (evidence.capturedAt.isBlank()) {
            throw EvidenceRuleViolation("Evidence must record when it was captured.")
        }
    }

    fun requireLocation(location: EvidenceLocation?, job: ServiceJob) {
        if (location == null) return
        val coords = location.coordinates
        if (coords != null) {
            if (coords.latitude !in -90.0..90.0 || coords.longitude !in -180.0..180.0) {
                throw EvidenceRuleViolation("Evidence coordinates are out of range.")
            }
            val accuracy = coords.horizontalAccuracyMeters
            if (accuracy != null && accuracy < 0) {
                throw EvidenceRuleViolation("Evidence location accuracy cannot be negative.")
            }
        }
        val locationId = location.locationId
        if (locationId != null && locationId != job.location.id) {
            throw EvidenceRuleViolation("Evidence location does not match the service job location.")
        }
    }

    fun requireFileForType(type: EvidenceType, file: EvidenceFile?) {
        when (type) {
            EvidenceType.Photo -> {
                val mime = file?.mimeType
                if (file == null || mime == null || !mime.startsWith("image/")) {
                    throw EvidenceRuleViolation("Photo evidence requires an image file.")
                }
            }
            EvidenceType.ChecklistCompletion,
            EvidenceType.Timestamp,
            -> Unit
        }
    }

    fun requireReadyToPersist(evidence: Evidence, job: ServiceJob) {
        requireConsistentKeys(evidence, job)
        requireCaptureProvenance(evidence)
        requireLocation(evidence.location, job)
        requireFileForType(evidence.type, evidence.file)
        if (evidence.file != null && evidence.file.evidenceRecordId != evidence.id) {
            throw EvidenceRuleViolation("Evidence file must belong to the evidence record.")
        }
    }

    fun isSameRequirement(existing: Evidence?, requirementId: String): Boolean {
        return existing?.serviceJobRequirementId == requirementId
    }
}
