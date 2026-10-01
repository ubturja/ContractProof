package com.contractproof.core.onboarding

import android.content.Context
import com.contractproof.core.requireApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "contractproof_onboarding"
private const val KEY_SEEN = "seen"

class AndroidOnboardingPreferences(
    context: Context = requireApplicationContext(),
) : OnboardingPreferences {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun hasSeenOnboarding(): Boolean {
        return withContext(Dispatchers.IO) {
            prefs.getBoolean(KEY_SEEN, false)
        }
    }

    override suspend fun markOnboardingSeen() {
        withContext(Dispatchers.IO) {
            prefs.edit().putBoolean(KEY_SEEN, true).apply()
        }
    }
}
