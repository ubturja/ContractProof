package com.contractproof.domain

import kotlin.math.min
import kotlin.math.pow

object SyncBackoffRules {
    const val BaseDelaySeconds = 30L
    const val MaxDelaySeconds = 15 * 60L

    fun delaySecondsForAttempt(attemptCount: Int): Long {
        if (attemptCount <= 0) {
            return 0L
        }
        val exponential = BaseDelaySeconds * 2.0.pow(attemptCount - 1).toLong()
        return min(exponential, MaxDelaySeconds)
    }
}
