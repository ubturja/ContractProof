package com.contractproof.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

fun createContractProofSupabaseClient(config: SupabaseConfig): SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = config.url,
        supabaseKey = config.anonKey,
    ) {
        install(Auth)
        install(Postgrest)
    }
    println("Supabase client initialized for ${config.host}")
    return client
}
