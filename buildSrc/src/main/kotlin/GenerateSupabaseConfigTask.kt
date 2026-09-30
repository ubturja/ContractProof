import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.Properties

abstract class GenerateSupabaseConfigTask : DefaultTask() {
    @get:Input
    abstract val urlFromEnvironment: Property<String>

    @get:Input
    abstract val keyFromEnvironment: Property<String>

    @get:Input
    abstract val urlFromGradle: Property<String>

    @get:Input
    abstract val keyFromGradle: Property<String>

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val url = firstPresent(
            urlFromEnvironment.get(),
            localProperty("SUPABASE_URL"),
            urlFromGradle.get(),
        )
        val anonKey = firstPresent(
            keyFromEnvironment.get(),
            localProperty("SUPABASE_ANON_KEY"),
            keyFromGradle.get(),
        )
        if (url.isBlank() || anonKey.isBlank() || !url.startsWith("https://")) {
            throw GradleException(
                "SUPABASE_URL and SUPABASE_ANON_KEY are required. SUPABASE_URL must be an https URL. See docs/development/supabase.md.",
            )
        }
        val output = outputDirectory.get().asFile.resolve("com/contractproof/data/SupabaseLocalConfig.kt")
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.contractproof.data

            internal object SupabaseLocalConfig {
                const val url: String = ${kotlinStringLiteral(url)}
                const val anonKey: String = ${kotlinStringLiteral(anonKey)}
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
                '$' -> append("\\$")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
        append('"')
    }
}
