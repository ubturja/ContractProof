package com.contractproof.data

import com.contractproof.domain.ContractExtractionV1
import com.contractproof.domain.ContractRecord
import com.contractproof.domain.ContractVersionRecord
import com.contractproof.domain.LocationRecord

data class ContractVersionDetail(
    val id: String,
    val contractId: String,
    val organizationId: String,
    val status: String,
    val documentFileName: String?,
    val documentPath: String?,
    val extraction: ContractExtractionV1?,
)

sealed class ContractFailure : Exception() {
    data object Network : ContractFailure()

    data object Rejected : ContractFailure()

    data object Upload : ContractFailure()
}

data class NewContractDocument(
    val fileName: String,
    val bytes: ByteArray,
)

interface ContractGateway {
    suspend fun list(): List<ContractRecord>

    suspend fun listVersions(contractId: String): List<ContractVersionRecord>

    suspend fun create(
        clientId: String,
        location: LocationRecord,
        title: String,
        startsOn: String,
        endsOn: String,
    ): ContractRecord

    suspend fun attachDocument(
        contractId: String,
        document: NewContractDocument,
        effectiveOn: String,
        onProgress: (Int) -> Unit,
    ): ContractRecord

    suspend fun activateVersion(contractId: String, versionId: String): ContractRecord

    suspend fun getVersion(contractId: String, versionId: String): ContractVersionDetail

    suspend fun approveVersion(contractId: String, versionId: String): ContractRecord

    suspend fun update(
        id: String,
        title: String,
        startsOn: String,
        endsOn: String,
        status: String,
    ): ContractRecord

    suspend fun documentUrl(path: String): String
}
