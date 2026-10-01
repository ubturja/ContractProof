package com.contractproof.core.push

object PushTokenBridge {
    var registrar: PushTokenRegistrar? = null

    suspend fun onNewToken(token: String) {
        registrar?.registerToken(token)
    }
}
