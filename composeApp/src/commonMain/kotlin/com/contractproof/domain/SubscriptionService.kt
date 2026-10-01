package com.contractproof.domain

import kotlinx.coroutines.flow.StateFlow

interface SubscriptionService {
    val state: StateFlow<SubscriptionSnapshot>

    fun isConfigured(): Boolean

    suspend fun refresh()

    suspend fun onSignedIn(organizationId: String)

    suspend fun onSignedOut()

    suspend fun restore(): SubscriptionResult

    suspend fun loadOfferings(): List<PlanOffering>

    suspend fun purchase(plan: SubscriptionPlan): SubscriptionResult
}
