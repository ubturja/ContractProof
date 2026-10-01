import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

val supabaseLocalProperties = rootProject.layout.projectDirectory.file("local.properties")
val generatedSupabaseDirectory = layout.buildDirectory.dir("generated/supabase")

val generateSupabaseConfig = tasks.register<GenerateSupabaseConfigTask>("generateSupabaseConfig") {
    urlFromEnvironment.set(providers.environmentVariable("SUPABASE_URL").orElse(""))
    keyFromEnvironment.set(providers.environmentVariable("SUPABASE_ANON_KEY").orElse(""))
    urlFromGradle.set(providers.gradleProperty("SUPABASE_URL").orElse(""))
    keyFromGradle.set(providers.gradleProperty("SUPABASE_ANON_KEY").orElse(""))
    localPropertiesFile.set(supabaseLocalProperties)
    outputDirectory.set(generatedSupabaseDirectory)
}

val generatedRevenueCatDirectory = layout.buildDirectory.dir("generated/revenuecat")

val generateRevenueCatConfig = tasks.register<GenerateRevenueCatConfigTask>("generateRevenueCatConfig") {
    apiKeyFromEnvironment.set(providers.environmentVariable("REVENUECAT_API_KEY").orElse(""))
    apiKeyFromGradle.set(providers.gradleProperty("REVENUECAT_API_KEY").orElse(""))
    entitlementProFromEnvironment.set(providers.environmentVariable("REVENUECAT_ENTITLEMENT_PRO").orElse(""))
    entitlementBusinessFromEnvironment.set(
        providers.environmentVariable("REVENUECAT_ENTITLEMENT_BUSINESS").orElse(""),
    )
    demoBypassFromEnvironment.set(providers.environmentVariable("DEMO_BYPASS_SUBSCRIPTION").orElse(""))
    demoBypassFromGradle.set(providers.gradleProperty("DEMO_BYPASS_SUBSCRIPTION").orElse(""))
    localPropertiesFile.set(supabaseLocalProperties)
    outputDirectory.set(generatedRevenueCatDirectory)
}

val generatedPostHogDirectory = layout.buildDirectory.dir("generated/posthog")

val generatePostHogConfig = tasks.register<GeneratePostHogConfigTask>("generatePostHogConfig") {
    apiKeyFromEnvironment.set(providers.environmentVariable("POSTHOG_API_KEY").orElse(""))
    apiKeyFromGradle.set(providers.gradleProperty("POSTHOG_API_KEY").orElse(""))
    hostFromEnvironment.set(providers.environmentVariable("POSTHOG_HOST").orElse(""))
    hostFromGradle.set(providers.gradleProperty("POSTHOG_HOST").orElse(""))
    localPropertiesFile.set(supabaseLocalProperties)
    outputDirectory.set(generatedPostHogDirectory)
}

val generatedDemoDirectory = layout.buildDirectory.dir("generated/demo")

val generateDemoConfig = tasks.register<GenerateDemoConfigTask>("generateDemoConfig") {
    showCredentialsFromEnvironment.set(providers.environmentVariable("DEMO_SHOW_CREDENTIALS").orElse(""))
    showCredentialsFromGradle.set(providers.gradleProperty("DEMO_SHOW_CREDENTIALS").orElse(""))
    localPropertiesFile.set(supabaseLocalProperties)
    outputDirectory.set(generatedDemoDirectory)
}

val generatedSentryDirectory = layout.buildDirectory.dir("generated/sentry")

val generateSentryConfig = tasks.register<GenerateSentryConfigTask>("generateSentryConfig") {
    dsnFromEnvironment.set(providers.environmentVariable("SENTRY_DSN").orElse(""))
    dsnFromGradle.set(providers.gradleProperty("SENTRY_DSN").orElse(""))
    environmentFromEnvironment.set(providers.environmentVariable("SENTRY_ENVIRONMENT").orElse(""))
    environmentFromGradle.set(providers.gradleProperty("SENTRY_ENVIRONMENT").orElse(""))
    debugBuild.set(
        providers.gradleProperty("SENTRY_DEBUG_BUILD").orElse("true").map { it.toBoolean() },
    )
    localPropertiesFile.set(supabaseLocalProperties)
    outputDirectory.set(generatedSentryDirectory)
}

kotlin {
    android {
        namespace = "com.contractproof.app.shared"
        compileSdk {
            version = release(37)
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ContractProof"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedSupabaseDirectory)
            kotlin.srcDir(generatedDemoDirectory)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.compose.material.icons.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.navigation3.ui)
                implementation(libs.sqldelight.runtime)
                implementation(libs.supabase.kt)
                implementation(libs.supabase.auth)
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.storage)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.sqlite.driver)
        }
        androidMain {
            kotlin.srcDir(generatedRevenueCatDirectory)
            kotlin.srcDir(generatedPostHogDirectory)
            kotlin.srcDir(generatedSentryDirectory)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.activity.compose)
            implementation(libs.revenuecat.purchases)
            implementation(libs.posthog.android)
            implementation(libs.sentry.android)
            implementation(libs.androidx.core)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(
        generateSupabaseConfig,
        generateRevenueCatConfig,
        generatePostHogConfig,
        generateSentryConfig,
        generateDemoConfig,
    )
}

sqldelight {
    databases {
        create("ContractProofDatabase") {
            packageName.set("com.contractproof.data.local")
        }
    }
}

tasks.register("checkIosCompile") {
    group = "verification"
    description = "Compile Kotlin for iOS device and simulator (no link on Linux CI)."
    dependsOn(
        "compileKotlinIosSimulatorArm64",
        "compileKotlinIosArm64",
    )
}
