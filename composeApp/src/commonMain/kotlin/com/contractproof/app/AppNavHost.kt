package com.contractproof.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.contractproof.core.push.InboundNotification
import com.contractproof.core.push.PendingNotificationTap
import com.contractproof.core.push.PushNotificationRouter
import com.contractproof.core.push.rememberNotificationPermissionController
import com.contractproof.domain.Access
import com.contractproof.domain.ClientRules
import com.contractproof.domain.LocationRules
import com.contractproof.feature.clientportal.ClientDisputeConfirmationScreen
import com.contractproof.feature.clientportal.ClientDisputeCreateController
import com.contractproof.feature.clientportal.ClientHomeScreen
import com.contractproof.feature.clientportal.ClientServiceController
import com.contractproof.feature.clientportal.ClientServiceRecordScreen
import com.contractproof.feature.dispute.DisputeCreateScreen
import com.contractproof.core.onboarding.OnboardingPreferences
import com.contractproof.core.platform.isDebugBuild
import com.contractproof.demo.DemoLocalConfig
import com.contractproof.feature.auth.CompanySetupScreen
import com.contractproof.feature.auth.OnboardingScreen
import com.contractproof.feature.auth.LoginScreen
import com.contractproof.feature.auth.RegisterScreen
import com.contractproof.feature.auth.ResetPasswordScreen
import com.contractproof.feature.auth.SplashScreen
import com.contractproof.feature.client.ClientDetailScreen
import com.contractproof.feature.client.ClientsController
import com.contractproof.feature.client.ClientsScreen
import com.contractproof.core.platform.rememberOpenUrl
import com.contractproof.domain.SubscriptionPlan
import com.contractproof.domain.SubscriptionService
import com.contractproof.feature.subscription.PaywallController
import com.contractproof.feature.subscription.PaywallScreen
import com.contractproof.core.platform.rememberPdfPickerLauncher
import com.contractproof.core.platform.rememberShareUrl
import com.contractproof.feature.report.ReportPreviewController
import com.contractproof.feature.report.ReportPreviewScreen
import com.contractproof.feature.contract.ContractCreateScreen
import com.contractproof.feature.contract.ContractDetailScreen
import com.contractproof.feature.contract.ContractHealthController
import com.contractproof.feature.contract.ContractsController
import com.contractproof.feature.contract.ContractsScreen
import com.contractproof.feature.extraction.ExtractionReviewController
import com.contractproof.feature.extraction.ExtractionReviewScreen
import com.contractproof.feature.requirement.RequirementsController
import com.contractproof.feature.requirement.RequirementsScreen
import com.contractproof.domain.ContractRules
import com.contractproof.feature.dashboard.DashboardController
import com.contractproof.feature.dashboard.DashboardScreen
import com.contractproof.feature.dispute.DisputeCreateScreen
import com.contractproof.feature.dispute.DisputeDetailScreen
import com.contractproof.feature.dispute.DisputeCreateController
import com.contractproof.feature.dispute.DisputeDetailController
import com.contractproof.feature.dispute.DisputesController
import com.contractproof.feature.dispute.DisputesScreen
import com.contractproof.feature.location.LocationDetailScreen
import com.contractproof.feature.location.LocationsController
import com.contractproof.feature.location.LocationsScreen
import com.contractproof.domain.TaskNextAction
import com.contractproof.feature.service.CaptureScreen
import com.contractproof.feature.service.CompleteScreen
import com.contractproof.feature.service.CoverageScreen
import com.contractproof.feature.service.ExceptionScreen
import com.contractproof.feature.service.JobController
import com.contractproof.feature.service.JobScreen
import com.contractproof.feature.service.ServiceScreen
import com.contractproof.feature.service.TaskScreen
import com.contractproof.feature.service.TodayController
import com.contractproof.feature.service.TodayScreen
import com.contractproof.feature.settings.SettingsController
import com.contractproof.feature.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    session: SessionController = koinInject(),
    clients: ClientsController = koinInject(),
    locations: LocationsController = koinInject(),
    contracts: ContractsController = koinInject(),
    contractHealth: ContractHealthController = koinInject(),
    clientServices: ClientServiceController = koinInject(),
    clientDisputeCreate: ClientDisputeCreateController = koinInject(),
    requirements: RequirementsController = koinInject(),
    extractionReview: ExtractionReviewController = koinInject(),
    today: TodayController = koinInject(),
    dashboard: DashboardController = koinInject(),
    jobs: JobController = koinInject(),
    disputesList: DisputesController = koinInject(),
    disputeCreate: DisputeCreateController = koinInject(),
    disputeDetail: DisputeDetailController = koinInject(),
    reportPreview: ReportPreviewController = koinInject(),
    paywall: PaywallController = koinInject(),
    subscription: SubscriptionService = koinInject(),
    settings: SettingsController = koinInject(),
    onboardingPrefs: OnboardingPreferences = koinInject(),
) {
    val backStack = rememberNavBackStack(contractProofNavSavedStateConfiguration, SplashRoute)
    val auth by session.auth.state.collectAsState()
    val destination by session.destination.collectAsState()
    val setup by session.setup.collectAsState()
    val membership by session.membership.collectAsState()
    val access = membership?.let { Access.forMembership(it.role) } ?: Access.unsigned
    val clientsState by clients.state.collectAsState()
    val locationsState by locations.state.collectAsState()
    val contractsState by contracts.state.collectAsState()
    val contractHealthState by contractHealth.state.collectAsState()
    val clientServiceListState by clientServices.listState.collectAsState()
    val clientServiceRecordState by clientServices.recordState.collectAsState()
    val clientDisputeCreateState by clientDisputeCreate.state.collectAsState()
    val requirementsState by requirements.state.collectAsState()
    val extractionReviewState by extractionReview.state.collectAsState()
    val reportPreviewState by reportPreview.state.collectAsState()
    val todayState by today.state.collectAsState()
    val dashboardState by dashboard.state.collectAsState()
    val jobState by jobs.jobState.collectAsState()
    val taskState by jobs.taskState.collectAsState()
    val exceptionState by jobs.exceptionState.collectAsState()
    val scope = rememberCoroutineScope()
    val openUrl = rememberOpenUrl()
    val paywallState by paywall.state.collectAsState()
    val subscriptionSnapshot by subscription.state.collectAsState()
    val settingsState by settings.state.collectAsState()
    var needsOnboarding by remember { mutableStateOf<Boolean?>(null) }
    val openPaywall: () -> Unit = {
        backStack.add(PaywallRoute)
    }
    val notificationPermission = rememberNotificationPermissionController()
    val showDemoCredentialsHint = DemoLocalConfig.showCredentialsHint && isDebugBuild()

    fun openNotificationTarget(notification: InboundNotification) {
        val route = NotificationRoutingRules.navTarget(notification, access) ?: return
        if (backStack.last().isAuthOrSetupRoute() || backStack.last() is SplashRoute) {
            when (destination) {
                SessionDestination.Dashboard -> backStack.replaceWith(DashboardRoute)
                SessionDestination.Today -> backStack.replaceWith(TodayRoute)
                SessionDestination.ClientHold -> backStack.replaceWith(ClientHomeRoute)
                else -> return
            }
        }
        backStack.add(route)
    }

    DisposableEffect(backStack) {
        PushNotificationRouter.register { notification ->
            openNotificationTarget(notification)
        }
        onDispose {
            PushNotificationRouter.clear()
        }
    }

    LaunchedEffect(destination) {
        if (destination != SessionDestination.SignedOut && destination != SessionDestination.Restoring) {
            PendingNotificationTap.consume()?.let { openNotificationTarget(it) }
        }
    }

    LaunchedEffect(locationsState.needsUpgrade) {
        if (locationsState.needsUpgrade) {
            locations.clearUpgradeSignal()
            openPaywall()
        }
    }
    LaunchedEffect(extractionReviewState.needsUpgrade) {
        if (extractionReviewState.needsUpgrade) {
            extractionReview.clearUpgradeSignal()
            openPaywall()
        }
    }
    LaunchedEffect(reportPreviewState.needsUpgrade) {
        if (reportPreviewState.needsUpgrade) {
            reportPreview.clearUpgradeSignal()
            openPaywall()
        }
    }

    LaunchedEffect(Unit) {
        needsOnboarding = !onboardingPrefs.hasSeenOnboarding()
        session.restore()
    }
    LaunchedEffect(destination) {
        when (destination) {
            SessionDestination.Restoring -> Unit
            SessionDestination.SignedOut -> {
                if (backStack.last() is SplashRoute || backStack.last().isSignedInRoute()) {
                    val nextRoute = if (needsOnboarding == true) {
                        OnboardingRoute
                    } else {
                        LoginRoute
                    }
                    backStack.replaceWith(nextRoute)
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
                    backStack.replaceWith(ClientHomeRoute)
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
            entry<OnboardingRoute> {
                OnboardingScreen(
                    onContinue = {
                        scope.launch {
                            onboardingPrefs.markOnboardingSeen()
                            needsOnboarding = false
                            backStack.replaceWith(LoginRoute)
                        }
                    },
                )
            }
            entry<LoginRoute> {
                LoginScreen(
                    state = auth,
                    showDemoCredentialsHint = showDemoCredentialsHint,
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
                if (membership != null) {
                    LaunchedEffect(destination) {
                        val home = when (destination) {
                            SessionDestination.Today -> TodayRoute
                            SessionDestination.ClientHold -> ClientHomeRoute
                            else -> DashboardRoute
                        }
                        backStack.replaceWith(home)
                    }
                } else {
                    CompanySetupScreen(
                        state = setup,
                        onCompanyNameChange = session::updateCompanyName,
                        onDisplayNameChange = session::updateDisplayName,
                        onSubmit = { scope.launch { session.createCompany() } },
                    )
                }
            }
            entry<DashboardRoute> {
                LaunchedEffect(Unit) {
                    locations.refresh()
                    dashboard.refresh()
                }
                DashboardScreen(
                    state = dashboardState,
                    hasLocations = locationsState.items.isNotEmpty(),
                    canAddLocation = access.canAddLocation,
                    canOpenLocations = access.canOpenLocations,
                    canOpenContracts = access.canOpenContracts,
                    canOpenDisputes = access.canOpenDisputes,
                    onOpenJob = { jobId -> backStack.add(JobRoute(jobId)) },
                    onOpenDispute = { disputeId -> backStack.add(DisputeDetailRoute(disputeId)) },
                    onOpenLocations = { backStack.add(LocationsRoute) },
                    onOpenContracts = { backStack.add(ContractsRoute) },
                    onOpenDisputes = { backStack.add(DisputesRoute) },
                    onOpenSettings = { backStack.add(SettingsRoute) },
                    onRetry = { scope.launch { dashboard.refresh() } },
                    onRefresh = { scope.launch { dashboard.refresh() } },
                )
            }
            entry<TodayRoute> {
                if (!access.canOpenAssignedJobs) {
                    LaunchedEffect(Unit) {
                        backStack.replaceWith(DashboardRoute)
                    }
                } else {
                    LaunchedEffect(Unit) {
                        today.refresh()
                    }
                    TodayScreen(
                        state = todayState,
                        onOpenJob = { jobId -> backStack.add(JobRoute(jobId)) },
                        onOpenSettings = { backStack.add(SettingsRoute) },
                        onRetry = { scope.launch { today.refresh() } },
                    )
                }
            }
            entry<JobRoute> { route ->
                LaunchedEffect(route.jobId) {
                    jobs.load(route.jobId)
                    jobs.refresh()
                }
                JobScreen(
                    state = jobState,
                    onStartService = { scope.launch { jobs.startService() } },
                    onOpenTask = { requirementId ->
                        jobs.loadTask(requirementId)
                        backStack.add(TaskRoute(route.jobId, requirementId))
                    },
                    onOpenCoverage = { backStack.add(CoverageRoute(route.jobId)) },
                    onFinishService = { backStack.add(CompleteRoute(route.jobId)) },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<TaskRoute> { route ->
                LaunchedEffect(route.requirementId) {
                    jobs.loadTask(route.requirementId)
                }
                TaskScreen(
                    state = taskState,
                    onPrimaryAction = {
                        when (taskState.primaryAction) {
                            TaskNextAction.CapturePhoto -> {
                                backStack.add(CaptureRoute(route.jobId, route.requirementId))
                            }
                            TaskNextAction.MarkDone -> {
                                scope.launch { jobs.markDone(route.requirementId) }
                            }
                            else -> Unit
                        }
                    },
                    onReportException = {
                        backStack.add(ExceptionRoute(route.jobId, route.requirementId))
                    },
                    onRetryUpload = {
                        scope.launch { jobs.retryPhotoUpload(route.requirementId) }
                    },
                    onRetakePhoto = {
                        backStack.add(CaptureRoute(route.jobId, route.requirementId))
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<CaptureRoute> { route ->
                LaunchedEffect(route.requirementId) {
                    jobs.loadTask(route.requirementId)
                }
                CaptureScreen(
                    requirementText = taskState.requirementText,
                    mandatoryLabel = taskState.mandatoryLabel,
                    saving = taskState.saving,
                    banner = taskState.banner,
                    onPhotoConfirmed = { photo ->
                        scope.launch {
                            jobs.preparePhoto(route.requirementId, photo)
                            backStack.removeAt(backStack.lastIndex)
                            jobs.uploadPhoto(route.requirementId)
                        }
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<ExceptionRoute> { route ->
                LaunchedEffect(route.requirementId) {
                    jobs.loadTask(route.requirementId)
                }
                ExceptionScreen(
                    requirementText = taskState.requirementText,
                    state = exceptionState,
                    onReasonSelect = jobs::updateExceptionReason,
                    onNoteChange = jobs::updateExceptionNote,
                    onSave = {
                        scope.launch {
                            if (jobs.saveException(route.requirementId)) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<CoverageRoute> { route ->
                CoverageScreen(
                    state = jobState.coverageUi,
                    onOpenTask = { requirementId ->
                        jobs.loadTask(requirementId)
                        backStack.add(TaskRoute(route.jobId, requirementId))
                    },
                    onRetryUpload = { requirementId ->
                        scope.launch { jobs.retryPhotoUpload(requirementId) }
                    },
                    onFinish = { backStack.add(CompleteRoute(route.jobId)) },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<CompleteRoute> { route ->
                CompleteScreen(
                    coveragePercent = jobState.coveragePercent,
                    canFinish = jobState.canFinish,
                    completionBlockerLabels = jobState.completionBlockerLabels,
                    saving = jobState.saving,
                    banner = jobState.banner,
                    onFinish = {
                        scope.launch {
                            if (jobs.finishService()) {
                                popCompletedServiceFlow(backStack)
                                today.refresh()
                            }
                        }
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<ClientHomeRoute> {
                if (!access.canOpenClientServiceRecords) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        clientServices.refreshList()
                    }
                    ClientHomeScreen(
                        state = clientServiceListState,
                        onOpenRecord = { jobId -> backStack.add(ClientServiceRecordRoute(jobId)) },
                        onSignOut = {
                            scope.launch {
                                session.signOut()
                                backStack.replaceWith(LoginRoute)
                            }
                        },
                        onRetry = { scope.launch { clientServices.refreshList() } },
                    )
                }
            }
            entry<ClientServiceRecordRoute> { route ->
                LaunchedEffect(route.jobId) {
                    clientServices.loadRecord(route.jobId)
                }
                ClientServiceRecordScreen(
                    state = clientServiceRecordState,
                    onReportIssue = {
                        backStack.add(ClientDisputeCreateRoute(jobId = route.jobId, requirementId = null))
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onRetry = { scope.launch { clientServices.loadRecord(route.jobId) } },
                )
            }
            entry<ClientDisputeCreateRoute> { route ->
                val pickAttachment = rememberPdfPickerLauncher { picked ->
                    if (picked != null) {
                        clientDisputeCreate.setAttachment(
                            fileName = picked.fileName,
                            bytes = picked.bytes,
                            mimeType = "application/pdf",
                        )
                    }
                }
                LaunchedEffect(route.jobId, route.requirementId) {
                    clientDisputeCreate.prepare(route.jobId, route.requirementId)
                    clientDisputeCreate.load()
                }
                DisputeCreateScreen(
                    state = clientDisputeCreateState,
                    showServiceDateField = false,
                    onSelectJob = clientDisputeCreate::selectJob,
                    onSelectRequirement = clientDisputeCreate::selectRequirement,
                    onComplaintChange = clientDisputeCreate::updateComplaint,
                    onServiceDateChange = {},
                    onPickAttachment = pickAttachment,
                    onClearAttachment = clientDisputeCreate::clearAttachment,
                    onSubmit = {
                        scope.launch {
                            clientDisputeCreate.submit {
                                backStack.removeAt(backStack.lastIndex)
                                backStack.add(ClientDisputeConfirmationRoute)
                            }
                        }
                    },
                    onRetry = { scope.launch { clientDisputeCreate.load() } },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<ClientDisputeConfirmationRoute> {
                ClientDisputeConfirmationScreen(
                    onDone = {
                        backStack.replaceWith(ClientHomeRoute)
                        scope.launch { clientServices.refreshList() }
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
                ServiceScreen(
                    onOpenDashboard = { backStack.replaceWith(DashboardRoute) },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<ContractsRoute> {
                if (!access.canOpenContracts) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        contracts.refresh()
                        clients.refresh()
                        locations.refresh()
                    }
                    LaunchedEffect(contractsState.items) {
                        if (contractsState.items.isNotEmpty()) {
                            contractHealth.refreshSummaries(contractsState.items)
                        }
                    }
                    ContractsScreen(
                        state = contractsState,
                        healthSubtitles = contractHealthState.listSubtitles,
                        clients = clientsState.items,
                        locations = locationsState.items,
                        clientName = { id -> contracts.clientName(clientsState.items, id) },
                        locationName = { id -> contracts.locationName(locationsState.items, id) },
                        onOpen = { id -> backStack.add(ContractDetailRoute(id)) },
                        onCreate = { backStack.add(ContractCreateRoute) },
                        onRetry = {
                            scope.launch {
                                contracts.refresh()
                                contractHealth.refreshSummaries(contracts.state.value.items)
                            }
                        },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<ContractCreateRoute> {
                if (!access.canWriteContracts) {
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
                    ContractCreateScreen(
                        state = contractsState,
                        clients = clientsState.items,
                        locations = locationsState.items,
                        onTitleChange = contracts::updateDraftTitle,
                        onClientChange = contracts::updateDraftClient,
                        onLocationChange = contracts::updateDraftLocation,
                        onStartsOnChange = contracts::updateDraftStartsOn,
                        onEndsOnChange = contracts::updateDraftEndsOn,
                        onEffectiveOnChange = contracts::updateDraftEffectiveOn,
                        onDocumentPicked = contracts::updateDraftDocument,
                        onClearDocument = contracts::clearDraftDocument,
                        onCreate = {
                            scope.launch {
                                val location = locations.location(contractsState.draftLocationId)
                                    ?: return@launch
                                val created = contracts.create(location)
                                if (created != null) {
                                    val versionId = contracts.latestVersionId(created.id)
                                    val hasPdf = ContractRules.hasDocument(created)
                                    backStack.removeAt(backStack.lastIndex)
                                    if (hasPdf && versionId != null) {
                                        backStack.add(
                                            ExtractionReviewRoute(
                                                contractId = created.id,
                                                versionId = versionId,
                                                autoExtract = true,
                                            ),
                                        )
                                    } else {
                                        backStack.add(ContractDetailRoute(created.id))
                                    }
                                }
                            }
                        },
                        onRetryUpload = {
                            scope.launch {
                                val location = locations.location(contractsState.draftLocationId)
                                    ?: return@launch
                                val created = contracts.retryUpload(location)
                                if (created != null) {
                                    backStack.removeAt(backStack.lastIndex)
                                    backStack.add(ContractDetailRoute(created.id))
                                }
                            }
                        },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<ContractDetailRoute> { route ->
                if (!access.canOpenContracts) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    val contract = contracts.contract(route.id)
                    var title by remember(route.id, contract?.title) {
                        mutableStateOf(contract?.title.orEmpty())
                    }
                    var startsOn by remember(route.id, contract?.startsOn) {
                        mutableStateOf(contract?.startsOn.orEmpty())
                    }
                    var endsOn by remember(route.id, contract?.endsOn) {
                        mutableStateOf(contract?.endsOn.orEmpty())
                    }
                    val pickPdf = rememberPdfPickerLauncher { picked ->
                        if (picked != null) {
                            contracts.updateDraftDocument(picked.fileName, picked.bytes)
                        }
                    }
                    LaunchedEffect(route.id) {
                        contracts.refreshVersions(route.id)
                    }
                    LaunchedEffect(route.id, contract?.id) {
                        contract?.let { contractHealth.load(it) }
                    }
                    ContractDetailScreen(
                        contract = contract,
                        health = contractHealthState.detail,
                        healthLoading = contractHealthState.loadingDetail,
                        healthBanner = contractHealthState.banner,
                        versions = contractsState.versions,
                        currentUserId = contractsState.currentUserId,
                        clientName = contracts.clientName(clientsState.items, contract?.clientId.orEmpty()),
                        locationName = contracts.locationName(locationsState.items, contract?.locationId.orEmpty()),
                        canWrite = access.canWriteContracts,
                        saving = contractsState.saving,
                        viewing = contractsState.viewing,
                        banner = contractsState.banner,
                        title = title,
                        startsOn = startsOn,
                        endsOn = endsOn,
                        onTitleChange = { title = it },
                        onStartsOnChange = { startsOn = it },
                        onEndsOnChange = { endsOn = it },
                        onSave = {
                            scope.launch {
                                contracts.save(
                                    id = route.id,
                                    title = title,
                                    startsOn = startsOn,
                                    endsOn = endsOn,
                                    status = contract?.status ?: ContractRules.Draft,
                                )
                            }
                        },
                        onEnd = {
                            scope.launch {
                                contracts.save(
                                    id = route.id,
                                    title = title,
                                    startsOn = startsOn,
                                    endsOn = endsOn,
                                    status = ContractRules.Ended,
                                )
                            }
                        },
                        onRestoreDraft = {
                            scope.launch {
                                contracts.save(
                                    id = route.id,
                                    title = title,
                                    startsOn = startsOn,
                                    endsOn = endsOn,
                                    status = ContractRules.Draft,
                                )
                            }
                        },
                        onOpenVersion = { path ->
                            scope.launch {
                                val url = contracts.openDocument(path)
                                if (url != null) {
                                    openUrl(url)
                                }
                            }
                        },
                        onPickDocument = pickPdf,
                        onAttachDocument = {
                            scope.launch { contracts.attach(route.id) }
                        },
                        onRetryUpload = {
                            scope.launch { contracts.attach(route.id) }
                        },
                        onActivate = { versionId ->
                            scope.launch { contracts.activate(route.id, versionId) }
                        },
                        onEditRequirements = { versionId ->
                            backStack.add(ContractRequirementsRoute(route.id, versionId))
                        },
                        onOpenExtractionReview = { versionId, autoExtract ->
                            backStack.add(
                                ExtractionReviewRoute(
                                    contractId = route.id,
                                    versionId = versionId,
                                    autoExtract = autoExtract,
                                ),
                            )
                        },
                        onEffectiveOnChange = contracts::updateDraftEffectiveOn,
                        uploadStatus = contractsState.uploadStatus,
                        uploadPercent = contractsState.uploadPercent,
                        draftFileName = contractsState.draftFileName,
                        draftEffectiveOn = contractsState.draftEffectiveOn,
                        canRetryUpload = contractsState.canRetryUpload,
                        canUploadNextVersion = contractsState.canUploadNextVersion,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<ContractRequirementsRoute> { route ->
                if (!access.canOpenContracts) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(route.contractId, route.versionId) {
                        requirements.refresh(route.contractId, route.versionId)
                    }
                    RequirementsScreen(
                        state = requirementsState,
                        onZoneCodeChange = requirements::updateZoneCode,
                        onSaveZone = {
                            scope.launch { requirements.saveZone() }
                        },
                        onFrequencyChange = requirements::updateDraftFrequency,
                        onWeekdayChange = requirements::updateDraftWeekday,
                        onStartTimeChange = requirements::updateDraftStartTime,
                        onEndTimeChange = requirements::updateDraftEndTime,
                        onStartsOnChange = requirements::updateDraftStartsOn,
                        onEndsOnChange = requirements::updateDraftEndsOn,
                        onSaveSchedule = {
                            scope.launch { requirements.saveSchedule() }
                        },
                        onTaskChange = requirements::updateDraftTask,
                        onRequiresPhotoChange = requirements::updateDraftRequiresPhoto,
                        onIsMandatoryChange = requirements::updateDraftIsMandatory,
                        onSaveRequirement = {
                            scope.launch { requirements.saveRequirement() }
                        },
                        onNewRequirement = requirements::startNewRequirement,
                        onEditRequirement = requirements::startEditRequirement,
                        onCancelEdit = requirements::cancelEdit,
                        onDeleteRequirement = { id ->
                            scope.launch { requirements.deleteRequirement(id) }
                        },
                        onMoveRequirement = { id, delta ->
                            scope.launch { requirements.moveRequirement(id, delta) }
                        },
                        onPauseSchedule = { id ->
                            scope.launch { requirements.deactivateSchedule(id) }
                        },
                        onResumeSchedule = { id ->
                            scope.launch { requirements.reactivateSchedule(id) }
                        },
                        onApprove = {
                            scope.launch {
                                if (requirements.approve(route.contractId, route.versionId)) {
                                    backStack.removeAt(backStack.lastIndex)
                                    contracts.refresh()
                                }
                            }
                        },
                        onRetry = {
                            scope.launch { requirements.refresh(route.contractId, route.versionId) }
                        },
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<ExtractionReviewRoute> { route ->
                if (!access.canOpenContracts) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                    LaunchedEffect(route.contractId, route.versionId) {
                        extractionReview.load(route.contractId, route.versionId)
                        if (route.autoExtract) {
                            extractionReview.startExtraction()
                        }
                    }
                    ExtractionReviewScreen(
                        state = extractionReviewState,
                        onDraftTaskChange = extractionReview::updateDraftTask,
                        onRequiresPhotoChange = extractionReview::updateDraftRequiresPhoto,
                        onIsMandatoryChange = extractionReview::updateDraftIsMandatory,
                        onSaveDraft = extractionReview::saveDraft,
                        onNewDraft = extractionReview::startNewDraft,
                        onEditDraft = extractionReview::startEditDraft,
                        onCancelEdit = extractionReview::cancelEdit,
                        onDeleteDraft = extractionReview::deleteDraft,
                        onMoveUp = { id -> extractionReview.moveDraft(id, -1) },
                        onMoveDown = { id -> extractionReview.moveDraft(id, 1) },
                        onApprove = {
                            scope.launch {
                                if (extractionReview.approve()) {
                                    backStack.removeAt(backStack.lastIndex)
                                    contracts.refresh()
                                }
                            }
                        },
                        onRetryExtract = {
                            scope.launch { extractionReview.retryExtraction() }
                        },
                        onStartExtract = {
                            scope.launch { extractionReview.startExtraction() }
                        },
                        onManualEntry = {
                            backStack.add(
                                ContractRequirementsRoute(route.contractId, route.versionId),
                            )
                        },
                        onRetryLoad = {
                            scope.launch { extractionReview.load(route.contractId, route.versionId) }
                        },
                        onOpenPlans = openPaywall,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                    )
                }
            }
            entry<DisputesRoute> {
                val disputesState by disputesList.state.collectAsState()
                LaunchedEffect(Unit) {
                    disputesList.load()
                }
                DisputesScreen(
                    state = disputesState,
                    onRetry = { scope.launch { disputesList.load() } },
                    onCreate = { backStack.add(DisputeCreateRoute) },
                    onOpen = { id -> backStack.add(DisputeDetailRoute(id)) },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<DisputeCreateRoute> {
                val createState by disputeCreate.state.collectAsState()
                val pickPdf = rememberPdfPickerLauncher { picked ->
                    if (picked != null) {
                        disputeCreate.setAttachment(
                            fileName = picked.fileName,
                            bytes = picked.bytes,
                            mimeType = "application/pdf",
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    disputeCreate.load()
                }
                DisputeCreateScreen(
                    state = createState,
                    onSelectJob = disputeCreate::selectJob,
                    onSelectRequirement = disputeCreate::selectRequirement,
                    onComplaintChange = disputeCreate::updateComplaint,
                    onServiceDateChange = disputeCreate::updateServiceDate,
                    onPickAttachment = pickPdf,
                    onClearAttachment = disputeCreate::clearAttachment,
                    onSubmit = {
                        scope.launch {
                            disputeCreate.submit { id ->
                                backStack.removeAt(backStack.lastIndex)
                                backStack.add(DisputeDetailRoute(id))
                            }
                        }
                    },
                    onRetry = { scope.launch { disputeCreate.load() } },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<DisputeDetailRoute> { route ->
                if (!access.canOpenDisputes) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                val detailState by disputeDetail.state.collectAsState()
                LaunchedEffect(route.disputeId) {
                    disputeDetail.load(route.disputeId)
                }
                DisputeDetailScreen(
                    state = detailState,
                    onRetry = { scope.launch { disputeDetail.load(route.disputeId) } },
                    onGenerateSummary = {
                        scope.launch { disputeDetail.generateSummary(route.disputeId) }
                    },
                    onOpenEvidenceReport = {
                        backStack.add(ReportPreviewRoute(route.disputeId))
                    },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
                }
            }
            entry<ReportPreviewRoute> { route ->
                if (!access.canOpenDisputes) {
                    LaunchedEffect(Unit) {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                } else {
                val reportState = reportPreviewState
                val openUrl = rememberOpenUrl()
                val shareUrl = rememberShareUrl()
                LaunchedEffect(route.disputeId) {
                    reportPreview.load(route.disputeId)
                }
                ReportPreviewScreen(
                    state = reportState,
                    onGenerate = {
                        scope.launch { reportPreview.generate(route.disputeId) }
                    },
                    onRetry = {
                        scope.launch { reportPreview.generate(route.disputeId, force = true) }
                    },
                    onOpen = {
                        reportState.signedUrl?.let { openUrl(it) }
                    },
                    onShare = {
                        reportState.signedUrl?.let { shareUrl(it, "Service evidence report") }
                    },
                    onReload = { scope.launch { reportPreview.load(route.disputeId) } },
                    onOpenPlans = openPaywall,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
                }
            }
            entry<PaywallRoute> {
                LaunchedEffect(Unit) {
                    paywall.load()
                }
                PaywallScreen(
                    state = paywallState,
                    onPurchase = { plan ->
                        scope.launch { paywall.purchase(plan) }
                    },
                    onRestore = { scope.launch { paywall.restore() } },
                    onRetry = { scope.launch { paywall.load() } },
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<SettingsRoute> {
                LaunchedEffect(Unit) {
                    settings.refresh()
                }
                val planLabel = when (subscriptionSnapshot.effectivePlan()) {
                    SubscriptionPlan.Business -> "Business"
                    SubscriptionPlan.Pro -> "Pro"
                    SubscriptionPlan.Free -> "Free"
                }
                val notificationState = notificationPermission.currentState()
                SettingsScreen(
                    state = settingsState,
                    canManageSubscription = access.canManageSubscription,
                    currentPlanLabel = planLabel,
                    onOpenSubscription = { backStack.add(PaywallRoute) },
                    showNotificationPermission = notificationState.canRequest,
                    onRequestNotificationPermission = {
                        scope.launch { notificationPermission.requestPermission() }
                    },
                    onRetry = { scope.launch { settings.refresh() } },
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

internal fun NavKey.isServiceExecutionRoute(): Boolean {
    return this is JobRoute ||
        this is TaskRoute ||
        this is CaptureRoute ||
        this is ExceptionRoute ||
        this is CoverageRoute ||
        this is CompleteRoute
}

internal fun popCompletedServiceFlow(backStack: MutableList<NavKey>) {
    while (backStack.size > 1 && backStack.last().isServiceExecutionRoute()) {
        backStack.removeAt(backStack.lastIndex)
    }
}

private fun NavKey.isAuthOrSetupRoute(): Boolean {
    return this is SplashRoute ||
        this is LoginRoute ||
        this is RegisterRoute ||
        this is ResetPasswordRoute ||
        this is OnboardingRoute ||
        this is CompanySetupRoute
}

private fun NavKey.isSignedInRoute(): Boolean {
    return this is CompanySetupRoute ||
        this is DashboardRoute ||
        this is TodayRoute ||
        this is ClientHomeRoute ||
        this is ClientServiceRecordRoute ||
        this is ClientDisputeCreateRoute ||
        this is ClientDisputeConfirmationRoute ||
        this is LocationsRoute ||
        this is LocationDetailRoute ||
        this is ClientsRoute ||
        this is ClientDetailRoute ||
        this is ServiceRoute ||
        this is JobRoute ||
        this is TaskRoute ||
        this is CaptureRoute ||
        this is ExceptionRoute ||
        this is CoverageRoute ||
        this is CompleteRoute ||
        this is ContractsRoute ||
        this is ContractCreateRoute ||
        this is ContractDetailRoute ||
        this is ContractRequirementsRoute ||
        this is ExtractionReviewRoute ||
        this is DisputesRoute ||
        this is DisputeCreateRoute ||
        this is DisputeDetailRoute ||
        this is ReportPreviewRoute ||
        this is SettingsRoute ||
        this is PaywallRoute
}

private fun NavBackStack<NavKey>.replaceWith(route: NavKey) {
    add(route)
    while (size > 1) {
        removeAt(0)
    }
}
