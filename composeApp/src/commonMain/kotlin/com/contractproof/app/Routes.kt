package com.contractproof.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute : NavKey

@Serializable
data object OnboardingRoute : NavKey

@Serializable
data object LoginRoute : NavKey

@Serializable
data object RegisterRoute : NavKey

@Serializable
data object ResetPasswordRoute : NavKey

@Serializable
data object CompanySetupRoute : NavKey

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object TodayRoute : NavKey

@Serializable
data object ClientHoldRoute : NavKey

@Serializable
data object ClientHomeRoute : NavKey

@Serializable
data class ClientServiceRecordRoute(val jobId: String) : NavKey

@Serializable
data class ClientDisputeCreateRoute(
    val jobId: String? = null,
    val requirementId: String? = null,
) : NavKey

@Serializable
data object ClientDisputeConfirmationRoute : NavKey

@Serializable
data object LocationsRoute : NavKey

@Serializable
data object ClientsRoute : NavKey

@Serializable
data class ClientDetailRoute(val id: String) : NavKey

@Serializable
data class LocationDetailRoute(val id: String) : NavKey

@Serializable
data object ServiceRoute : NavKey

@Serializable
data class JobRoute(val jobId: String) : NavKey

@Serializable
data class TaskRoute(val jobId: String, val requirementId: String) : NavKey

@Serializable
data class CaptureRoute(val jobId: String, val requirementId: String) : NavKey

@Serializable
data class ExceptionRoute(val jobId: String, val requirementId: String) : NavKey

@Serializable
data class CoverageRoute(val jobId: String) : NavKey

@Serializable
data class CompleteRoute(val jobId: String) : NavKey

@Serializable
data object ContractsRoute : NavKey

@Serializable
data object ContractCreateRoute : NavKey

@Serializable
data class ContractDetailRoute(val id: String) : NavKey

@Serializable
data class ContractRequirementsRoute(
    val contractId: String,
    val versionId: String,
) : NavKey

@Serializable
data class ExtractionReviewRoute(
    val contractId: String,
    val versionId: String,
    val autoExtract: Boolean = false,
) : NavKey

@Serializable
data object DisputesRoute : NavKey

@Serializable
data object DisputeCreateRoute : NavKey

@Serializable
data class DisputeDetailRoute(val disputeId: String) : NavKey

@Serializable
data class ReportPreviewRoute(val disputeId: String) : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object PaywallRoute : NavKey
