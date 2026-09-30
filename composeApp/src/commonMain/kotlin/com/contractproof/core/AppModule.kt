package com.contractproof.core

import com.contractproof.app.SessionController
import com.contractproof.data.AuthGateway
import com.contractproof.data.ClientGateway
import com.contractproof.data.LocationGateway
import com.contractproof.data.OrganizationGateway
import com.contractproof.data.SupabaseAuthGateway
import com.contractproof.data.SupabaseClientGateway
import com.contractproof.data.SupabaseConfig
import com.contractproof.data.SupabaseLocalConfig
import com.contractproof.data.SupabaseLocationGateway
import com.contractproof.data.SupabaseOrganizationGateway
import com.contractproof.data.createContractProofSupabaseClient
import com.contractproof.feature.auth.AuthController
import com.contractproof.feature.client.ClientsController
import com.contractproof.feature.location.LocationsController
import org.koin.dsl.module

val appModule = module {
    single {
        createContractProofSupabaseClient(
            SupabaseConfig(
                url = SupabaseLocalConfig.url,
                anonKey = SupabaseLocalConfig.anonKey,
            ),
        )
    }
    single<AuthGateway> { SupabaseAuthGateway(get()) }
    single<OrganizationGateway> { SupabaseOrganizationGateway(get(), get()) }
    single<ClientGateway> { SupabaseClientGateway(get(), get()) }
    single<LocationGateway> { SupabaseLocationGateway(get(), get()) }
    single { AuthController(get()) }
    single { SessionController(get(), get(), get()) }
    single { ClientsController(get(), get()) }
    single { LocationsController(get(), get()) }
}
