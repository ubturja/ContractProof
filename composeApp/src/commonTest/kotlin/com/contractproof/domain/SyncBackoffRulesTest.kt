package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncBackoffRulesTest {
    @Test
    fun firstAttemptUsesBaseDelay() {
        assertEquals(30L, SyncBackoffRules.delaySecondsForAttempt(1))
    }

    @Test
    fun exponentialGrowthIsCapped() {
        assertEquals(60L, SyncBackoffRules.delaySecondsForAttempt(2))
        assertEquals(SyncBackoffRules.MaxDelaySeconds, SyncBackoffRules.delaySecondsForAttempt(10))
    }
}
