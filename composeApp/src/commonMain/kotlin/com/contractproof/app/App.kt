package com.contractproof.app

import androidx.compose.runtime.Composable
import com.contractproof.core.appModule
import com.contractproof.core.design.ContractProofTheme
import io.github.jan.supabase.SupabaseClient
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        koinConfiguration {
            modules(appModule)
        },
    ) {
        koinInject<SupabaseClient>()
        ContractProofTheme {
            AppNavHost()
        }
    }
}
