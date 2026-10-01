package com.contractproof.core

import android.content.Context
import android.content.pm.ApplicationInfo
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.contractproof.data.local.ContractProofDatabase
import com.contractproof.data.local.createContractProofDatabase
import com.contractproof.core.analytics.NoOpProductAnalytics
import com.contractproof.core.analytics.PostHogInitializer
import com.contractproof.core.analytics.PostHogProductAnalytics
import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.core.observability.NoOpErrorReporter
import com.contractproof.core.observability.SentryErrorReporter
import com.contractproof.core.observability.SentryInitializer
import com.contractproof.core.onboarding.AndroidOnboardingPreferences
import com.contractproof.core.onboarding.OnboardingPreferences
import com.contractproof.core.push.NoOpPushTokenRegistrar
import com.contractproof.core.push.PushTokenRegistrar
import com.contractproof.core.push.SupabasePushTokenRegistrar
import io.github.jan.supabase.SupabaseClient
import com.contractproof.domain.SubscriptionService
import com.contractproof.subscription.DemoBypassSubscriptionService
import com.contractproof.subscription.DisabledSubscriptionService
import com.contractproof.subscription.RevenueCatInitializer
import com.contractproof.subscription.RevenueCatLocalConfig
import com.contractproof.core.platform.isDebugBuild
import com.contractproof.subscription.RevenueCatSubscriptionService
import org.koin.core.module.Module
import org.koin.dsl.module

private var applicationContext: Context? = null
private var revenueCatEnabled: Boolean = false
private var postHogEnabled: Boolean = false
private var sentryEnabled: Boolean = false
private val sdkInitLock = Any()
private var sdksInitialized: Boolean = false

fun initPlatformContext(context: Context) {
    applicationContext = context.applicationContext
}

private fun ensureAndroidSdks() {
    if (sdksInitialized) {
        return
    }
    val context = requireApplicationContext()
    synchronized(sdkInitLock) {
        if (sdksInitialized) {
            return
        }
        try {
            postHogEnabled = PostHogInitializer.configure(context)
        } catch (_: Throwable) {
            postHogEnabled = false
        }
        try {
            sentryEnabled = SentryInitializer.configure(context)
        } catch (_: Throwable) {
            sentryEnabled = false
        }
        try {
            revenueCatEnabled = RevenueCatInitializer.configure(context)
        } catch (_: Throwable) {
            revenueCatEnabled = false
        }
        sdksInitialized = true
    }
}

internal fun requireApplicationContext(): Context {
    return applicationContext ?: error("Call initPlatformContext before starting the app on Android.")
}

internal fun isApplicationDebuggable(): Boolean {
    val context = applicationContext ?: return false
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

actual fun platformModule(): Module {
    return module {
        single {
            ensureAndroidSdks()
            Unit
        }
        single<OnboardingPreferences> { AndroidOnboardingPreferences() }
        single {
            val context = applicationContext
                ?: error("Call initPlatformContext before starting the app on Android.")
            val driver = AndroidSqliteDriver(
                schema = ContractProofDatabase.Schema,
                context = context,
                name = "contractproof.db",
            )
            createContractProofDatabase(driver)
        }
        single<ProductAnalytics> {
            if (postHogEnabled) {
                PostHogProductAnalytics()
            } else {
                NoOpProductAnalytics()
            }
        }
        single<PushTokenRegistrar> {
            SupabasePushTokenRegistrar(get<SupabaseClient>())
        }
        single<ErrorReporter> {
            if (sentryEnabled) {
                SentryErrorReporter()
            } else {
                NoOpErrorReporter()
            }
        }
        single<SubscriptionService> {
            val base: SubscriptionService = if (revenueCatEnabled) {
                RevenueCatSubscriptionService()
            } else {
                DisabledSubscriptionService()
            }
            val allowDemoBypass = RevenueCatLocalConfig.demoBypassSubscription && isDebugBuild()
            if (allowDemoBypass) {
                DemoBypassSubscriptionService(base)
            } else {
                base
            }
        }
    }
}
