package com.contractproof.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.contractproof.core.appModule
import com.contractproof.core.push.PushTokenBridge
import com.contractproof.core.push.PushTokenRegistrar
import com.contractproof.data.sync.SyncCoordinator
import com.contractproof.core.platformModule
import com.contractproof.core.design.ContractProofTheme
import io.github.jan.supabase.SupabaseClient
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        koinConfiguration {
            modules(appModule, platformModule())
        },
    ) {
        koinInject<SupabaseClient>()
        val syncCoordinator = koinInject<SyncCoordinator>()
        val pushTokenRegistrar = koinInject<PushTokenRegistrar>()
        LaunchedEffect(syncCoordinator) {
            syncCoordinator.drain()
        }
        LaunchedEffect(pushTokenRegistrar) {
            PushTokenBridge.registrar = pushTokenRegistrar
        }
        ContractProofTheme {
            AppNavHost()
        }
    }
}
