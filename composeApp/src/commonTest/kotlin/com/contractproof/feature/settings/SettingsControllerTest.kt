package com.contractproof.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsControllerTest {
    @Test
    fun syncSummaryReflectsQueueCounts() {
        val idle = SettingsUiState(pendingSyncCount = 0, failedSyncCount = 0)
        assertEquals("All changes are synced.", idle.syncSummaryLine)

        val pending = SettingsUiState(pendingSyncCount = 2, failedSyncCount = 0)
        assertEquals("2 waiting to sync", pending.syncSummaryLine)

        val failed = SettingsUiState(pendingSyncCount = 1, failedSyncCount = 3)
        assertEquals("1 waiting · 3 need attention", failed.syncSummaryLine)
    }
}
