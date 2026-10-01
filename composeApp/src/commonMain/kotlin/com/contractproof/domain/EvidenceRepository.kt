package com.contractproof.domain

import com.contractproof.core.platform.CapturedPhoto

interface EvidenceRepository {
    suspend fun getByRequirement(organizationId: String, serviceJobRequirementId: String): Evidence?

    suspend fun listForJob(organizationId: String, serviceJobId: String): List<Evidence>

    suspend fun upsert(evidence: Evidence)

    suspend fun preparePhotoEvidence(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation? = null,
    ): Evidence

    suspend fun uploadPhotoEvidence(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit = {},
    ): EvidenceSubmitResult

    suspend fun retryPhotoUpload(
        jobId: String,
        requirementId: String,
        onProgress: (Int) -> Unit = {},
    ): EvidenceSubmitResult

    suspend fun submitPhoto(
        jobId: String,
        requirementId: String,
        photo: CapturedPhoto,
        capturedAt: String,
        location: EvidenceLocation? = null,
        onProgress: (Int) -> Unit = {},
    ): EvidenceSubmitResult

    suspend fun submitChecklistCompletion(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation? = null,
    ): EvidenceSubmitResult

    suspend fun submitTimestamp(
        jobId: String,
        requirementId: String,
        capturedAt: String,
        location: EvidenceLocation? = null,
    ): EvidenceSubmitResult
}
