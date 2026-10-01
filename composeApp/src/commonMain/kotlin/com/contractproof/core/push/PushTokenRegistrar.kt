package com.contractproof.core.push

interface PushTokenRegistrar {
    suspend fun registerToken(token: String)
}

class NoOpPushTokenRegistrar : PushTokenRegistrar {
    override suspend fun registerToken(token: String) {
        // Server registration is not wired in this MVP step.
    }
}
