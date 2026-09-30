package com.contractproof.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.contractproof.domain.Access
import com.contractproof.domain.ClientRules
import com.contractproof.domain.LocationRules
import com.contractproof.feature.auth.ClientHoldScreen
import com.contractproof.feature.auth.CompanySetupScreen
import com.contractproof.feature.auth.LoginScreen
import com.contractproof.feature.auth.RegisterScreen
import com.contractproof.feature.auth.ResetPasswordScreen
import com.contractproof.feature.auth.SplashScreen
import com.contractproof.feature.client.ClientDetailScreen
import com.contractproof.feature.client.ClientsController
import com.contractproof.feature.client.ClientsScreen
import com.contractproof.feature.contract.ContractsScreen
import com.contractproof.feature.dashboard.DashboardScreen
import com.contractproof.feature.dispute.DisputesScreen
import com.contractproof.feature.location.LocationDetailScreen
import com.contractproof.feature.location.LocationsController
import com.contractproof.feature.location.LocationsScreen
import com.contractproof.feature.service.ServiceScreen
import com.contractproof.feature.service.TodayScreen
import com.contractproof.feature.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    session: SessionController = koinInject(),
    clients: ClientsController = koinInject(),
    locations: LocationsController = koinInject(),
) {
    val backStack = rememberNavBackStack(SplashRoute)
    val auth by session.auth.state.collectAsState()
    val destination by session.destination.collectAsState()
    val setup by session.setup.collectAsState()
    val membership by session.membership.collectAsState()
    val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
    val clientsState by clients.state.collectAsState()
    val locationsState by locations.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        session.restore()
    }
    LaunchedEffect(destination) {
        when (destination) {
            SessionDestination.Restoring -> Unit
            SessionDestination.SignedOut -> {
                if (backStack.last() is SplashRoute || backStack.last().isSignedInRoute()) {
                    backStack.replaceWith(LoginRoute)
                }
            }
            SessionDestination.CompanySetup -> {
                if (backStack.last() !is CompanySetupRoute) {
                    backStack.replaceWith(CompanySetupRoute)
                }
            }
            SessionDestination.Dashboard -> {
                if (backStack.last().isAuthOrSetupRoute()) {
                    backStack.replaceWith(DashboardRoute)
                }
            }
            SessionDestination.Today -> {
                if (backStack.last().isAuthOrSetupRoute() || backStack.last() is DashboardRoute) {
                    backStack.replaceWith(TodayRoute)
                }
            }
            SessionDestination.ClientHold -> {
                if (backStack.last().isAuthOrSetupRoute() || backStack.last() is DashboardRoute) {
                    backStack.replaceWith(ClientHoldRoute)
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = entryProvider {
            entry<SplashRoute> {
                SplashScreen()
            }
            entry<LoginRoute> {
                LoginScreen(
                    state = auth,
                    onEmailChange = session.auth::updateEmail,
                    onPasswordChange = session.auth::updatePassword,
                    onSubmit = { scope.launch { session.signIn() } },
                    onCreateAccount = {
                        session.auth.clearNotice()
                        backStack.add(RegisterRoute)
                    },
                    onForgotPassword = {
                        session.auth.clearNotice()
                        backStack.add(ResetPasswordRoute)
                    },
                )
            }
            entry<RegisterRoute> {
                RegisterScreen(
                    state = auth,
                    onEmailChange = session.auth::updateEmail,
                    onPasswordChange = session.auth::updatePassword,
                    onSubmit = { scope.launch { session.signUp() } },
                    onSignIn = {
                        session.auth.clearNotice()
                        backStack.removeAt(backStack.lastIndex)
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<ResetPasswordRoute> {
                ResetPasswordScreen(
                    state = auth,
                    onEmailChange = session.auth::updateEmail,
                    onSubmit = { scope.launch { session.requestPasswordReset() } },
                    onSignIn = {
                        session.auth.clearNotice()
                        backStack.removeAt(backStack.lastIndex)
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<CompanySetupRoute> {
                CompanySetupScreen(
                    state = setup,
                    onCompanyNameChange = session::updateCompanyName,
                    onDisplayNameChange = session::updateDisplayName,
                    onSubmit = { scope.launch { session.createCompany() } },
                )
            }
            entry<DashboardRoute> {
                DashboardScreen(
                    organizationName = membership?.organizationName.orEmpty(),
                    canAddLocation = access.canAddLocation,
                    canOpenLocations = access.canOpenLocations,
                    canOpenService = access.canOpenService,
                    canOpenContracts = access.canOpenContracts,
                    canOpenDisputes = access.canOpenDisputes,
                    canOpenClients = access.canOpenClients,
                    onOpenLocations = { backStack.add(LocationsRoute) },
                    onOpenService = { backStack.add(ServiceRoute) },
                    onOpenContracts = { backStack.add(ContractsRoute) },
                    onOpenDisputes = { backStack.add(DisputesRoute) },
                    onOpenClients = { backStack.add(ClientsRoute) },
                    onOpenSettings = { backStack.add(SettingsRoute) },
                )
            }
            entry<TodayRoute> {
                TodayScreen(
                    organizationName = membership?.organizationName.orEmpty(),
                    onOpenSettings = { backStack.add(SettingsRoute) },
                )
            }
            entry<ClientHoldRoute> {
                ClientHoldScreen(
                    onSignOut = {
                        scope.launch {
                            session.signOut()
                            backStack.replaceWith(LoginRoute)
                        }
                    },
                )
            }
            entry<ClientsRoute> {
                if (!access.canOpenClients) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        clients.refresh()
                    }
                    LaunchedEffect(Unit) {
                        locations.refresh()
                    }
                    ClientsScreen(
                        state = clientsState,
                        onQueryChange = clients::updateQuery,
                        onDraftNameChange = clients::updateDraftName,
                        onCreate = { scope.launch { clients.create() } },
                        onOpen = { id -> backStack.add(ClientDetailRoute(id)) },
                        onRetry = { scope.launch { clients.refresh() } },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<ClientDetailRoute> { route ->
                if (!access.canOpenClients) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        locations.refresh()
                    }
                    val client = clients.client(route.id)
                    var name by remember(route.id, client?.name) {
                        mutableStateOf(client?.name.orEmpty())
                    }
                    ClientDetailScreen(
                        client = client,
                        canWrite = access.canWriteClients,
                        saving = clientsState.saving,
                        banner = clientsState.banner,
                        name = name,
                        onNameChange = { name = it },
                        onSave = {
                            scope.launch {
                                clients.save(route.id, name, client?.status ?: ClientRules.Active)
                            }
                        },
                        onArchive = {
                            scope.launch {
                                clients.save(route.id, name, ClientRules.Archived)
                            }
                        },
                        onRestore = {
                            scope.launch {
                                clients.save(route.id, name, ClientRules.Active)
                            }
                        },
                        locations = locationsState.forClient(route.id),
                        canWriteLocations = access.canAddLocation,
                        locationSaving = locationsState.saving,
                        canCreateLocation = locationsState.canCreate,
                        draftLocationName = locationsState.draftName,
                        draftTimezone = locationsState.draftTimezone,
                        draftAddress = locationsState.draftAddress,
                        draftZoneCode = locationsState.draftZoneCode,
                        onDraftLocationNameChange = locations::updateDraftName,
                        onDraftTimezoneChange = locations::updateDraftTimezone,
                        onDraftAddressChange = locations::updateDraftAddress,
                        onDraftZoneCodeChange = locations::updateDraftZoneCode,
                        onCreateLocation = { scope.launch { locations.create(route.id) } },
                        onOpenLocation = { id -> backStack.add(LocationDetailRoute(id)) },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<LocationsRoute> {
                if (!access.canOpenLocations) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        locations.refresh()
                    }
                    LocationsScreen(
                        state = locationsState,
                        onOpen = { id -> backStack.add(LocationDetailRoute(id)) },
                        onRetry = { scope.launch { locations.refresh() } },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<LocationDetailRoute> { route ->
                if (!access.canOpenLocations) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    val location = locations.location(route.id)
                    var name by remember(route.id, location?.name) {
                        mutableStateOf(location?.name.orEmpty())
                    }
                    var timezone by remember(route.id, location?.timezone) {
                        mutableStateOf(location?.timezone.orEmpty())
                    }
                    var address by remember(route.id, location?.address) {
                        mutableStateOf(location?.address.orEmpty())
                    }
                    var zoneCode by remember(route.id, location?.zoneCode) {
                        mutableStateOf(location?.zoneCode.orEmpty())
                    }
                    LocationDetailScreen(
                        location = location,
                        canWrite = access.canAddLocation,
                        saving = locationsState.saving,
                        banner = locationsState.banner,
                        name = name,
                        timezone = timezone,
                        address = address,
                        zoneCode = zoneCode,
                        onNameChange = { name = it },
                        onTimezoneChange = { timezone = it },
                        onAddressChange = { address = it },
                        onZoneCodeChange = { zoneCode = it },
                        onSave = {
                            scope.launch {
                                locations.save(
                                    id = route.id,
                                    name = name,
                                    timezone = timezone,
                                    address = address,
                                    zoneCode = zoneCode,
                                    status = location?.status ?: LocationRules.Active,
                                )
                            }
                        },
                        onArchive = {
                            scope.launch {
                                locations.save(
                                    id = route.id,
                                    name = name,
                                    timezone = timezone,
                                    address = address,
                                    zoneCode = zoneCode,
                                    status = LocationRules.Archived,
                                )
                            }
                        },
                        onRestore = {
                            scope.launch {
                                locations.save(
                                    id = route.id,
                                    name = name,
                                    timezone = timezone,
                                    address = address,
                                    zoneCode = zoneCode,
                                    status = LocationRules.Active,
                                )
                            }
                        },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<ServiceRoute> {
                ServiceScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<ContractsRoute> {
                ContractsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<DisputesRoute> {
                DisputesScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<SettingsRoute> {
                SettingsScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onSignOut = {
                        scope.launch {
                            session.signOut()
                            backStack.replaceWith(LoginRoute)
                        }
                    },
                )
            }
        },
    )
}

private fun NavKey.isAuthOrSetupRoute(): Boolean {
    return this is SplashRoute ||
        this is LoginRoute ||
        this is RegisterRoute ||
        this is ResetPasswordRoute ||
        this is CompanySetupRoute
}

private fun NavKey.isSignedInRoute(): Boolean {
    return this is CompanySetupRoute ||
        this is DashboardRoute ||
        this is TodayRoute ||
        this is ClientHoldRoute ||
        this is LocationsRoute ||
        this is LocationDetailRoute ||
        this is ClientsRoute ||
        this is ClientDetailRoute ||
        this is ServiceRoute ||
        this is ContractsRoute ||
        this is DisputesRoute ||
        this is SettingsRoute
}

private fun NavBackStack<NavKey>.replaceWith(route: NavKey) {
    add(route)
    while (size > 1) {
        removeAt(0)
    }
}
