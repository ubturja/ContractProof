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

abstract class GenerateSentryConfigTask : DefaultTask() {
    @get:Input
    abstract val dsnFromEnvironment: Property<String>

    @get:Input
    abstract val dsnFromGradle: Property<String>

    @get:Input
    abstract val environmentFromEnvironment: Property<String>

    @get:Input
    abstract val environmentFromGradle: Property<String>

    @get:Input
    abstract val debugBuild: Property<Boolean>

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val dsn = firstPresent(
            dsnFromEnvironment.get(),
            localProperty("SENTRY_DSN"),
            dsnFromGradle.get(),
        )
        val environment = firstPresent(
            environmentFromEnvironment.get(),
            localProperty("SENTRY_ENVIRONMENT"),
            environmentFromGradle.get(),
            if (debugBuild.get()) "development" else "production",
        )
        val output = outputDirectory.get().asFile.resolve(
            "com/contractproof/core/observability/SentryLocalConfig.kt",
        )
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.contractproof.core.observability

            internal object SentryLocalConfig {
                const val dsn: String = ${kotlinStringLiteral(dsn)}
                const val environment: String = ${kotlinStringLiteral(environment)}
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
