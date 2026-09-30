package com.contractproof.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute : NavKey

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
data object ContractsRoute : NavKey

@Serializable
data object DisputesRoute : NavKey

@Serializable
data object SettingsRoute : NavKey
