package com.contractproof.core.onboarding

interface OnboardingPreferences {
    suspend fun hasSeenOnboarding(): Boolean

    suspend fun markOnboardingSeen()
}

class InMemoryOnboardingPreferences(
    private var seen: Boolean = false,
) : OnboardingPreferences {
    override suspend fun hasSeenOnboarding(): Boolean = seen

    override suspend fun markOnboardingSeen() {
        seen = true
    }
}
