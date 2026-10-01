import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.Properties

abstract class GenerateRevenueCatConfigTask : DefaultTask() {
    @get:Input
    abstract val apiKeyFromEnvironment: Property<String>

    @get:Input
    abstract val apiKeyFromGradle: Property<String>

    @get:Input
    abstract val entitlementProFromEnvironment: Property<String>

    @get:Input
    abstract val entitlementBusinessFromEnvironment: Property<String>

    @get:Input
    abstract val demoBypassFromEnvironment: Property<String>

    @get:Input
    abstract val demoBypassFromGradle: Property<String>

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val apiKey = firstPresent(
            apiKeyFromEnvironment.get(),
            localProperty("REVENUECAT_API_KEY"),
            apiKeyFromGradle.get(),
        )
        val entitlementPro = firstPresent(
            entitlementProFromEnvironment.get(),
            localProperty("REVENUECAT_ENTITLEMENT_PRO"),
            "pro",
        )
        val entitlementBusiness = firstPresent(
            entitlementBusinessFromEnvironment.get(),
            localProperty("REVENUECAT_ENTITLEMENT_BUSINESS"),
            "business",
        )
        val demoBypass = parseBoolean(
            firstPresent(
                demoBypassFromEnvironment.get(),
                localProperty("DEMO_BYPASS_SUBSCRIPTION"),
                demoBypassFromGradle.get(),
            ),
        )
        val output = outputDirectory.get().asFile.resolve(
            "com/contractproof/subscription/RevenueCatLocalConfig.kt",
        )
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.contractproof.subscription

            internal object RevenueCatLocalConfig {
                const val apiKey: String = ${kotlinStringLiteral(apiKey)}
                const val entitlementPro: String = ${kotlinStringLiteral(entitlementPro)}
                const val entitlementBusiness: String = ${kotlinStringLiteral(entitlementBusiness)}
                const val demoBypassSubscription: Boolean = $demoBypass
            }
            """.trimIndent() + "\n",
        )
    }

    private fun parseBoolean(raw: String): Boolean {
        return raw.trim().lowercase() in setOf("1", "true", "yes", "on")
    }

    private fun localProperty(name: String): String {
        val file = localPropertiesFile.asFile.orNull ?: return ""
        if (!file.exists()) {
            return ""
        }
        val properties = Properties()
        file.inputStream().use(properties::load)
        return properties.getProperty(name).orEmpty()
    }

    private fun firstPresent(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun kotlinStringLiteral(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
