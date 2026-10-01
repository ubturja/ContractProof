package com.contractproof.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightSyncMetadataStore(
    private val database: ContractProofDatabase,
) {
    private val queries = database.syncMetadataQueries

    suspend fun get(key: String): String? {
        return withContext(Dispatchers.Default) {
            queries.selectValue(key).executeAsOneOrNull()
        }
    }

    suspend fun put(key: String, value: String) {
        withContext(Dispatchers.Default) {
            queries.upsert(key, value)
        }
    }
}
