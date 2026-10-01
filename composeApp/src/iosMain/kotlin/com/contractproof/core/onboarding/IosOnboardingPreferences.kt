package com.contractproof.core.onboarding

import platform.Foundation.NSUserDefaults

private const val KEY_SEEN = "contractproof_onboarding_seen"

class IosOnboardingPreferences : OnboardingPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun hasSeenOnboarding(): Boolean {
        return defaults.boolForKey(KEY_SEEN)
    }

    override suspend fun markOnboardingSeen() {
        defaults.setBool(true, KEY_SEEN)
    }
}
