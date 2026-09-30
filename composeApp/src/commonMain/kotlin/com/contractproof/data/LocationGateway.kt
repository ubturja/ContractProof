package com.contractproof.data

import com.contractproof.domain.LocationRecord

sealed class LocationFailure : Exception() {
    data object Network : LocationFailure()

    data object Rejected : LocationFailure()
}

interface LocationGateway {
    suspend fun list(): List<LocationRecord>

    suspend fun create(
        clientId: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
    ): LocationRecord

    suspend fun update(
        id: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
        status: String,
    ): LocationRecord
}
