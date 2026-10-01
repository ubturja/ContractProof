package com.contractproof.app

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val contractProofNavSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SplashRoute::class, SplashRoute.serializer())
        subclass(OnboardingRoute::class, OnboardingRoute.serializer())
        subclass(LoginRoute::class, LoginRoute.serializer())
        subclass(RegisterRoute::class, RegisterRoute.serializer())
        subclass(ResetPasswordRoute::class, ResetPasswordRoute.serializer())
        subclass(CompanySetupRoute::class, CompanySetupRoute.serializer())
        subclass(DashboardRoute::class, DashboardRoute.serializer())
        subclass(TodayRoute::class, TodayRoute.serializer())
        subclass(ClientHoldRoute::class, ClientHoldRoute.serializer())
        subclass(ClientHomeRoute::class, ClientHomeRoute.serializer())
        subclass(ClientServiceRecordRoute::class, ClientServiceRecordRoute.serializer())
        subclass(ClientDisputeCreateRoute::class, ClientDisputeCreateRoute.serializer())
        subclass(ClientDisputeConfirmationRoute::class, ClientDisputeConfirmationRoute.serializer())
        subclass(LocationsRoute::class, LocationsRoute.serializer())
        subclass(ClientsRoute::class, ClientsRoute.serializer())
        subclass(ClientDetailRoute::class, ClientDetailRoute.serializer())
        subclass(LocationDetailRoute::class, LocationDetailRoute.serializer())
        subclass(ServiceRoute::class, ServiceRoute.serializer())
        subclass(JobRoute::class, JobRoute.serializer())
        subclass(TaskRoute::class, TaskRoute.serializer())
        subclass(CaptureRoute::class, CaptureRoute.serializer())
        subclass(ExceptionRoute::class, ExceptionRoute.serializer())
        subclass(CoverageRoute::class, CoverageRoute.serializer())
        subclass(CompleteRoute::class, CompleteRoute.serializer())
        subclass(ContractsRoute::class, ContractsRoute.serializer())
        subclass(ContractCreateRoute::class, ContractCreateRoute.serializer())
        subclass(ContractDetailRoute::class, ContractDetailRoute.serializer())
        subclass(ContractRequirementsRoute::class, ContractRequirementsRoute.serializer())
        subclass(ExtractionReviewRoute::class, ExtractionReviewRoute.serializer())
        subclass(DisputesRoute::class, DisputesRoute.serializer())
        subclass(DisputeCreateRoute::class, DisputeCreateRoute.serializer())
        subclass(DisputeDetailRoute::class, DisputeDetailRoute.serializer())
        subclass(ReportPreviewRoute::class, ReportPreviewRoute.serializer())
        subclass(SettingsRoute::class, SettingsRoute.serializer())
        subclass(PaywallRoute::class, PaywallRoute.serializer())
    }
}

val contractProofNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = contractProofNavSerializersModule
}
