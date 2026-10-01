package com.contractproof.data.local

import com.contractproof.data.Membership
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightUserContextStore(
    private val database: ContractProofDatabase,
) {
    private val queries = database.userContextQueries

    suspend fun currentMembership(expectedUserId: String): Membership? {
        return withContext(Dispatchers.Default) {
            val row = queries.selectCurrent().executeAsOneOrNull() ?: return@withContext null
            if (row.user_id != expectedUserId) {
                return@withContext null
            }
            UserContextLocalMapper.toMembership(row, expectedUserId)
        }
    }

    suspend fun upsert(email: String, membership: Membership, cachedAt: String) {
        withContext(Dispatchers.Default) {
            val values = UserContextLocalMapper.fromMembership(email, membership, cachedAt)
            queries.upsert(
                user_id = values.userId,
                email = values.email,
                organization_id = values.organizationId,
                organization_name = values.organizationName,
                role = values.role,
                location_ids_json = values.locationIdsJson,
                client_id = values.clientId,
                cached_at = values.cachedAt,
            )
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.Default) {
            queries.clear()
        }
    }
}
