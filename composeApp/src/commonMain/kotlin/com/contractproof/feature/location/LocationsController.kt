package com.contractproof.feature.location

import com.contractproof.data.LocationFailure
import com.contractproof.data.LocationGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.domain.Access
import com.contractproof.domain.LocationRecord
import com.contractproof.domain.LocationRules
import com.contractproof.domain.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LocationsUiState(
    val items: List<LocationRecord> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val banner: String? = null,
    val canWrite: Boolean = false,
    val draftName: String = "",
    val draftTimezone: String = "",
    val draftAddress: String = "",
    val draftZoneCode: String = "",
) {
    val canCreate: Boolean
        get() = canWrite &&
            draftName.trim().isNotEmpty() &&
            draftTimezone.trim().isNotEmpty() &&
            !loading &&
            !saving

    fun forClient(clientId: String): List<LocationRecord> {
        return LocationRules.forClient(items, clientId)
    }
}

class LocationsController(
    private val organizations: OrganizationGateway,
    private val locations: LocationGateway,
) {
    private val ui = MutableStateFlow(LocationsUiState())
    val state: StateFlow<LocationsUiState> = ui.asStateFlow()

    fun updateDraftName(value: String) {
        ui.update { it.copy(draftName = value) }
    }

    fun updateDraftTimezone(value: String) {
        ui.update { it.copy(draftTimezone = value) }
    }

    fun updateDraftAddress(value: String) {
        ui.update { it.copy(draftAddress = value) }
    }

    fun updateDraftZoneCode(value: String) {
        ui.update { it.copy(draftZoneCode = value) }
    }

    suspend fun refresh() {
        val membership = organizations.currentMembership()
        val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
        if (!access.canOpenLocations) {
            ui.update {
                it.copy(
                    loading = false,
                    canWrite = false,
                    banner = "Locations are not available.",
                    items = emptyList(),
                )
            }
            return
        }
        ui.update { it.copy(loading = true, banner = null, canWrite = access.canAddLocation) }
        try {
            val items = LocationRules.visibleTo(
                locations = locations.list(),
                role = Role.from(membership?.role),
                locationIds = membership?.locationIds.orEmpty(),
            )
            ui.update { it.copy(loading = false, items = items, banner = null) }
        } catch (failure: LocationFailure) {
            ui.update { latest ->
                latest.copy(
                    loading = false,
                    banner = when (failure) {
                        LocationFailure.Network ->
                            if (latest.items.isEmpty()) {
                                "You need a connection to load locations."
                            } else {
                                "You are offline. Showing the last loaded locations."
                            }
                        LocationFailure.Rejected -> "Locations could not be loaded."
                    },
                )
            }
        }
    }

    suspend fun create(clientId: String) {
        val current = ui.value
        if (!current.canCreate) {
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val created = locations.create(
                clientId = clientId,
                name = current.draftName,
                timezone = current.draftTimezone,
                address = current.draftAddress,
                zoneCode = current.draftZoneCode,
            )
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    draftName = "",
                    draftTimezone = "",
                    draftAddress = "",
                    draftZoneCode = "",
                    items = latest.items + created,
                    banner = null,
                )
            }
        } catch (failure: LocationFailure) {
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        LocationFailure.Network -> "You need a connection to create a location."
                        LocationFailure.Rejected -> "The location was not saved."
                    },
                )
            }
        }
    }

    suspend fun save(
        id: String,
        name: String,
        timezone: String,
        address: String,
        zoneCode: String,
        status: String,
    ) {
        if (!ui.value.canWrite || ui.value.saving) {
            return
        }
        ui.update { it.copy(saving = true, banner = null) }
        try {
            val updated = locations.update(id, name, timezone, address, zoneCode, status)
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    items = latest.items.map { location ->
                        if (location.id == id) updated else location
                    },
                    banner = null,
                )
            }
        } catch (failure: LocationFailure) {
            ui.update { latest ->
                latest.copy(
                    saving = false,
                    banner = when (failure) {
                        LocationFailure.Network -> "You need a connection to save this location."
                        LocationFailure.Rejected -> "The location was not saved."
                    },
                )
            }
        }
    }

    fun location(id: String): LocationRecord? {
        return ui.value.items.find { it.id == id }
    }
}
