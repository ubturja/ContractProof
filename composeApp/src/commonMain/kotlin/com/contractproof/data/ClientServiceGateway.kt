package com.contractproof.data

import com.contractproof.domain.ServiceJob
import com.contractproof.domain.ServiceRecordSnapshot

sealed class ClientServiceFailure : Exception() {
    data object Network : ClientServiceFailure()

    data object Rejected : ClientServiceFailure()
}

interface ClientServiceGateway {
    suspend fun listCompletedForClient(): List<ServiceJob>

    suspend fun loadRecord(jobId: String): ServiceRecordSnapshot?
}
