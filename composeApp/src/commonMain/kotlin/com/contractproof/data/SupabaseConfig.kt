package com.contractproof.data

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
) {
    val host: String = url.removePrefix("https://").substringBefore('/').substringBefore('?')

    init {
        require(url.startsWith("https://") && host.isNotEmpty()) {
            "SUPABASE_URL must be an https URL. See docs/development/supabase.md."
        }
        require(anonKey.isNotBlank()) {
            "SUPABASE_ANON_KEY is required. See docs/development/supabase.md."
        }
    }
}
