package com.contractproof.core

import com.contractproof.app.SessionController
import com.contractproof.data.AuthGateway
import com.contractproof.data.CachingOrganizationGateway
import com.contractproof.data.ClientGateway
import com.contractproof.data.ContractGateway
import com.contractproof.data.LocationGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.ExceptionGateway
import com.contractproof.data.ExtractionGateway
import com.contractproof.data.OfflineFirstExceptionGateway
import com.contractproof.data.OfflineFirstServiceJobRepository
import com.contractproof.data.SupabaseEvidenceRepository
import com.contractproof.domain.EvidenceRepository
import com.contractproof.data.SupabaseExceptionGateway
import com.contractproof.data.RequirementGateway
import com.contractproof.data.ScheduleGateway
import com.contractproof.domain.ServiceJobRepository
import com.contractproof.data.SupabaseAuthGateway
import com.contractproof.data.SupabaseClientGateway
import com.contractproof.data.SupabaseConfig
import com.contractproof.data.DisputeGateway
import com.contractproof.data.ReportGateway
import com.contractproof.data.SupabaseContractGateway
import com.contractproof.data.SupabaseDisputeGateway
import com.contractproof.data.SupabaseReportGateway
import com.contractproof.data.SupabaseLocalConfig
import com.contractproof.data.SupabaseLocationGateway
import com.contractproof.data.SupabaseOrganizationGateway
import com.contractproof.data.SupabaseExtractionGateway
import com.contractproof.data.SupabaseRequirementGateway
import com.contractproof.data.SupabaseScheduleGateway
import com.contractproof.data.SupabaseServiceJobGateway
import com.contractproof.data.createContractProofSupabaseClient
import com.contractproof.data.local.SqlDelightEvidenceStore
import com.contractproof.data.local.SqlDelightExceptionStore
import com.contractproof.data.local.SqlDelightServiceJobStore
import com.contractproof.data.local.SqlDelightSyncMetadataStore
import com.contractproof.data.local.SqlDelightSyncQueueStore
import com.contractproof.data.local.SqlDelightUserContextStore
import com.contractproof.data.sync.SyncCoordinator
import com.contractproof.feature.auth.AuthController
import com.contractproof.feature.client.ClientsController
import com.contractproof.feature.contract.ContractsController
import com.contractproof.feature.extraction.ExtractionReviewController
import com.contractproof.feature.requirement.RequirementsController
import com.contractproof.feature.location.LocationsController
import com.contractproof.feature.service.JobController
import com.contractproof.feature.service.ServiceExecutionDraftHydrator
import com.contractproof.feature.service.ServiceExecutionDraftStore
import com.contractproof.feature.dispute.DisputeCreateController
import com.contractproof.feature.dispute.DisputeDetailController
import com.contractproof.feature.report.ReportPreviewController
import com.contractproof.feature.dispute.DisputesController
import com.contractproof.core.onboarding.InMemoryOnboardingPreferences
import com.contractproof.core.onboarding.OnboardingPreferences
import com.contractproof.feature.settings.SettingsController
import com.contractproof.feature.service.TodayController
import com.contractproof.feature.subscription.PaywallController
import kotlin.time.Clock
import kotlinx.datetime.Instant
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single {
        SupabaseConfig(
            url = SupabaseLocalConfig.url,
            anonKey = SupabaseLocalConfig.anonKey,
        )
    }
    single {
        createContractProofSupabaseClient(get())
    }
    single<AuthGateway> { SupabaseAuthGateway(get()) }
    single { SqlDelightUserContextStore(get()) }
    single { SqlDelightServiceJobStore(get()) }
    single { SqlDelightExceptionStore(get()) }
    single { SqlDelightSyncMetadataStore(get()) }
    single { SqlDelightSyncQueueStore(get()) }
    single { SqlDelightEvidenceStore(get()) }
    single { ServiceExecutionDraftHydrator(get(), get()) }
    single<OrganizationGateway> {
        val clock: () -> String = {
            Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toString()
        }
        CachingOrganizationGateway(
            delegate = SupabaseOrganizationGateway(get(), get()),
            auth = get(),
            userContext = get(),
            clock = clock,
        )
    }
    single<ClientGateway> { SupabaseClientGateway(get(), get()) }
    single<LocationGateway> { SupabaseLocationGateway(get(), get()) }
    single<RequirementGateway> { SupabaseRequirementGateway(get(), get()) }
    single<ScheduleGateway> { SupabaseScheduleGateway(get(), get()) }
    single<ServiceJobRepository>(named("remote")) {
        SupabaseServiceJobGateway(get(), get(), get(), get(), get(), get())
    }
    single<ServiceJobRepository> {
        OfflineFirstServiceJobRepository(
            remote = get(named("remote")),
            local = get(),
            organizations = get(),
        )
    }
    single<ContractGateway> { SupabaseContractGateway(get(), get(), get(), get()) }
    single<ExtractionGateway> { SupabaseExtractionGateway(get(), get(), get()) }
    single<ReportGateway> {
        SupabaseReportGateway(
            client = get(),
            config = get(),
            organizations = get(),
            disputes = get(),
        )
    }
    single<DisputeGateway> {
        SupabaseDisputeGateway(
            client = get(),
            config = get(),
            organizations = get(),
            auth = get(),
            clients = get(),
            locations = get(),
            serviceJobs = get(named("remote")),
        )
    }
    single<com.contractproof.data.ClientServiceGateway> {
        com.contractproof.data.SupabaseClientServiceGateway(
            client = get(),
            organizations = get(),
            serviceJobs = get(named("remote")),
            evidence = get(),
        )
    }
    single<EvidenceRepository> { SupabaseEvidenceRepository(get(), get(), get(), get(named("remote")), get()) }
    single<ExceptionGateway>(named("remote")) { SupabaseExceptionGateway(get(), get(), get()) }
    single<ExceptionGateway> {
        OfflineFirstExceptionGateway(
            remote = get(named("remote")),
            organizations = get(),
            auth = get(),
            local = get(),
            syncCoordinator = get(),
        )
    }
    single {
        SyncCoordinator(
            organizations = get(),
            queue = get(),
            metadata = get(),
            evidence = get(),
            exceptions = get(named("remote")),
            serviceJobs = get(named("remote")),
            exceptionStore = get(),
        )
    }
    single { ServiceExecutionDraftStore() }
    single { AuthController(get(), get()) }
    single { SessionController(get(), get(), get(), get(), get()) }
    single { ClientsController(get(), get(), get()) }
    single { LocationsController(get(), get(), get(), get()) }
    single { ContractsController(get(), get(), get(), get()) }
    single { com.contractproof.feature.contract.ContractHealthController(get(), get()) }
    single { RequirementsController(get(), get(), get(), get(), get()) }
    single { ExtractionReviewController(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<OnboardingPreferences> { InMemoryOnboardingPreferences() }
    single { SettingsController(get(), get(), get()) }
    single { TodayController(get(), get()) }
    single { com.contractproof.feature.dashboard.DashboardController(get(), get(), get()) }
    single { DisputesController(get(), get()) }
    single { DisputeCreateController(get(), get(), get(), get()) }
    single { DisputeDetailController(get(), get()) }
    single { ReportPreviewController(get(), get(), get(), get(), get()) }
    single { PaywallController(get(), get()) }
    single { com.contractproof.feature.clientportal.ClientServiceController(get(), get()) }
    single {
        com.contractproof.feature.clientportal.ClientDisputeCreateController(
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        JobController(
            organizations = get(),
            serviceJobs = get(),
            evidence = get(),
            exceptions = get(),
            drafts = get(),
            draftHydrator = get(),
            syncCoordinator = get(),
            evidenceStore = get(),
            analytics = get(),
        )
    }
}
