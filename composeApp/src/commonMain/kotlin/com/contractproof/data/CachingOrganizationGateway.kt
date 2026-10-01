package com.contractproof.data

import com.contractproof.data.local.SqlDelightUserContextStore
import kotlin.coroutines.cancellation.CancellationException

class CachingOrganizationGateway(
    private val delegate: OrganizationGateway,
    private val auth: AuthGateway,
    private val userContext: SqlDelightUserContextStore,
    private val clock: () -> String,
) : OrganizationGateway {
    override suspend fun currentMembership(): Membership? {
        val user = auth.currentUser()
        if (user == null) {
            return null
        }
        return try {
            val membership = delegate.currentMembership()
            if (membership != null) {
                userContext.upsert(user.email, membership, clock())
            }
            membership
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (error is OrganizationFailure.Network) {
                return userContext.currentMembership(user.id)
            }
            throw error
        }
    }

    override suspend fun createOwnerOrganization(companyName: String, displayName: String): Membership {
        return delegate.createOwnerOrganization(companyName, displayName)
    }
}
