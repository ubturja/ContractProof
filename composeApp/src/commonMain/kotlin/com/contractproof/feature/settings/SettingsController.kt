package com.contractproof.feature.settings

import com.contractproof.data.AuthGateway
import com.contractproof.data.OrganizationFailure
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.local.SqlDelightSyncQueueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val loading: Boolean = false,
    val banner: String? = null,
    val email: String = "",
    val roleLabel: String = "",
    val organizationName: String = "",
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
) {
    val syncSummaryLine: String
        get() = when {
            pendingSyncCount == 0 && failedSyncCount == 0 -> "All changes are synced."
            failedSyncCount > 0 ->
                "$pendingSyncCount waiting · $failedSyncCount need attention"
            else -> "$pendingSyncCount waiting to sync"
        }
}

class SettingsController(
    private val organizations: OrganizationGateway,
    private val auth: AuthGateway,
    private val syncQueue: SqlDelightSyncQueueStore,
) {
    private val ui = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = ui.asStateFlow()

    suspend fun refresh() {
        ui.update { it.copy(loading = true, banner = null) }
        val email = auth.currentUser()?.email.orEmpty()
        val pending = syncQueue.countPendingOrRetrying()
        val failed = syncQueue.countFailed()
        try {
            val membership = organizations.currentMembership()
            ui.update {
                it.copy(
                    loading = false,
                    email = email,
                    roleLabel = membership?.role?.replaceFirstChar { c -> c.uppercase() } ?: "",
                    organizationName = membership?.organizationName.orEmpty(),
                    pendingSyncCount = pending,
                    failedSyncCount = failed,
                    banner = null,
                )
            }
        } catch (_: OrganizationFailure) {
            ui.update {
                it.copy(
                    loading = false,
                    email = email,
                    pendingSyncCount = pending,
                    failedSyncCount = failed,
                    banner = "Could not refresh account details. Showing cached info.",
                )
            }
        }
    }
}
