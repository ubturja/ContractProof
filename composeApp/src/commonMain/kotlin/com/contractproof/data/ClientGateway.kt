package com.contractproof.data

import com.contractproof.domain.ClientRecord

sealed class ClientFailure : Exception() {
    data object Network : ClientFailure()

    data object Rejected : ClientFailure()
}

interface ClientGateway {
    suspend fun list(): List<ClientRecord>

    suspend fun create(name: String): ClientRecord

    suspend fun update(id: String, name: String, status: String): ClientRecord
}
