package com.contractproof.data

import com.contractproof.domain.Dispute
import com.contractproof.domain.DisputeAiSummary
import com.contractproof.domain.DisputeDraft
import com.contractproof.domain.DisputeReconstructionBundle
import com.contractproof.domain.ServiceJob

sealed class DisputeFailure : Exception() {
    data object Network : DisputeFailure()

    data class Rejected(val userMessage: String) : DisputeFailure()
}

interface DisputeGateway {
    suspend fun list(): List<Dispute>

    suspend fun get(id: String): Dispute?

    suspend fun listDisputableJobs(): List<ServiceJob>

    suspend fun create(draft: DisputeDraft): Dispute

    suspend fun getReconstruction(disputeId: String): DisputeReconstructionBundle?

    suspend fun requestSummary(disputeId: String): DisputeAiSummary
}
