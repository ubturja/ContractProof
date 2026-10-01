package com.contractproof.core

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.contractproof.data.local.ContractProofDatabase
import com.contractproof.data.local.createContractProofDatabase
import com.contractproof.core.onboarding.IosOnboardingPreferences
import com.contractproof.core.onboarding.OnboardingPreferences
import com.contractproof.core.analytics.NoOpProductAnalytics
import com.contractproof.core.analytics.ProductAnalytics
import com.contractproof.core.observability.ErrorReporter
import com.contractproof.core.observability.NoOpErrorReporter
import com.contractproof.core.push.NoOpPushTokenRegistrar
import com.contractproof.core.push.PushTokenRegistrar
import com.contractproof.domain.SubscriptionService
import com.contractproof.subscription.DisabledSubscriptionService
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module {
    return module {
        single<OnboardingPreferences> { IosOnboardingPreferences() }
        single<ProductAnalytics> { NoOpProductAnalytics() }
        single<ErrorReporter> { NoOpErrorReporter() }
        single<PushTokenRegistrar> { NoOpPushTokenRegistrar() }
        single<SubscriptionService> { DisabledSubscriptionService() }
        single {
            val driver = NativeSqliteDriver(
                schema = ContractProofDatabase.Schema,
                name = "contractproof.db",
            )
            createContractProofDatabase(driver)
        }
    }
}
