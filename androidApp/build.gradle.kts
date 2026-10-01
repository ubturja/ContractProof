import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.core)
    androidTestImplementation(projects.composeApp)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation("org.jetbrains.compose.ui:ui-test-junit4:${libs.versions.compose.get()}")
    androidTestImplementation(libs.kotlinx.datetime)
}

val keystorePropertiesFile = rootProject.layout.projectDirectory.file("keystore.properties").asFile
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

val useDebugSigningForRelease =
    System.getenv("CONTRACTPROOF_USE_DEBUG_SIGNING") == "true"

android {
    namespace = "com.contractproof.app"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "com.contractproof.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            when {
                keystorePropertiesFile.exists() -> {
                    signingConfig = signingConfigs.getByName("release")
                }
                useDebugSigningForRelease -> {
                    signingConfig = signingConfigs.getByName("debug")
                }
                else -> {
                    // Unsigned release; assembleRelease fails at package step unless debug fallback is set.
                }
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val googleServicesFile = layout.projectDirectory.file("google-services.json").asFile
if (googleServicesFile.exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
}
