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
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.compose.material.icons.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
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
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateSupabaseConfig)
}

sqldelight {
    databases {
        create("ContractProofDatabase") {
            packageName.set("com.contractproof.data.local")
        }
    }
}
