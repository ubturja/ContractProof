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

abstract class GeneratePostHogConfigTask : DefaultTask() {
    @get:Input
    abstract val apiKeyFromEnvironment: Property<String>

    @get:Input
    abstract val apiKeyFromGradle: Property<String>

    @get:Input
    abstract val hostFromEnvironment: Property<String>

    @get:Input
    abstract val hostFromGradle: Property<String>

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val apiKey = firstPresent(
            apiKeyFromEnvironment.get(),
            localProperty("POSTHOG_API_KEY"),
            apiKeyFromGradle.get(),
        )
        val host = firstPresent(
            hostFromEnvironment.get(),
            localProperty("POSTHOG_HOST"),
            hostFromGradle.get(),
            "https://us.i.posthog.com",
        )
        val output = outputDirectory.get().asFile.resolve(
            "com/contractproof/core/analytics/PostHogLocalConfig.kt",
        )
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.contractproof.core.analytics

            internal object PostHogLocalConfig {
                const val apiKey: String = ${kotlinStringLiteral(apiKey)}
                const val host: String = ${kotlinStringLiteral(host)}
            }
            """.trimIndent() + "\n",
        )
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
