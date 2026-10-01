package com.contractproof.core.push

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabasePushTokenRegistrar(
    private val client: SupabaseClient,
) : PushTokenRegistrar {
    override suspend fun registerToken(token: String) {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        client.from("push_device_tokens").upsert(
            PushDeviceTokenRow(
                userId = userId,
                token = token,
                platform = "android",
            ),
        )
    }
}

@Serializable
private data class PushDeviceTokenRow(
    @SerialName("user_id") val userId: String,
    val token: String,
    val platform: String,
)
